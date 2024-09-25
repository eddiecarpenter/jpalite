package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Converter;
import org.jpalite.FieldConvertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The LocalDateTimeFieldType class is a concrete implementation of the FieldConvertType interface.
 * It is used for defining entities field types that represent LocalDateTime values.
 * <p>
 * This class provides methods to convert LocalDateTime values to and from different representations,
 * such as JSON, DataOutputStream, ResultSet, and Timestamp.
 * <p>
 * The class overrides methods from the FieldConvertType interface to implement the conversion logic.
 * <p>
 * This class is marked with the @Converter annotation with autoApply=true, indicating that it should be
 * automatically applied for all LocalDateTime fields in entities.
 */
@Converter(autoApply = true)
public class DateFieldType implements FieldConvertType<Date, Date>
{
    @Override
    public Class<Date> getAttributeType()
    {
        return Date.class;
    }

    @Override
    public void toJson(JsonGenerator jsonGenerator, Date value) throws IOException
    {
        jsonGenerator.writeNumber(value.getTime());
    }

    @Override
    public Date fromJson(JsonNode json)
    {
        return new Date(json.asLong());
    }

    @Override
    public void writeField(Object value, DataOutputStream out) throws IOException
    {
        if (value == null) {
            out.writeLong(0L);
        }
        else {
            out.writeLong(((Date) value).getTime());
        }
    }

    @Override
    public Date readField(DataInputStream in) throws IOException
    {
        return new Date(in.readLong());
    }

    @Override
    public Date convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
    {
        Date val = resultSet.getDate(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
    }

    @Override
    public Date convertToDatabaseColumn(Date attribute)
    {
        return attribute;
    }

    @Override
    public Date convertToEntityAttribute(Date dbData)
    {
        return dbData;
    }
}
