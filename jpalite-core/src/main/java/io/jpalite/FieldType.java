/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.jpalite;

import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.descriptors.WireType;

public enum FieldType
{
	TYPE_BOOLEAN(Type.BOOL, null),
	TYPE_INTEGER(Type.INT32, null),
	TYPE_LONGLONG(Type.INT64, null),
	TYPE_DOUBLEDOUBLE(Type.DOUBLE, null),
	TYPE_BOOL(Type.BOOL, null),
	TYPE_INT(Type.INT32, null),
	TYPE_LONG(Type.INT64, null),
	TYPE_DOUBLE(Type.DOUBLE, null),
	TYPE_STRING(Type.STRING, null),
	TYPE_TIMESTAMP(Type.FIXED64, null),
	TYPE_LOCALTIME(Type.FIXED64, null),
	TYPE_CUSTOMTYPE(Type.MESSAGE, null),
	TYPE_ENUM(Type.STRING, null),
	TYPE_ORDINAL_ENUM(Type.INT32, null),
	TYPE_BYTES(Type.BYTES, null),
	TYPE_OBJECT(Type.BYTES, null),
	TYPE_ENTITY(Type.MESSAGE, null);

	private final String type;
	private final Type protoType;

	FieldType(Type protoType, String type)
	{
		this.protoType = protoType;
		this.type = type;
	}

	public String getProtoType()
	{
		return protoType.equals(Type.MESSAGE) ? type : protoType.toString();
	}

	public int getWireTypeTag(int fieldNr)
	{
		return WireType.makeTag(fieldNr, protoType.getWireType());
	}

	public static FieldType fieldType(Class<?> fieldType)
	{
		return switch (fieldType.getSimpleName()) {
			case "Boolean" -> TYPE_BOOLEAN;
			case "Integer" -> TYPE_INTEGER;
			case "Long" -> TYPE_LONGLONG;
			case "Double" -> TYPE_DOUBLEDOUBLE;
			case "boolean" -> TYPE_BOOL;
			case "int" -> TYPE_INT;
			case "long" -> TYPE_LONG;
			case "double" -> TYPE_DOUBLE;
			case "String" -> TYPE_STRING;
			case "Timestamp" -> TYPE_TIMESTAMP;
			case "LocalDateTime" -> TYPE_LOCALTIME;
			case "byte[]", "byte[][]" -> TYPE_BYTES;
			default -> {
				if (JPAEntity.class.isAssignableFrom(fieldType)) {
					yield TYPE_ENTITY;
				}

				yield TYPE_OBJECT;
			}
		};
	}//fieldType
}
