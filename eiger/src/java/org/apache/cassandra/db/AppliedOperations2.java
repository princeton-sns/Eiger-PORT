package org.apache.cassandra.db;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.GroupDepCheckCallback;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.ShortNodeId;
import org.apache.cassandra.utils.VersionUtil;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppliedOperations2 {
    private static Logger logger = LoggerFactory.getLogger(AppliedOperations2.class);

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

    static Map<Short, Map<Long, OpStatus>> shortNodeIdToPendingOps = new HashMap<Short, Map<Long, OpStatus>>();
    static Map<String, Long> threadIdToNewestOp = new HashMap<String, Long>();

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

    static Map<Long, Queue<DepCheckReplyInfo>> blockedDepChecks = new HashMap<Long, Queue<DepCheckReplyInfo>>();

    private static void updateNewestOp(String threadName, Long opTimestamp) {
        // Khiem's note: update the stages should NOT be ignored if we have separate stages for dependency check
        // or any stages whose calls to this updateNewestOp matter
        if (!threadName.contains("MutationStage") && !threadName.contains("RequestResponseStage")) {
            //if (!threadName.contains("MutationStage") && !threadName.contains("RequestResponseStage") && !threadName.contains("DependencyCheckStage")) {
            //We ignore updates that aren't mutations, they only show up at system start up and we don't care about them
            logger.debug("Ignoring updateNewestOp for: " + threadName);
            return;
        }
        Long previousOpTimestamp = threadIdToNewestOp.get(threadName);
        if (previousOpTimestamp == null) {
            //Replicated operations get written too, we'll have more writing threads if we have local writers and replicated writers
            //WL TODO: keep the size of threadIdToNewestOp bounded
            //assert threadIdToNewestOp.size() <= 2*DatabaseDescriptor.getConcurrentWriters() : "Set of writing threads should be static (" + threadIdToNewestOp.size() + ", " + DatabaseDescriptor.getConcurrentWriters() + "), adding " + threadName + " to " + threadIdToNewestOp;
            threadIdToNewestOp.put(threadName, opTimestamp);
        } else {
            if (opTimestamp > previousOpTimestamp) {
                threadIdToNewestOp.put(threadName, opTimestamp);
            }
        }
    }

    private static Long oldestNewestOp() {
        Long oldest = Long.MAX_VALUE;
        for (Long opTimestamp : threadIdToNewestOp.values()) {
            oldest = Math.min(oldest, opTimestamp);
        }
        return oldest;
    }

    //WL TODO reduce the granularity of synchronization here

    public static synchronized void addPendingOp(ByteBuffer locatorKey, long timestamp) {
        if (VersionUtil.extractDatacenter(timestamp) == ShortNodeId.getLocalDC()) {
            if (logger.isDebugEnabled())
                logger.debug("Local DC origin, so not adding pending op: {}", new Dependency(locatorKey, timestamp));
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Add pending op: {}", new Dependency(locatorKey, timestamp));

        Map<Long, OpStatus> pendingOps = shortNodeIdToPendingOps.get(VersionUtil.extractShortNodeId(timestamp));
        if (pendingOps == null) {
            pendingOps = new HashMap<Long, OpStatus>();
            shortNodeIdToPendingOps.put(VersionUtil.extractShortNodeId(timestamp), pendingOps);
        }

        pendingOps.put(timestamp, OpStatus.PENDING);
    }

    public static synchronized void addAppliedOp(ByteBuffer locatorKey, long timestamp) {
        logger.debug("addAppliedOp called: {}", new Dependency(locatorKey, timestamp));

        if (VersionUtil.extractDatacenter(timestamp) == ShortNodeId.getLocalDC()) {
            return;
        }

        logger.debug("Add applied op: {}", new Dependency(locatorKey, timestamp));

        Map<Long, OpStatus> pendingOps = shortNodeIdToPendingOps.get(VersionUtil.extractShortNodeId(timestamp));
        //Note: We can get applied ops that weren't pending because they don't have deps
        if (pendingOps == null) {
            pendingOps = new HashMap<Long, OpStatus>();
            shortNodeIdToPendingOps.put(VersionUtil.extractShortNodeId(timestamp), pendingOps);
        }

        pendingOps.put(timestamp, OpStatus.APPLIED);

        //respond to any blocked dep checks on this op
        Queue<DepCheckReplyInfo> blockedQueue = blockedDepChecks.get(timestamp);
        if (blockedQueue != null) {
            for (DepCheckReplyInfo dcri : blockedQueue) {
                dcri.receiveResponseAndReply();
            }
            blockedDepChecks.remove(timestamp);
        }
    }

    public static synchronized void checkGroupDependency(GroupDependencyCheck groupDepCheck, final Message depCheckMessage, final String id, GroupDepCheckCallback localGroupDepCheckCallback) {

        Set<Long> blockedDeps = new HashSet<Long>();
        Byte localDC = ShortNodeId.getLocalDC();

        // Skip those deps original from local DC and those already satisfied
        for (Dependency dep : groupDepCheck.getDependencies()) {

            long depTS = dep.getTimestamp();
            // Don't check dependencies for values written in this DC, we know they've been applied
            if (VersionUtil.extractDatacenter(depTS) == localDC) {
                continue;
            }

            if (logger.isDebugEnabled() && depCheckMessage != null) {
                logger.debug("Check dependency: {}, (dcm.lt={})", dep, depCheckMessage.getLamportTimestamp());
            }

            // pendingOps: all ops from the original node producing this dep version
            Map<Long, OpStatus> pendingOps = shortNodeIdToPendingOps.get(VersionUtil.extractShortNodeId(depTS));

            //no pendingOps => nothing's been received from that node yet
            if (pendingOps == null || pendingOps.size() == 0) {
                blockedDeps.add(depTS);
                continue;
            }

            OpStatus opStatus = pendingOps.get(depTS);

            // this version has already been applied
            if (opStatus != null && opStatus == OpStatus.APPLIED) {
                continue;
            }

            /**
             * The next three ifs are optimization from Eiger.
             * I'm not sure these conditions/assumptions are necessary true in K2
             * So we disable those checking for now
             */
            /*
            //first op pending => nothing's been applied yet
            if (pendingOps.get(pendingOps.firstKey()) == OpStatus.PENDING) {
                blockedDeps.add(depTS);
                continue;
            }

            //firstKey and everything older than it have been applied => respond immediately
            if (depTS <= pendingOps.firstKey()) { continue; }

            //lastKey is the mostly recently received op from this node => hasn't been applied yet
            if (depTS > pendingOps.lastKey()) {
                blockedDeps.add(depTS);
                continue;
            }
            */

            if (opStatus == null || opStatus == OpStatus.PENDING) {
                blockedDeps.add(depTS);
                continue;
            }
        } // End traversing through deps

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

        //TODO add timeouts to cause an error if they are never satisfied

        for (Long timestamp : deps) {
            Queue<DepCheckReplyInfo> blockedQueue = blockedDepChecks.get(timestamp);
            if (blockedQueue == null) {
                blockedQueue = new LinkedList<DepCheckReplyInfo>();
                blockedDepChecks.put(timestamp, blockedQueue);
            }
            blockedQueue.add(depCheckReplyInfo);
        }
    }

    private static void sendDepCheckReply(Message depCheckMessage, String id) {
        logger.debug("Send dependency check reply. (dcm.lt={})", depCheckMessage.getLamportTimestamp());

        byte[] empty = new byte[0];
        Message reply = depCheckMessage.getReply(FBUtilities.getBroadcastAddress(), empty, depCheckMessage.getVersion());
        MessagingService.instance().sendReply(reply, id, depCheckMessage.getFrom());
    }

}
