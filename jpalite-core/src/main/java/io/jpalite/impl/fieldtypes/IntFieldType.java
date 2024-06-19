package io.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.jpalite.FieldConvertType;
import jakarta.persistence.Converter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

@Converter(autoApply = true)
public class IntFieldType implements FieldConvertType<Integer, Integer>
{
	@Override
	public Class<Integer> getAttributeType()
	{
		return int.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Integer value) throws IOException
	{
		jsonGenerator.writeNumber(value);
	}

	@Override
	public Integer fromJson(JsonNode json)
	{
		return json.intValue();
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		if (value == null) {
			value = 0;
		}
		out.writeInt((Integer) value);
	}

	@Override
	public Integer readField(DataInputStream in) throws IOException
	{
		return in.readInt();
	}

	@Override
	public Integer convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		return convertToEntityAttribute(resultSet.getInt(column));
	}

	@Override
	public Integer convertToDatabaseColumn(Integer attribute)
	{
		return attribute;
	}

	@Override
	public Integer convertToEntityAttribute(Integer dbData)
	{
		return dbData;
	}
}
