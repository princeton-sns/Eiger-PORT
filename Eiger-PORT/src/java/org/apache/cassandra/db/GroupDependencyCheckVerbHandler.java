package org.apache.cassandra.db;

import java.io.IOException;

import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDependencyCheckVerbHandler implements IVerbHandler
{
    private static Logger logger_ = LoggerFactory.getLogger(DependencyCheckVerbHandler.class);

    @Override
    public void doVerb(Message message, String id)
    {

        GroupDependencyCheck groupDepCheck = null;
        try {
            groupDepCheck = GroupDependencyCheck.fromBytes(message.getMessageBody(), message.getVersion());
        } catch (IOException e) {
            logger_.error("Error in decoding groupDependencyCheck");
        }
        //AppliedOperations2.checkGroupDependency(groupDepCheck, message, id);
        AppliedOperations2.checkGroupDependency(groupDepCheck, message, id, null);
    }
}
