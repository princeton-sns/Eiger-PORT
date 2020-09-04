package org.apache.cassandra.db;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.GroupDepCheckCallback;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.ShortNodeId;
import org.apache.cassandra.utils.VersionUtil;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppliedOperations3 {
    private static Logger logger = LoggerFactory.getLogger(AppliedOperations3.class);

    enum OpStatus {
        PENDING,
        APPLIED
    }

    /**
     * Pending Operations are partitioned by node (via it's ShortNodeId that is embedded in timestamps).
     * We know that ops arrive from each node in order here, but can then be processed in a different order
     * because we're multithreaded.  So we can only clean up applied Ops that are older than the oldest of
     * the per-thread newest ops.
     */

    static final int CAPACITY = 500000;
    //static final int NUM_CONSUMERS = 4;

    static Map<Long, OpStatus> ops = new ConcurrentHashMap<Long, OpStatus>(CAPACITY, 0.75f, DatabaseDescriptor.getConcurrentWriters());
    static Map<Long, Queue<DepCheckReplyInfo>> blockedDepChecks = new HashMap<Long, Queue<DepCheckReplyInfo>>();
    //static BlockingQueue<Long> appliedOps = new LinkedBlockingQueue<Long>();
    //static Consumer[] consumers = new Consumer[NUM_CONSUMERS];


    private static class DepCheckReplyInfo {
        private final Message message;
        private final String id;
        private int expectedResponses;
        private AtomicInteger receivedResponses = new AtomicInteger(0);
        private boolean sent = false;

        private GroupDepCheckCallback localGroupDepCheckCallback;

        public DepCheckReplyInfo(Message message, String id) {
            this.message = message;
            this.id = id;
            expectedResponses = 0;

            localGroupDepCheckCallback = null;
        }

        public DepCheckReplyInfo(Message message, String id, int expectedResponses) {
            this(message, id);
            this.expectedResponses = expectedResponses;
        }

        public DepCheckReplyInfo(Message message, String id, int expectedResponses, GroupDepCheckCallback localGroupDepCheckCallback) {
            this(message, id, expectedResponses);
            this.localGroupDepCheckCallback = localGroupDepCheckCallback;

        }

        public Message getMessage() {
            return message;
        }

        public String getId() {
            return id;
        }

        public int getExpectedResponses() {
            return expectedResponses;
        }

        public int getReceivedResponses() {
            return receivedResponses.get();
        }

        public int receiveResponse() {
            return receivedResponses.incrementAndGet();
        }

        public void receiveResponseAndReply() {
            synchronized (this) {
                int responses = receivedResponses.incrementAndGet();
                if (responses >= expectedResponses && !sent) {
                    sent = true;
                    if (localGroupDepCheckCallback != null) {
                        localGroupDepCheckCallback.response(null);
                    } else {
                        assert message != null && id != null : "message and id cannot be null";
                        //sendDepCheckReply(message, id);

                        // We use stage with number of concurrent_replicates threads to
                        // put this sending off the path of updating applied ops, which are accessed by many
                        Runnable runnable = new WrappedRunnable() {
                            @Override
                            protected void runMayThrow() throws Exception {
                                sendDepCheckReply(message, id);
                            }
                        };
                        StageManager.getStage(Stage.REPLICATE_ON_WRITE).execute(runnable);
                    }
                }
            }
        }
    }

    public static void addPendingOp(ByteBuffer locatorKey, long timestamp) {
        if (VersionUtil.extractDatacenter(timestamp) == ShortNodeId.getLocalDC()) {
            if (logger.isDebugEnabled())
                logger.debug("Local DC origin, so not adding pending op: {}", new Dependency(locatorKey, timestamp));
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Add pending op: {}", new Dependency(locatorKey, timestamp));

        ops.put(timestamp, OpStatus.PENDING);
    }

    public static void addAppliedOp(ByteBuffer locatorKey, final long timestamp) {
        logger.debug("addAppliedOp called: {}", new Dependency(locatorKey, timestamp));

        if (VersionUtil.extractDatacenter(timestamp) == ShortNodeId.getLocalDC()) {
            return;
        }

        logger.debug("Add applied op: {}", new Dependency(locatorKey, timestamp));

        ops.put(timestamp, OpStatus.APPLIED);

        // signal to any blocked dep checks on this op
        //appliedOps.add(timestamp);
        Runnable runnable = new WrappedRunnable() {
            @Override
            protected void runMayThrow() throws Exception {
                signalWaitingDepChecks(timestamp);
            }
        };
        //StageManager.getStage(Stage.MISC).execute(runnable);
        StageManager.getStage(Stage.DEPENDENCY_CHECK).execute(runnable);
    }

    public static void signalWaitingDepChecks(final long timestamp) {

        // signal to any blocked dep checks on this op
        synchronized (blockedDepChecks) {
            Queue<DepCheckReplyInfo> blockedQueue = blockedDepChecks.get(timestamp);
            if (blockedQueue != null) {
                for (DepCheckReplyInfo dcri : blockedQueue) {
                    dcri.receiveResponseAndReply();
                }
                blockedDepChecks.remove(timestamp);
            }
        }
    }

    public static void checkGroupDependency(GroupDependencyCheck groupDepCheck, final Message depCheckMessage, final String id, GroupDepCheckCallback localGroupDepCheckCallback) {

        Set<Long> blockedDeps = new HashSet<Long>();
        Byte localDC = ShortNodeId.getLocalDC();

        // Skip those deps original from local DC and those already satisfied
        for (Dependency dep : groupDepCheck.getDependencies()) {

            long depTS = dep.getTimestamp();
            // Don't check dependencies for values written in this DC, we know they've been applied
            if (VersionUtil.extractDatacenter(depTS) == localDC) {
                continue;
            }

            OpStatus opStatus = ops.get(depTS);

            if (opStatus == null || opStatus == OpStatus.PENDING) {
                blockedDeps.add(depTS);
            }

        } // End traversing through deps

        // TODO: Khiem put this on the block queue
        // If no blocked deps, we can send reply and return
        if (blockedDeps.size() == 0) {
            if (localGroupDepCheckCallback != null) {
                localGroupDepCheckCallback.response(null);
            } else {
                assert depCheckMessage != null && id != null : "message and id cannot be null";
                //sendDepCheckReply(depCheckMessage, id);

                Runnable runnable = new WrappedRunnable() {
                    @Override
                    protected void runMayThrow() throws Exception {
                        sendDepCheckReply(depCheckMessage, id);
                    }
                };
                StageManager.getStage(Stage.REPLICATE_ON_WRITE).execute(runnable);

            }
            return;
        }

        // Add blocked dep reply info for each blocked dep
        blockGroupDepCheck(blockedDeps, depCheckMessage, id, localGroupDepCheckCallback);

    }

    private static void blockGroupDepCheck(Set<Long> deps, Message depCheckMessage, String id, GroupDepCheckCallback localGroupDepCheckCallback) {
        if (logger.isDebugEnabled() && depCheckMessage != null) {
            logger.debug("Block dependency check. (dcm.lt={})", depCheckMessage.getLamportTimestamp());
        }
        DepCheckReplyInfo depCheckReplyInfo = new DepCheckReplyInfo(depCheckMessage, id, deps.size(), localGroupDepCheckCallback);

        // get lock on this blockedDepChecks to make sure no replies for the checked keys
        // are sent before they are put on the queue
        synchronized (blockedDepChecks) {
            for (Long timestamp : deps) {

                // before we put the key on the queue, we should check its status again
                // to make sure the replies not sent yet
                // (it's possible that the status is changed from pending to applied
                // between checkDeps and block deps)
                OpStatus opStatus = ops.get(timestamp);
                if (opStatus != null & opStatus == OpStatus.APPLIED) {
                    depCheckReplyInfo.receiveResponseAndReply();
                    continue;
                }

                // if not applied yet, we are sure the replies are not sent
                Queue<DepCheckReplyInfo> blockedQueue = blockedDepChecks.get(timestamp);
                if (blockedQueue == null) {
                    blockedQueue = new LinkedList<DepCheckReplyInfo>();
                    blockedDepChecks.put(timestamp, blockedQueue);
                }
                blockedQueue.add(depCheckReplyInfo);

            }
        }
    }

    private static void sendDepCheckReply(Message depCheckMessage, String id) {
        logger.debug("Send dependency check reply. (dcm.lt={})", depCheckMessage.getLamportTimestamp());

        byte[] empty = new byte[0];
        Message reply = depCheckMessage.getReply(FBUtilities.getBroadcastAddress(), empty, depCheckMessage.getVersion());
        MessagingService.instance().sendReply(reply, id, depCheckMessage.getFrom());
    }

}
