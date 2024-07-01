package io.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.jpalite.FieldConvertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Special converter type of ordinal enums. It is used internally by {@link io.jpalite.impl.EntityFieldImpl}
 * Note that the type must not have a @Converter annotation
 */
public class OrdinalEnumFieldType implements FieldConvertType<Enum<?>, Integer>
{
	private final Class<Enum<?>> enumType;

	public OrdinalEnumFieldType(Class<Enum<?>> enumType)
	{
		this.enumType = enumType;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<Enum> getAttributeType()
	{
		return Enum.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Enum<?> value) throws IOException
	{
		jsonGenerator.writeNumber(value.ordinal());
	}

	@Override
	public Enum<?> fromJson(JsonNode json)
	{
		int ordinal = json.intValue();
		return enumType.getEnumConstants()[ordinal];
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		out.writeShort(((Enum<?>) value).ordinal());
	}

	@Override
	public Enum<?> readField(DataInputStream in) throws IOException
	{
		int ordinal = in.readShort();
		return enumType.getEnumConstants()[ordinal];
	}

	@Override
	public Enum<?> convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		int val = resultSet.getInt(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public Integer convertToDatabaseColumn(Enum<?> attribute)
	{
		return attribute.ordinal();
	}

	@Override
	public Enum<?> convertToEntityAttribute(Integer ordinal)
	{
		return enumType.getEnumConstants()[ordinal];
	}
}
