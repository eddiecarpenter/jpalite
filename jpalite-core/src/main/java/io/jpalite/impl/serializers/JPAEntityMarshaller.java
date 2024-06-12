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

package io.jpalite.impl.serializers;

import io.jpalite.*;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.annotations.impl.GeneratedMarshallerBase;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

import java.io.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JPAEntityMarshaller<T> extends GeneratedMarshallerBase implements ProtobufTagMarshaller<T>
{
	private final Class<T> entityClass;

	public JPAEntityMarshaller(Class<T> entityClass)
	{
		this.entityClass = entityClass;
	}

	@Override
	public Class<T> getJavaClass()
	{
		return entityClass;
	}

	@Override
	public String getTypeName()
	{
		return "org.tradeswitch." + entityClass.getSimpleName();
	}

	@Override
	public T read(ReadContext context) throws IOException
	{
		TagReader reader = context.getReader();

		try {
			EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityClass);

			T entity = (T) metaData.getNewEntity();

			int tag = reader.readTag();
			for (EntityField field : metaData.getEntityFields()) {
				if (tag == 0) {
					//if tag is zero we have reached the end
					break;
				}//if

				int fieldTag;
				if (field.getFieldType() == FieldType.TYPE_ENTITY) {
					EntityMetaData<?> entityMetaData = EntityMetaDataManager.getMetaData(field.getType());
					if (entityMetaData.getEntityType() == EntityType.ENTITY_EMBEDDABLE) {
						fieldTag = field.getFieldType().getWireTypeTag(field.getFieldNr());
					}//if
					else {
						fieldTag = entityMetaData.getIdField().getFieldType().getWireTypeTag(field.getFieldNr());
					}//else
				}//if
				else {
					fieldTag = field.getFieldType().getWireTypeTag(field.getFieldNr());
				}//else

				if (fieldTag == tag) {
					Object value = readField(context, reader, metaData, entity, field);
					field.invokeSetter(entity, value);
					tag = reader.readTag();
				}//if
			}//for
			((JPAEntity) entity)._clearModified();

			return entity;
		}//try
		catch (Throwable ex) {
			throw new IOException("Error reading Entity", ex);
		}//catch
	}//read

	@SuppressWarnings("java:S6205")//Block is not redundant
	private Object readField(ReadContext context, TagReader reader, EntityMetaData<?> metaData, T entity, EntityField field) throws Throwable
	{
		return switch (field.getFieldType()) {
			case TYPE_BOOLEAN, TYPE_BOOL -> reader.readBool();
			case TYPE_INTEGER, TYPE_INT -> reader.readInt32();
			case TYPE_LONGLONG, TYPE_LONG -> reader.readInt64();
			case TYPE_DOUBLEDOUBLE, TYPE_DOUBLE -> reader.readDouble();
			case TYPE_STRING -> reader.readString();
			case TYPE_ENUM -> {
				String enumName = reader.readString();
				for (Object enumVal : field.getType().getEnumConstants()) {
					if (((Enum) enumVal).name().equals(enumName)) {
						yield enumVal;
					}//if
				}//for

				yield null;
			}
			case TYPE_ORDINAL_ENUM -> field.getType().getEnumConstants()[reader.readInt32()];
			case TYPE_TIMESTAMP -> new Timestamp(reader.readFixed64());
			case TYPE_LOCALTIME -> new Timestamp(reader.readFixed64()).toLocalDateTime();
			case TYPE_BYTES -> reader.readByteArray();
			case TYPE_OBJECT -> {
				ByteArrayInputStream in = new ByteArrayInputStream(reader.readByteArray());
				ObjectInputStream stream = new ObjectInputStream(in);
				yield stream.readObject();
			}
			case TYPE_CUSTOMTYPE -> readNestedEntity(context, reader, field.getType());
			case TYPE_ENTITY -> {
				if (metaData.getEntityType() == EntityType.ENTITY_EMBEDDABLE) {
					yield readNestedEntity(context, reader, field.getType());
				}//if
				else {
					EntityMetaData<?> subMetaData = EntityMetaDataManager.getMetaData(field.getType());
					Object primaryKey;
					//If we have multiple keys then that primary key will be stored in an embedded object
					if (metaData.hasMultipleIdFields()) {
						primaryKey = readNestedEntity(context, reader, field.getType());
					}//if
					else {
						primaryKey = readField(context, reader, metaData, entity, subMetaData.getIdField());
					}//else

					JPAEntity sub = (JPAEntity) subMetaData.getNewEntity();
					sub._makeReference(primaryKey);
					yield sub;
				}//else
			}
		};
	}//readField

	private Object readNestedEntity(ReadContext context, TagReader reader, Class<?> objectClass) throws IOException
	{
		BaseMarshallerDelegate<?> delegate = ((SerializationContextImpl) context.getSerializationContext()).getMarshallerDelegate(objectClass);

		int length = reader.readUInt32();
		int oldLimit = reader.pushLimit(length);

		Object nestedObj = readMessage(delegate, context);

		reader.checkLastTagWas(0);
		reader.popLimit(oldLimit);

		return nestedObj;
	}

	@Override
	public void write(WriteContext context, T entity) throws IOException
	{
		try {
			writeEntity(context, context.getWriter(), (JPAEntity) entity);
		}//try
		catch (Throwable ex) {
			throw new IOException("Error writing Entity", ex);
		}//catch
	}//write

	private void writeNestedEntity(WriteContext context, TagWriter writer, int fieldNr, JPAEntity entity) throws Throwable
	{
		ByteArrayOutputStreamEx out = new ByteArrayOutputStreamEx();
		TagWriterImpl nestedWriter = TagWriterImpl.newNestedInstance(context, out);
		writeEntity(context, nestedWriter, entity);
		writer.writeBytes(fieldNr, out.getByteBuffer());
	}//writeNestedEntity

	private void writeField(WriteContext context, TagWriter writer, Class<?> typeClass, FieldType fieldType, int fieldNr, Object value) throws Throwable
	{
		switch (fieldType) {
			case TYPE_BOOLEAN -> writer.writeBool(fieldNr, (Boolean) value);
			case TYPE_INTEGER -> writer.writeInt32(fieldNr, (Integer) value);
			case TYPE_LONGLONG -> writer.writeInt64(fieldNr, (Long) value);
			case TYPE_DOUBLEDOUBLE -> writer.writeDouble(fieldNr, (Double) value);
			case TYPE_BOOL -> writer.writeBool(fieldNr, (boolean) value);
			case TYPE_INT -> writer.writeInt32(fieldNr, (int) value);
			case TYPE_LONG -> writer.writeInt64(fieldNr, (long) value);
			case TYPE_DOUBLE -> writer.writeDouble(fieldNr, (double) value);
			case TYPE_STRING -> writer.writeString(fieldNr, (String) value);
			case TYPE_ENUM -> writer.writeString(fieldNr, ((Enum) value).name());
			case TYPE_ORDINAL_ENUM -> writer.writeInt32(fieldNr, ((Enum) value).ordinal());
			case TYPE_TIMESTAMP -> writer.writeFixed64(fieldNr, ((Timestamp) value).getTime());
			case TYPE_BYTES -> writer.writeBytes(fieldNr, ((byte[]) value));
			case TYPE_LOCALTIME ->
					writer.writeFixed64(fieldNr, Timestamp.from(((LocalDateTime) value).toInstant(ZoneOffset.UTC)).getTime());
			case TYPE_OBJECT -> {
				ByteArrayOutputStream recvOut = new ByteArrayOutputStream();
				ObjectOutputStream stream = new ObjectOutputStream(recvOut);
				stream.writeObject(value);
				stream.flush();
				writer.writeBytes(fieldNr, recvOut.toByteArray());
			}
			case TYPE_CUSTOMTYPE -> {
				@SuppressWarnings("java:S3740")//Can't use generics here
				BaseMarshallerDelegate delegate = ((SerializationContextImpl) context.getSerializationContext()).getMarshallerDelegate(typeClass);
				writeNestedMessage(delegate, context, fieldNr, value);
			}
			case TYPE_ENTITY -> {
				EntityMetaData<?> metaData = ((JPAEntity) value)._getMetaData();

				//If it is not an embeddable entity then we only store the primary key
				if (metaData.getEntityType() == EntityType.ENTITY_EMBEDDABLE) {
					writeNestedEntity(context, writer, fieldNr, (JPAEntity) value);
				}//if
				else {
					//If we have multiple keys then that primary key will be stored in an embedded object
					if (metaData.hasMultipleIdFields()) {
						writeNestedEntity(context, writer, fieldNr, (JPAEntity) ((JPAEntity) value)._getPrimaryKey());
					}//if
					else {
						EntityField vKeyField = metaData.getIdField();
						writeField(context, writer, vKeyField.getType(), vKeyField.getFieldType(), fieldNr, ((JPAEntity) value)._getPrimaryKey());
					}//else
				}//else
			}
		}//switch
	}//writeField

	private void writeEntity(WriteContext context, TagWriter writer, JPAEntity entity) throws Throwable
	{
		for (EntityField field : entity._getMetaData().getEntityFields()) {
			Object value = field.invokeGetter(entity);
			if (value != null) {
				writeField(context, writer, field.getType(), field.getFieldType(), field.getFieldNr(), value);
			}//if
		}//for
	}//writeEntity
}
