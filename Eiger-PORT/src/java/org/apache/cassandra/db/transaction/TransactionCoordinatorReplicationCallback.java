package org.apache.cassandra.db.transaction;

import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.ICompletable;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.service.StorageProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TransactionCoordinatorReplicationCallback implements IAsyncCallback
{
    private static Logger logger = LoggerFactory.getLogger(TransactionCoordinatorReplicationCallback.class);

    private final long startTime;
    private int expectedResponses;
    private int responses = 0;

    public TransactionCoordinatorReplicationCallback(int expectedResponses)
    {

        this.startTime = System.currentTimeMillis();
        this.expectedResponses = expectedResponses;

    }

    @Override
    public boolean isLatencyForSnitch()
    {
        //not on the read path
        return false;
    }

    @Override
    synchronized public void response(Message msg)
    {
        responses++;

        if (logger.isDebugEnabled()) {
            logger.debug("Response " + responses + "/" + expectedResponses);
        }

        // == to make sure this is called at most once
        if (responses == expectedResponses) {
            TransactionProxy.numReplicationsReceived.incrementAndGet();
            /*
            if (TransactionProxy.FLOW_CONTROL) {
                try {
                    StorageProxy.outstandingOps.poll(10000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.info(e.toString());
                }
            }
            */
        }
    }

}
