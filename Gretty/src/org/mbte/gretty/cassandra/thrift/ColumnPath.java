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
import java.nio.ByteBuffer;

import org.apache.thrift.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.protocol.*;

/**
 * The ColumnPath is the path to a single column in Cassandra. It might make sense to think of ColumnPath and
 * ColumnParent in terms of a directory structure.
 * 
 * ColumnPath is used to looking up a single column.
 * 
 * @param column_family. The name of the CF of the column being looked up.
 * @param super_column. The super column name.
 * @param column. The column name.
 */
public class ColumnPath implements TBase<ColumnPath, ColumnPath._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("ColumnPath");

  private static final TField COLUMN_FAMILY_FIELD_DESC = new TField("column_family", TType.STRING, (short)3);
  private static final TField SUPER_COLUMN_FIELD_DESC = new TField("super_column", TType.STRING, (short)4);
  private static final TField COLUMN_FIELD_DESC = new TField("column", TType.STRING, (short)5);

  public String column_family;
  public ByteBuffer super_column;
  public ByteBuffer column;

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements TFieldIdEnum {
    COLUMN_FAMILY((short)3, "column_family"),
    SUPER_COLUMN((short)4, "super_column"),
    COLUMN((short)5, "column");

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
        case 3: // COLUMN_FAMILY
          return COLUMN_FAMILY;
        case 4: // SUPER_COLUMN
          return SUPER_COLUMN;
        case 5: // COLUMN
          return COLUMN;
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
    tmpMap.put(_Fields.COLUMN_FAMILY, new FieldMetaData("column_family", TFieldRequirementType.REQUIRED, 
        new FieldValueMetaData(TType.STRING)));
    tmpMap.put(_Fields.SUPER_COLUMN, new FieldMetaData("super_column", TFieldRequirementType.OPTIONAL, 
        new FieldValueMetaData(TType.STRING)));
    tmpMap.put(_Fields.COLUMN, new FieldMetaData("column", TFieldRequirementType.OPTIONAL, 
        new FieldValueMetaData(TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(ColumnPath.class, metaDataMap);
  }

  public ColumnPath() {
  }

  public ColumnPath(
    String column_family)
  {
    this();
    this.column_family = column_family;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ColumnPath(ColumnPath other) {
    if (other.isSetColumn_family()) {
      this.column_family = other.column_family;
    }
    if (other.isSetSuper_column()) {
      this.super_column = ByteBuffer.wrap(new byte[other.super_column.limit() - other.super_column.arrayOffset()]);
      System.arraycopy(other.super_column.array(), other.super_column.arrayOffset(), super_column.array(), 0, other.super_column.limit() - other.super_column.arrayOffset());
    }
    if (other.isSetColumn()) {
      this.column = ByteBuffer.wrap(new byte[other.column.limit() - other.column.arrayOffset()]);
      System.arraycopy(other.column.array(), other.column.arrayOffset(), column.array(), 0, other.column.limit() - other.column.arrayOffset());
    }
  }

  public ColumnPath deepCopy() {
    return new ColumnPath(this);
  }

  @Deprecated
  public ColumnPath clone() {
    return new ColumnPath(this);
  }


  public void clear() {
    this.column_family = null;
    this.super_column = null;
    this.column = null;
  }

  public String getColumn_family() {
    return this.column_family;
  }

  public ColumnPath setColumn_family(String column_family) {
    this.column_family = column_family;
    return this;
  }

  public void unsetColumn_family() {
    this.column_family = null;
  }

  /** Returns true if field column_family is set (has been asigned a value) and false otherwise */
  public boolean isSetColumn_family() {
    return this.column_family != null;
  }

  public void setColumn_familyIsSet(boolean value) {
    if (!value) {
      this.column_family = null;
    }
  }

  public ByteBuffer getSuper_column() {
    return this.super_column;
  }

  public ColumnPath setSuper_column(ByteBuffer super_column) {
    this.super_column = super_column;
    return this;
  }

  public void unsetSuper_column() {
    this.super_column = null;
  }

  /** Returns true if field super_column is set (has been asigned a value) and false otherwise */
  public boolean isSetSuper_column() {
    return this.super_column != null;
  }

  public void setSuper_columnIsSet(boolean value) {
    if (!value) {
      this.super_column = null;
    }
  }

  public ByteBuffer getColumn() {
    return this.column;
  }

  public ColumnPath setColumn(ByteBuffer column) {
    this.column = column;
    return this;
  }

  public void unsetColumn() {
    this.column = null;
  }

  /** Returns true if field column is set (has been asigned a value) and false otherwise */
  public boolean isSetColumn() {
    return this.column != null;
  }

  public void setColumnIsSet(boolean value) {
    if (!value) {
      this.column = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case COLUMN_FAMILY:
      if (value == null) {
        unsetColumn_family();
      } else {
        setColumn_family((String)value);
      }
      break;

    case SUPER_COLUMN:
      if (value == null) {
        unsetSuper_column();
      } else {
        setSuper_column((ByteBuffer)value);
      }
      break;

    case COLUMN:
      if (value == null) {
        unsetColumn();
      } else {
        setColumn((ByteBuffer)value);
      }
      break;

    }
  }

  public void setFieldValue(int fieldID, Object value) {
    setFieldValue(_Fields.findByThriftIdOrThrow(fieldID), value);
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case COLUMN_FAMILY:
      return getColumn_family();

    case SUPER_COLUMN:
      return getSuper_column();

    case COLUMN:
      return getColumn();

    }
    throw new IllegalStateException();
  }

  public Object getFieldValue(int fieldId) {
    return getFieldValue(_Fields.findByThriftIdOrThrow(fieldId));
  }

  /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    switch (field) {
    case COLUMN_FAMILY:
      return isSetColumn_family();
    case SUPER_COLUMN:
      return isSetSuper_column();
    case COLUMN:
      return isSetColumn();
    }
    throw new IllegalStateException();
  }

  public boolean isSet(int fieldID) {
    return isSet(_Fields.findByThriftIdOrThrow(fieldID));
  }


  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ColumnPath)
      return this.equals((ColumnPath)that);
    return false;
  }

  public boolean equals(ColumnPath that) {
    if (that == null)
      return false;

    boolean this_present_column_family = true && this.isSetColumn_family();
    boolean that_present_column_family = true && that.isSetColumn_family();
    if (this_present_column_family || that_present_column_family) {
      if (!(this_present_column_family && that_present_column_family))
        return false;
      if (!this.column_family.equals(that.column_family))
        return false;
    }

    boolean this_present_super_column = true && this.isSetSuper_column();
    boolean that_present_super_column = true && that.isSetSuper_column();
    if (this_present_super_column || that_present_super_column) {
      if (!(this_present_super_column && that_present_super_column))
        return false;
      if (!this.super_column.equals(that.super_column))
        return false;
    }

    boolean this_present_column = true && this.isSetColumn();
    boolean that_present_column = true && that.isSetColumn();
    if (this_present_column || that_present_column) {
      if (!(this_present_column && that_present_column))
        return false;
      if (!this.column.equals(that.column))
        return false;
    }

    return true;
  }


  public int hashCode() {
    return 0;
  }

  public int compareTo(ColumnPath other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    ColumnPath typedOther = (ColumnPath)other;

    lastComparison = Boolean.valueOf(isSetColumn_family()).compareTo(typedOther.isSetColumn_family());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_family()) {      lastComparison = TBaseHelper.compareTo(this.column_family, typedOther.column_family);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSuper_column()).compareTo(typedOther.isSetSuper_column());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuper_column()) {      lastComparison = TBaseHelper.compareTo(this.super_column, typedOther.super_column);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetColumn()).compareTo(typedOther.isSetColumn());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn()) {      lastComparison = TBaseHelper.compareTo(this.column, typedOther.column);
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
        case 3: // COLUMN_FAMILY
          if (field.type == TType.STRING) {
            this.column_family = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 4: // SUPER_COLUMN
          if (field.type == TType.STRING) {
            this.super_column = iprot.readBinary();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 5: // COLUMN
          if (field.type == TType.STRING) {
            this.column = iprot.readBinary();
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
    if (this.column_family != null) {
      oprot.writeFieldBegin(COLUMN_FAMILY_FIELD_DESC);
      oprot.writeString(this.column_family);
      oprot.writeFieldEnd();
    }
    if (this.super_column != null) {
      if (isSetSuper_column()) {
        oprot.writeFieldBegin(SUPER_COLUMN_FIELD_DESC);
        oprot.writeBinary(this.super_column);
        oprot.writeFieldEnd();
      }
    }
    if (this.column != null) {
      if (isSetColumn()) {
        oprot.writeFieldBegin(COLUMN_FIELD_DESC);
        oprot.writeBinary(this.column);
        oprot.writeFieldEnd();
      }
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }


  public String toString() {
    StringBuilder sb = new StringBuilder("ColumnPath(");
    boolean first = true;

    sb.append("column_family:");
    if (this.column_family == null) {
      sb.append("null");
    } else {
      sb.append(this.column_family);
    }
    first = false;
    if (isSetSuper_column()) {
      if (!first) sb.append(", ");
      sb.append("super_column:");
      if (this.super_column == null) {
        sb.append("null");
      } else {
        TBaseHelper.toString(this.super_column, sb);
      }
      first = false;
    }
    if (isSetColumn()) {
      if (!first) sb.append(", ");
      sb.append("column:");
      if (this.column == null) {
        sb.append("null");
      } else {
        TBaseHelper.toString(this.column, sb);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws TException {
    // check for required fields
    if (column_family == null) {
      throw new TProtocolException("Required field 'column_family' was not present! Struct: " + toString());
    }
  }

}

