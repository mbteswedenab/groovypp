/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package org.mbte.gretty.cassandra.thrift;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Collections;

import org.apache.thrift.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.protocol.*;

/**
 * invalid authorization request (user does not have access to keyspace)
 */
public class AuthorizationException extends Exception implements TBase<AuthorizationException, AuthorizationException._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("AuthorizationException");

  private static final TField WHY_FIELD_DESC = new TField("why", TType.STRING, (short)1);

  public String why;

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements TFieldIdEnum {
    WHY((short)1, "why");

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
        case 1: // WHY
          return WHY;
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

  public static final Map<_Fields, FieldMetaData> metaDataMap;
  static {
    Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.WHY, new FieldMetaData("why", TFieldRequirementType.REQUIRED, 
        new FieldValueMetaData(TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(AuthorizationException.class, metaDataMap);
  }

  public AuthorizationException() {
  }

  public AuthorizationException(
    String why)
  {
    this();
    this.why = why;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public AuthorizationException(AuthorizationException other) {
    if (other.isSetWhy()) {
      this.why = other.why;
    }
  }

  public AuthorizationException deepCopy() {
    return new AuthorizationException(this);
  }

  @Deprecated
  public AuthorizationException clone() {
    return new AuthorizationException(this);
  }


  public void clear() {
    this.why = null;
  }

  public String getWhy() {
    return this.why;
  }

  public AuthorizationException setWhy(String why) {
    this.why = why;
    return this;
  }

  public void unsetWhy() {
    this.why = null;
  }

  /** Returns true if field why is set (has been asigned a value) and false otherwise */
  public boolean isSetWhy() {
    return this.why != null;
  }

  public void setWhyIsSet(boolean value) {
    if (!value) {
      this.why = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case WHY:
      if (value == null) {
        unsetWhy();
      } else {
        setWhy((String)value);
      }
      break;

    }
  }

  public void setFieldValue(int fieldID, Object value) {
    setFieldValue(_Fields.findByThriftIdOrThrow(fieldID), value);
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case WHY:
      return getWhy();

    }
    throw new IllegalStateException();
  }

  public Object getFieldValue(int fieldId) {
    return getFieldValue(_Fields.findByThriftIdOrThrow(fieldId));
  }

  /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    switch (field) {
    case WHY:
      return isSetWhy();
    }
    throw new IllegalStateException();
  }

  public boolean isSet(int fieldID) {
    return isSet(_Fields.findByThriftIdOrThrow(fieldID));
  }


  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AuthorizationException)
      return this.equals((AuthorizationException)that);
    return false;
  }

  public boolean equals(AuthorizationException that) {
    if (that == null)
      return false;

    boolean this_present_why = true && this.isSetWhy();
    boolean that_present_why = true && that.isSetWhy();
    if (this_present_why || that_present_why) {
      if (!(this_present_why && that_present_why))
        return false;
      if (!this.why.equals(that.why))
        return false;
    }

    return true;
  }


  public int hashCode() {
    return 0;
  }

  public int compareTo(AuthorizationException other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    AuthorizationException typedOther = (AuthorizationException)other;

    lastComparison = Boolean.valueOf(isSetWhy()).compareTo(typedOther.isSetWhy());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetWhy()) {      lastComparison = TBaseHelper.compareTo(this.why, typedOther.why);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public void read(TProtocol iprot) throws TException {
    TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 1: // WHY
          if (field.type == TType.STRING) {
            this.why = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();

    // check for required fields of primitive type, which can't be checked in the validate method
    validate();
  }

  public void write(TProtocol oprot) throws TException {
    validate();

    oprot.writeStructBegin(STRUCT_DESC);
    if (this.why != null) {
      oprot.writeFieldBegin(WHY_FIELD_DESC);
      oprot.writeString(this.why);
      oprot.writeFieldEnd();
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }


  public String toString() {
    StringBuilder sb = new StringBuilder("AuthorizationException(");
    boolean first = true;

    sb.append("why:");
    if (this.why == null) {
      sb.append("null");
    } else {
      sb.append(this.why);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws TException {
    // check for required fields
    if (why == null) {
      throw new TProtocolException("Required field 'why' was not present! Struct: " + toString());
    }
  }

}

