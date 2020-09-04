package org.apache.cassandra.db;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.FastByteArrayInputStream;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.net.MessageProducer;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.Dep;
import org.apache.cassandra.utils.FBUtilities;

public class GroupDependencyCheck implements MessageProducer
{
    private static GroupDependencyCheckSerializer serializer_ = new GroupDependencyCheckSerializer();

    public static GroupDependencyCheckSerializer serializer()
    {
        return serializer_;
    }

    private final Set<Dependency> dependencies;
    private final InetAddress inquiringNode;

    public GroupDependencyCheck(Set<Dependency> dependencies)
    {
        this.dependencies = dependencies;
        this.inquiringNode = DatabaseDescriptor.getListenAddress();
    }

    public GroupDependencyCheck(Set<Dependency> dependencies, InetAddress inquiringNode)
    {
        this.dependencies = dependencies;
        this.inquiringNode = inquiringNode;
    }

    public Set<Dependency> getDependencies()
    {
        return dependencies;
    }

    public InetAddress getInquiringNode()
    {
        return inquiringNode;
    }

    public static GroupDependencyCheck fromBytes(byte[] raw, int version) throws IOException
    {
        return serializer_.deserialize(new DataInputStream(new FastByteArrayInputStream(raw)), version);
    }

    public static class GroupDependencyCheckSerializer implements IVersionedSerializer<GroupDependencyCheck>
    {
        @Override
        public void serialize(GroupDependencyCheck groupDepCheck, DataOutput dos, int version) throws IOException
        {
            // size of the dependency set
            dos.writeInt(groupDepCheck.getDependencies().size());
            // each dependency element
            for (Dependency dep : groupDepCheck.getDependencies()) {
                Dependency.serializer().serialize(dep, dos);
            }

            // address length and inetaddress
            dos.writeInt(groupDepCheck.getInquiringNode().getAddress().length);
            dos.write(groupDepCheck.getInquiringNode().getAddress());
        }

        @Override
        public GroupDependencyCheck deserialize(DataInput dis, int version) throws IOException
        {
            // size of dependency set
            int depsSize = dis.readInt();
            // each dependency
            Set<Dependency> deps = new HashSet<Dependency>(depsSize);
            for (int i = 0; i < depsSize; i++) {
                deps.add(Dependency.serializer().deserialize(dis));
            }

            // inetaddress length and inetaddress
            int addrSize = dis.readInt();
            byte[] rawAddr = new byte[addrSize];
            dis.readFully(rawAddr);
            InetAddress addr = InetAddress.getByAddress(rawAddr);

            return new GroupDependencyCheck(deps, addr);
        }

        @Override
        public long serializedSize(GroupDependencyCheck depCheck, int version)
        {
            // size of dep set: int
            long size = DBConstants.intSize;
            // plus size of all dep
            for (Dependency dep : depCheck.getDependencies()) {
                size += (Dependency.serializer().serializedSize(dep));
            }

            // inetaddress length and inetaddress
            size += DBConstants.intSize;
            size += depCheck.getInquiringNode().getAddress().length;

            return size;
        }
    }

    @Override
    public Message getMessage(Integer version) throws IOException
    {
        DataOutputBuffer dob = new DataOutputBuffer();
        serializer_.serialize(this, dob, version);

        return new Message(FBUtilities.getBroadcastAddress(), StorageService.Verb.GROUP_DEPENDENCY_CHECK, Arrays.copyOf(dob.getData(), dob.getLength()), version);
    }
}
