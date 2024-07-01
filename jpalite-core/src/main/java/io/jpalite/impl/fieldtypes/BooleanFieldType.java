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
public class BooleanFieldType implements FieldConvertType<Boolean, Boolean>
{
	@Override
	public Class<Boolean> getAttributeType()
	{
		return Boolean.class;
	}


	@Override
	public void toJson(JsonGenerator jsonGenerator, Boolean value) throws IOException
	{
		jsonGenerator.writeBoolean(value);
	}

	@Override
	public Boolean fromJson(JsonNode json)
	{
		return json.booleanValue();
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		out.writeBoolean((Boolean) value);
	}

	@Override
	public Boolean readField(DataInputStream in) throws IOException
	{
		return in.readBoolean();
	}

	@Override
	public Boolean convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		boolean val = resultSet.getBoolean(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public Boolean convertToDatabaseColumn(Boolean attribute)
	{
		return attribute;
	}

	@Override
	public Boolean convertToEntityAttribute(Boolean dbData)
	{
		return dbData;
	}
}
