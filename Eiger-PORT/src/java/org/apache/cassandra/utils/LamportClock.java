package org.apache.cassandra.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import org.apache.cassandra.thrift.Cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There should only be one LamportClock for each process, so this class is a
 * singleton (all static).
 *
 * @author wlloyd
 *
 */
public class LamportClock {
    //private static Logger logger = LoggerFactory.getLogger(LamportClock.class);

    //NO_CLOCK_TICK should only be used when calling function *locally* on the node
    public static final long NO_CLOCK_TICK = -1;

    //COPS_UNSUPPORTED should only be used in code that we don't intend to support like Hadoop on top of cassandra
    public static final long COPS_UNSUPPORTED = -2;

    private static AtomicLong logicalTime = new AtomicLong();
    private static Short localId = null;

    // Haonan: readts map used for each client machine
    // all logical clients on the same client machine have the same read_ts (this is safe b/c read_ts is gst which is
    // in the safe zone.)
    public static final int num_svrs = 8;
    // Haonan: serverToTS map for each client
    private static HashMap<String, Long> ReadTSMap = new HashMap<String, Long>(num_svrs);
    // hardcoded number of servers!!
    private static long read_ts = 0L;

    // Haonan: clients readTS map for GC
    // hardcoded number of client machines:
    public static final int num_clients = 8;
    public static HashMap<Long, Long> gc_map = new HashMap<Long, Long>(num_clients);
    static Random rand = new Random();
    public static final long nodeIP
            = new Long(rand.nextInt(Integer.MAX_VALUE)).longValue(); // this is for GC, one nodeIP per client machine

    // for a server's local safe time: lst
    public static long local_safe_time = 0L;
    // note: on local machine, each pending write txn is tagged by its pending timestamp, which is the Lamport clock
    // version when the write txn is received. so we do not need to record write txn ids. It is possible multuple
    // concurrent write txns have the same pending time.
    // <pending_time, txn_count>: pending_time is the timestamp that
    // identifies a txn; it is possible multiple txns come in during the same version, so we use a counter for each
    // pending_time.
    // Lamport clock versions are monotonically increasing, so we do not need a sortedset or sortedmap, we only need
    // to preserve the interstion order, e.g., linkedhashmap, which gives us o(1) insertion and retrieval time.
    public static LinkedHashMap<Long, Integer> running_write_txns = new LinkedHashMap<Long, Integer>();

    private LamportClock() {
        //don't instantiate me
    }

    //localId must be set before calling getVersion, otherwise you'll get a null exception

    /**
     * @return next "version" for this node, version is timestamp + nodeid
     */
    public static long getVersion() {
        long localTime = logicalTime.incrementAndGet();
        long version = (localTime << 16) + localId.shortValue();
        //logger.debug("getVersion {} = {} << 16 + {}", new Object[]{version, localTime, localId.shortValue()});
        return version;
    }

    //Should only be used for sanity checking
    public static long currentVersion() {
        return (logicalTime.get() << 16) + localId.shortValue();
    }


    public static long sendTimestamp() {
        long newLocalTime = logicalTime.incrementAndGet();
        //logger.debug("sendTimestamp({})", newLocalTime);
        return newLocalTime;
    }

    //public static void updateTime(long updateTime) {
    public static synchronized void updateTime(long updateTime) {
        if (updateTime == NO_CLOCK_TICK) {
            //logger.debug("updateTimestamp(NO_CLOCK_TICK == {})", updateTime);
            return;
        }

        long localTime = logicalTime.longValue();
        long timeDiff = updateTime - localTime;

        long resultTime;
        if (timeDiff < 0) {
            resultTime = logicalTime.incrementAndGet();
        } else {
            resultTime = logicalTime.addAndGet(timeDiff+1);
        }
        //logger.debug("updateTimestamp({},{}) = {}", new Object[]{updateTime, localTime, resultTime});
    }

    public static void setLocalId(short localId2) {
        localId = localId2;
    }

    // Haonan: manage gc_map
    public static synchronized void setReadTS(long nodeIP, long read_ts) {
        // put nodeip and readts into gc_map
        if (!gc_map.containsKey(nodeIP) || gc_map.get(nodeIP) < read_ts) {
            gc_map.put(nodeIP, read_ts);
        }
    }

    // Haonan: manage readts map called from ClientLibrary
    public static long getReadTS() {
        //Haonan: find the readTS based on readTSMap on *ALL* servers: GST = min of LSTs of all servers
        if (ReadTSMap.size() < num_svrs) {
            // we don't have lst for all servers yet, return current read_ts, which should be 0.
            return read_ts;
        } else if (ReadTSMap.size() > num_svrs) {
            assert false : "ReadTSMap becomes inconsistent.";
        }
        // find gst = min lst across all servers
        long rts = Long.MAX_VALUE;
        for (Long lst : ReadTSMap.values()) {
            if (lst < rts) {
                rts = lst;
            }
        }
        updateReadTS(rts);
        return read_ts;
    }

    private static synchronized void updateReadTS(long rts) {
        // ensures read_ts is monotonically increasing.
        if (read_ts < rts) {
            assert (rts != Long.MAX_VALUE);
            read_ts = rts;
        }
    }

    public static synchronized void updateReadTsMap(String server, long lst) {
        // node: updating readTsMap doesn't require sync, b/c it only matters to staleness and correctness is
        // guaranteed as long as read_ts is advanced monotonically.
        if (!ReadTSMap.containsKey(server) || ReadTSMap.get(server) < lst) {
            ReadTSMap.put(server, lst);
        }
    }

    public static long read_ts() {
        // return the current read_ts, this is called by wtxns
        return read_ts;
    }

    public static synchronized long record_pending_time() {
        //synchronized (running_write_txns) {
            long version = currentVersion();
            int counter = 1;
            if (running_write_txns.containsKey(version)) {
                counter = running_write_txns.get(version) + 1;
            }
            running_write_txns.put(version, counter);
            return version;
        //}
    }

    public static synchronized void remove_pending_time(long pending_time) {
        //synchronized (running_write_txns) {
            if (!running_write_txns.containsKey(pending_time)) {
                assert false : "We should have recorded this pending time.";
                return;
            }
            int counter = running_write_txns.get(pending_time);
            if (counter == 1) {
                running_write_txns.remove(pending_time);
            } else {
                running_write_txns.put(pending_time, --counter);
            }
            // update local safe time
            long newLST = running_write_txns.isEmpty() ? LamportClock.currentVersion()
                    : running_write_txns.entrySet().iterator().next().getKey();
            if (local_safe_time < newLST) {
                local_safe_time = newLST;
            }
            return;
        //}
    }
}
