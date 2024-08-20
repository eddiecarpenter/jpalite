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
public class DoubleFieldType implements FieldConvertType<Double, Double>
{
	@Override
	public Class<Double> getAttributeType()
	{
		return double.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Double value) throws IOException
	{
		jsonGenerator.writeNumber(value);
	}

	@Override
	public Double fromJson(JsonNode json)
	{
		return json.doubleValue();
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		if (value == null) {
			value = 0.0;
		}
		out.writeDouble((Double) value);
	}

	@Override
	public Double readField(DataInputStream in) throws IOException
	{
		return in.readDouble();
	}

	@Override
	public Double convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		return convertToEntityAttribute(resultSet.getDouble(column));
	}

	@Override
	public Double convertToDatabaseColumn(Double attribute)
	{
		return attribute;
	}

	@Override
	public Double convertToEntityAttribute(Double dbData)
	{
		return dbData;
	}
}
