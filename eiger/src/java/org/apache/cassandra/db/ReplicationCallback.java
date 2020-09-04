package org.apache.cassandra.db;

import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.ICompletable;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.utils.ShortNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class ReplicationCallback implements IAsyncCallback
{
    private static Logger logger_ = LoggerFactory.getLogger(ReplicationCallback.class);

    private final long startTime;
    private int expectedResponses;
    private int responses = 0;
    private final ICompletable completable;

    public ReplicationCallback(int expectedResponses, ICompletable completable)
    {

        this.startTime = System.currentTimeMillis();
        this.expectedResponses = expectedResponses;
        this.completable = completable;
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

        if (logger_.isDebugEnabled()) {
            logger_.debug("Response " + responses + "/" + expectedResponses + " for " + completable);
        }

        // == to make sure this is called at most once
        if (responses == expectedResponses) {
            completable.complete();
        }
    }

}