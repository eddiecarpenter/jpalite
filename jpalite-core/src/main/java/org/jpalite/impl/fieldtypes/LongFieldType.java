package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.jpalite.FieldConvertType;
import jakarta.persistence.Converter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

@Converter(autoApply = true)
public class LongFieldType implements FieldConvertType<Long, Long>
{
	@Override
	public Class<Long> getAttributeType()
	{
		return long.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Long value) throws IOException
	{
		jsonGenerator.writeNumber(value);
	}

	@Override
	public Long fromJson(JsonNode json)
	{
		return json.longValue();
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		if (value == null) {
			value = 0L;
		}
		out.writeLong((Long) value);
	}

	@Override
	public Long readField(DataInputStream in) throws IOException
	{
		return in.readLong();
	}

	@Override
	public Long convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		return convertToEntityAttribute(resultSet.getLong(column));
	}

	@Override
	public Long convertToDatabaseColumn(Long attribute)
	{
		return attribute;
	}

	@Override
	public Long convertToEntityAttribute(Long dbData)
	{
		return dbData;
	}
}
