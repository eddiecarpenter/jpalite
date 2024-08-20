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
 * The IntegerFieldType class is an implementation of the FieldConvertType interface
 * for handling fields of type Integer.
 * <p>
 * This class is annotated with @Converter(autoApply = true), indicating that it is an
 * auto-apply attribute converter, meaning it will be applied to all entity attributes
 * of type Integer unless overridden by a different converter.
 * <p>
 * This class provides methods for converting Integer values to and from JSON format,
 * as well as writing and reading them from DataInputStream and DataOutputStream objects.
 * It also provides methods for converting Integer values from a database column to
 * an entity attribute of type Integer, and vice versa.
 **/
@Converter(autoApply = true)
public class IntegerFieldType implements FieldConvertType<Integer, Integer>
{
    @Override
    public Class<Integer> getAttributeType()
    {
        return Integer.class;
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
        int val = resultSet.getInt(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
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
