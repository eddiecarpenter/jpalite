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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Converter(autoApply = true)
public class TimestampFieldType implements FieldConvertType<Timestamp, Timestamp>
{
	@Override
	public Class<Timestamp> getAttributeType()
	{
		return Timestamp.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, Timestamp value) throws IOException
	{
		jsonGenerator.writeString(DateTimeFormatter.ISO_INSTANT.format(value.toInstant().truncatedTo(ChronoUnit.NANOS)));
	}

	@Override
	public Timestamp fromJson(JsonNode json)
	{
		return Timestamp.from(Instant.parse(json.textValue()));
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		if (value == null) {
			out.writeLong(0L);
		}
		else {
			out.writeLong(((Timestamp) value).getTime());
		}
	}

	@Override
	public Timestamp readField(DataInputStream in) throws IOException
	{
		return new Timestamp(in.readLong());
	}

	@Override
	public Timestamp convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		Timestamp val = resultSet.getTimestamp(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public Timestamp convertToDatabaseColumn(Timestamp attribute)
	{
		return attribute;
	}

	@Override
	public Timestamp convertToEntityAttribute(Timestamp dbData)
	{
		return dbData;
	}
}
