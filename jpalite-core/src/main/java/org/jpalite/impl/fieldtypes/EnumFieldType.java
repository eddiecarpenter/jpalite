package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jpalite.FieldConvertType;
import org.jpalite.impl.EntityFieldImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Special converter type of enums. It is used internally by {@link EntityFieldImpl}
 * Note that the type must not have a @Converter annotation
 */
public class EnumFieldType implements FieldConvertType<Enum<?>, String>
{
	private final Class<Enum<?>> enumType;

	public EnumFieldType(Class<Enum<?>> enumType)
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
		jsonGenerator.writeString(value.name());
	}

	@Override
	public Enum<?> fromJson(JsonNode enumName)
	{
		for (Enum<?> enumValue : enumType.getEnumConstants()) {
			if (enumValue.name().equals(enumName.textValue())) {
				return enumValue;
			}//if
		}//for

		return null;
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
		String val = resultSet.getString(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public String convertToDatabaseColumn(Enum<?> attribute)
	{
		return (attribute).name();
	}

	@Override
	public Enum<?> convertToEntityAttribute(String dbData)
	{
		return fromJson(TextNode.valueOf(dbData));
	}
}
