/**
 * Autogenerated by Thrift Compiler (0.7.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package org.apache.cassandra.thrift;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import org.apache.commons.lang.builder.HashCodeBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dep(endencies) indicate the operations that should appear before a
 *  given value or values in each datacenter.
 *  These are only used on the write path and will be the same for
 *  all parts of a write.  They should be included in each write call.
 * 
 * @param locator_key the key to send a dep_check on this dependency to.  For insert, it's simply the key.  For put-transactions it's the coordinator key.
 * @timestamp the timestamp of the earlier operation, this uniquely identifies it
 */
public class Dep implements org.apache.thrift.TBase<Dep, Dep._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Dep");

  private static final org.apache.thrift.protocol.TField LOCATOR_KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("locator_key", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField TIMESTAMP_FIELD_DESC = new org.apache.thrift.protocol.TField("timestamp", org.apache.thrift.protocol.TType.I64, (short)2);

  public ByteBuffer locator_key; // required
  public long timestamp; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    LOCATOR_KEY((short)1, "locator_key"),
    TIMESTAMP((short)2, "timestamp");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // LOCATOR_KEY
          return LOCATOR_KEY;
        case 2: // TIMESTAMP
          return TIMESTAMP;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __TIMESTAMP_ISSET_ID = 0;
  private BitSet __isset_bit_vector = new BitSet(1);

  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.LOCATOR_KEY, new org.apache.thrift.meta_data.FieldMetaData("locator_key", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.TIMESTAMP, new org.apache.thrift.meta_data.FieldMetaData("timestamp", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Dep.class, metaDataMap);
  }

  public Dep() {
  }

  public Dep(
    ByteBuffer locator_key,
    long timestamp)
  {
    this();
    this.locator_key = locator_key;
    this.timestamp = timestamp;
    setTimestampIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Dep(Dep other) {
    __isset_bit_vector.clear();
    __isset_bit_vector.or(other.__isset_bit_vector);
    if (other.isSetLocator_key()) {
      this.locator_key = org.apache.thrift.TBaseHelper.copyBinary(other.locator_key);
;
    }
    this.timestamp = other.timestamp;
  }

  public Dep deepCopy() {
    return new Dep(this);
  }

  @Override
  public void clear() {
    this.locator_key = null;
    setTimestampIsSet(false);
    this.timestamp = 0;
  }

  public byte[] getLocator_key() {
    setLocator_key(org.apache.thrift.TBaseHelper.rightSize(locator_key));
    return locator_key == null ? null : locator_key.array();
  }

  public ByteBuffer bufferForLocator_key() {
    return locator_key;
  }

  public Dep setLocator_key(byte[] locator_key) {
    setLocator_key(locator_key == null ? (ByteBuffer)null : ByteBuffer.wrap(locator_key));
    return this;
  }

  public Dep setLocator_key(ByteBuffer locator_key) {
    this.locator_key = locator_key;
    return this;
  }

  public void unsetLocator_key() {
    this.locator_key = null;
  }

  /** Returns true if field locator_key is set (has been assigned a value) and false otherwise */
  public boolean isSetLocator_key() {
    return this.locator_key != null;
  }

  public void setLocator_keyIsSet(boolean value) {
    if (!value) {
      this.locator_key = null;
    }
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public Dep setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    setTimestampIsSet(true);
    return this;
  }

  public void unsetTimestamp() {
    __isset_bit_vector.clear(__TIMESTAMP_ISSET_ID);
  }

  /** Returns true if field timestamp is set (has been assigned a value) and false otherwise */
  public boolean isSetTimestamp() {
    return __isset_bit_vector.get(__TIMESTAMP_ISSET_ID);
  }

  public void setTimestampIsSet(boolean value) {
    __isset_bit_vector.set(__TIMESTAMP_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case LOCATOR_KEY:
      if (value == null) {
        unsetLocator_key();
      } else {
        setLocator_key((ByteBuffer)value);
      }
      break;

    case TIMESTAMP:
      if (value == null) {
        unsetTimestamp();
      } else {
        setTimestamp((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case LOCATOR_KEY:
      return getLocator_key();

    case TIMESTAMP:
      return Long.valueOf(getTimestamp());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case LOCATOR_KEY:
      return isSetLocator_key();
    case TIMESTAMP:
      return isSetTimestamp();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Dep)
      return this.equals((Dep)that);
    return false;
  }

  public boolean equals(Dep that) {
    if (that == null)
      return false;

    boolean this_present_locator_key = true && this.isSetLocator_key();
    boolean that_present_locator_key = true && that.isSetLocator_key();
    if (this_present_locator_key || that_present_locator_key) {
      if (!(this_present_locator_key && that_present_locator_key))
        return false;
      if (!this.locator_key.equals(that.locator_key))
        return false;
    }

    boolean this_present_timestamp = true;
    boolean that_present_timestamp = true;
    if (this_present_timestamp || that_present_timestamp) {
      if (!(this_present_timestamp && that_present_timestamp))
        return false;
      if (this.timestamp != that.timestamp)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_locator_key = true && (isSetLocator_key());
    builder.append(present_locator_key);
    if (present_locator_key)
      builder.append(locator_key);

    boolean present_timestamp = true;
    builder.append(present_timestamp);
    if (present_timestamp)
      builder.append(timestamp);

    return builder.toHashCode();
  }

  public int compareTo(Dep other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    Dep typedOther = (Dep)other;

    lastComparison = Boolean.valueOf(isSetLocator_key()).compareTo(typedOther.isSetLocator_key());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLocator_key()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.locator_key, typedOther.locator_key);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetTimestamp()).compareTo(typedOther.isSetTimestamp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTimestamp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.timestamp, typedOther.timestamp);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    org.apache.thrift.protocol.TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == org.apache.thrift.protocol.TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 1: // LOCATOR_KEY
          if (field.type == org.apache.thrift.protocol.TType.STRING) {
            this.locator_key = iprot.readBinary();
          } else { 
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 2: // TIMESTAMP
          if (field.type == org.apache.thrift.protocol.TType.I64) {
            this.timestamp = iprot.readI64();
            setTimestampIsSet(true);
          } else { 
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          org.apache.thrift.protocol.TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();

    // check for required fields of primitive type, which can't be checked in the validate method
    if (!isSetTimestamp()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'timestamp' was not found in serialized data! Struct: " + toString());
    }
    validate();
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    validate();

    oprot.writeStructBegin(STRUCT_DESC);
    if (this.locator_key != null) {
      oprot.writeFieldBegin(LOCATOR_KEY_FIELD_DESC);
      oprot.writeBinary(this.locator_key);
      oprot.writeFieldEnd();
    }
    oprot.writeFieldBegin(TIMESTAMP_FIELD_DESC);
    oprot.writeI64(this.timestamp);
    oprot.writeFieldEnd();
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Dep(");
    boolean first = true;

    sb.append("locator_key:");
    if (this.locator_key == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.locator_key, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("timestamp:");
    sb.append(this.timestamp);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (locator_key == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'locator_key' was not present! Struct: " + toString());
    }
    // alas, we cannot check 'timestamp' because it's a primitive and you chose the non-beans generator.
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bit_vector = new BitSet(1);
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

}

