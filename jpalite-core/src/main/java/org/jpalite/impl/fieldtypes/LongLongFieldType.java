package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Converter;
import org.jpalite.FieldConvertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class that implements the FieldConvertType interface for converting Long values.
 * It provides methods for converting Long values to and from JSON format, as well as
 * reading and writing Long values to a DataInputStream and DataOutputStream. This class
 * also includes methods for converting Long values from a database column to an entity
 * attribute and vice versa.
 * <p>
 * This class is annotated with the @Converter annotation and is set to autoApply, meaning
 * that it will automatically be applied as a converter for all Long fields in entities.
 * <p>
 * Use this class when you have Long fields in your entities and need to convert them
 * to and from different formats such as JSON or a database column.
 */
@Converter(autoApply = true)
public class LongLongFieldType implements FieldConvertType<Long, Long>
{
    @Override
    public Class<Long> getAttributeType()
    {
        return Long.class;
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
        long val = resultSet.getLong(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
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
