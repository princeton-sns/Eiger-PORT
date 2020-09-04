package org.apache.cassandra.db;

import org.apache.cassandra.net.ICompletable;
import org.apache.cassandra.service.IWriteResponseHandler;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Created by khiem on 4/10/16.
 */
public class ReplicationCompletion implements ICompletable {

    private static Logger logger_ = LoggerFactory.getLogger(ReplicationCompletion.class);

    // Data fields here

    public ReplicationCompletion() {

    }

    // Do something after all replication messages have been received
    @Override
    public void complete() {

        if (logger_.isDebugEnabled()) {
            logger_.debug("Replication to other datacenters is done.");
        }
        /*
        try {
            StorageProxy.numReplicationsReceived.incrementAndGet();
            if (StorageProxy.FLOW_CONTROL) {
                StorageProxy.outstandingOps.poll(10000, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger_.info(e.toString());
        }
        */
    }
}