package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.jpalite.CachingException;
import org.jpalite.FieldConvertType;
import org.jpalite.impl.EntityFieldImpl;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Special converter type of Object type. It is used internally by {@link EntityFieldImpl}
 * Note that the type must not have a @Converter annotation
 */
public class ObjectFieldType implements FieldConvertType<Object, byte[]>
{
	@Override
	public Class<Object> getAttributeType()
	{
		return Object.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Object value) throws IOException
	{
		jsonGenerator.writeBinary(convertToDatabaseColumn(value));
	}

	@Override
	public Object fromJson(JsonNode json)
	{
		try {
			return convertToEntityAttribute(json.binaryValue());
		}
		catch (IOException ex) {
			throw new CachingException("Error reading byte[]", ex);
		}
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		byte[] bytes = convertToDatabaseColumn(value);
		out.writeShort(bytes.length);
		out.write(bytes);
	}

	@Override
	public Object readField(DataInputStream in) throws IOException
	{
		int size = in.readShort();
		byte[] bytes = in.readNBytes(size);

		return convertToEntityAttribute(bytes);
	}

	@Override
	public Object convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		byte[] val = resultSet.getBytes(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public byte[] convertToDatabaseColumn(Object attribute)
	{
		try {
			ByteArrayOutputStream recvOut = new ByteArrayOutputStream();
			ObjectOutputStream stream = new ObjectOutputStream(recvOut);
			stream.writeObject(attribute);
			stream.flush();
			return recvOut.toByteArray();
		}
		catch (IOException ex) {
			throw new CachingException("Error writing Object to stream", ex);
		}
	}

	@Override
	public Object convertToEntityAttribute(byte[] dbData)
	{
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(dbData);
			ObjectInputStream stream = new ObjectInputStream(in);
			return stream.readObject();
		}
		catch (IOException | ClassNotFoundException ex) {
			throw new CachingException("Error reading Object from stream", ex);
		}
	}
}
