package org.apache.cassandra.db.transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NavigableSet;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.Column;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ColumnSerializer;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.db.marshal.MarshalException;
import org.apache.cassandra.utils.Allocator;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.HeapAllocator;

public class PendingTransactionColumn extends Column
{
    /*
     * PendingTransactionColumns (PTC) are place holders that indicate there is a
     * pending transaction that will commit at some time after the pendingTime.
     * Normal Column processing should work with them, with the following
     * exceptions:
     *   - No value should be returned to a client
     *     ~ Done in ThriftConverter.thriftifyPendingTransactionColumn()
     *   - When being reconciled it limits the lvt of the normal column to be
     *     max(PTC.evt - 1, normal.evt)
     *     ~ Done in Column.reconcile()
     *   - Always keep previousVersions if either column is a PTC
     *     ~ Done in Column.updatePreviousVersion()
     *   - valueByTime resulting in querying the coordinators of all transaction that could determine the value at the given time
     *     ~ Done in CassandraServer.multiget_slice_by_time, should also be done in CassandraServer.*_by_time
     */

    public PendingTransactionColumn(ByteBuffer name, long transactionId, long pendingTime)
    {
        this(name, ByteBufferUtil.bytes(transactionId), pendingTime);
    }

    public PendingTransactionColumn(ByteBuffer name, ByteBuffer value, long timestamp)
    {
        this(name, value, timestamp, null, null, timestamp, null, null, null, -1, -1);
    }

    public PendingTransactionColumn(ByteBuffer name, ByteBuffer value, long timestamp, Long lastAccessTime, Long previousVersionLastAccessTime, Long earliestValidTime, Long latestValidTime, NavigableSet<IColumn> previousVersions, ByteBuffer transactionCoordinatorKey, long client_id, long gst)
    {
        super(name, value, timestamp, lastAccessTime, previousVersionLastAccessTime, earliestValidTime, latestValidTime, previousVersions, transactionCoordinatorKey, client_id, gst);
    }

    public PendingTransactionColumn(ByteBuffer name, long transactionId, long pendingTime, ByteBuffer transactionCoordinatorKey) {
        this(name, ByteBufferUtil.bytes(transactionId), pendingTime, null, null, pendingTime, null, null, transactionCoordinatorKey, -1, -1);
    }

    @Override
    public IColumn localCopy(ColumnFamilyStore cfs) {
        return localCopy(cfs, HeapAllocator.instance);
    }

    @Override
    public IColumn localCopy(ColumnFamilyStore cfs, Allocator allocator) {
        return new PendingTransactionColumn(cfs.internOrCopy(name, allocator), allocator.clone(value), timestamp, lastAccessTime, lastAccessTimeOfAPreviousVersion, earliestValidTime, latestValidTime, previousVersions, transactionCoordinatorKey, client_id, gst);
    }

    @Override
    public int serializationFlags()
    {
        return ColumnSerializer.PENDING_TRANSACTION_MASK;
    }

    @Override
    public void validateFields(CFMetaData metadata) throws MarshalException
    {
        validateName(metadata);
        if (value().remaining() != 4) {
            throw new MarshalException("A transactionId value should be 4 bytes long");
        }
    }

    public long getTransactionId()
    {
        return ByteBufferUtil.toLong(value);
    }
}
