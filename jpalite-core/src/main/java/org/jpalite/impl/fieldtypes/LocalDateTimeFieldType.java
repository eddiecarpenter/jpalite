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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Converter(autoApply = true)
public class LocalDateTimeFieldType implements FieldConvertType<LocalDateTime, Timestamp>
{
	@Override
	public Class<LocalDateTime> getAttributeType()
	{
		return LocalDateTime.class;
	}

	@Override
	public void toJson(JsonGenerator jsonGenerator, LocalDateTime value) throws IOException
	{
		jsonGenerator.writeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.NANOS)));
	}

	@Override
	public LocalDateTime fromJson(JsonNode json)
	{
		return LocalDateTime.from(Instant.parse(json.textValue()));
	}

	@Override
	public void writeField(Object value, DataOutputStream out) throws IOException
	{
		if (value == null) {
			out.writeLong(0L);
		}
		else {
			out.writeLong(((LocalDateTime) value).toInstant(ZoneOffset.UTC).getEpochSecond());
		}
	}

	@Override
	public LocalDateTime readField(DataInputStream in) throws IOException
	{
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(in.readLong()), ZoneId.of("UTC"));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
	{
		Timestamp val = resultSet.getTimestamp(column);
		return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
	}

	@Override
	public Timestamp convertToDatabaseColumn(LocalDateTime attribute)
	{
		return Timestamp.from(attribute.toInstant(ZoneOffset.UTC));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(Timestamp dbData)
	{
		return dbData.toLocalDateTime();
	}
}
