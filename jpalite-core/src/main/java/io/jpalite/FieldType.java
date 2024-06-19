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

public enum FieldType
{
	TYPE_BOOLEAN,
	TYPE_INTEGER,
	TYPE_LONGLONG,
	TYPE_DOUBLEDOUBLE,
	TYPE_BOOL,
	TYPE_INT,
	TYPE_LONG,
	TYPE_DOUBLE,
	TYPE_STRING,
	TYPE_TIMESTAMP,
	TYPE_LOCALTIME,
	TYPE_ENUM,
	TYPE_BYTES,
	TYPE_OBJECT,
	TYPE_ENTITY;

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
			case "EnumType" -> TYPE_ENUM;
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
