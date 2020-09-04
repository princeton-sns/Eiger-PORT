package org.apache.cassandra.service;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.db.Dependency;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.ICompletable;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDepCheckCallback implements IAsyncCallback
{
    private static Logger logger_ = LoggerFactory.getLogger(DepCheckCallback.class);

    private final long startTime;
    private int responses = 0;
    private final Map<InetAddress, Set<Dependency>> groupDeps;
    private final ICompletable completable;

    public GroupDepCheckCallback(Map<InetAddress, Set<Dependency>> groupDeps, ICompletable completable)
    {
        this.groupDeps = groupDeps;
        this.startTime = System.currentTimeMillis();
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
            logger_.debug("Response " + responses + "/" + groupDeps.size() + " for " + completable);
        }

        assert responses > 0 && responses <= groupDeps.size() : responses + "?" + groupDeps.size();
        if (responses == groupDeps.size()) {
            Runnable runnable = new WrappedRunnable() {
                @Override
                protected void runMayThrow() throws Exception {
                    completable.complete();
                }
            };
            StageManager.getStage(Stage.MUTATION).execute(runnable);
            //completable.complete();
        }
    }

    //Khiem TODO: Add a timeout that fires and complains if this hangs
}
