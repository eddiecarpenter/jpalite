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
 * Represents a field type that handles boolean values.
 * <p>
 * This class implements the `FieldConvertType` interface and provides methods for converting boolean values
 * to JSON representation, reading/writing boolean values from/to `DataInputStream` and `DataOutputStream`,
 * and converting boolean values from a database column to an entity attribute and vice versa.
 * <p>
 * The `BooleanFieldType` class is annotated with `@Converter(autoApply = true)` to indicate that it should
 * be automatically applied as a converter for entity attributes of type Boolean.
 */
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
