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
public class StringFieldType implements FieldConvertType<String, String>
{
	@Override
	public Class<String> getAttributeType()
	{
		return String.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, String value) throws IOException
	{
		jsonGenerator.writeString(value);
	}

	@Override
	public String fromJson(JsonNode json)
	{
		return json.textValue();
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		out.writeUTF((String) value);
	}

	@Override
	public String readField(DataInputStream in) throws IOException
	{
		return in.readUTF();
	}

	@Override
	public String convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		String val = resultSet.getString(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public String convertToDatabaseColumn(String attribute)
	{
		return attribute;
	}

	@Override
	public String convertToEntityAttribute(String dbData)
	{
		return dbData;
	}
}
