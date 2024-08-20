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
 * A class that implements the {@link FieldConvertType} interface and represents a field type for working with double values.
 * This class provides conversion methods for converting double values to and from various formats, such as JSON, DataOutputStream, and ResultSet.
 * <p>
 * This class is annotated with {@link Converter} annotation with the "autoApply" attribute set to true, indicating that the conversion should be applied automatically.
 *
 * @see FieldConvertType
 * @see Converter
 */
@Converter(autoApply = true)
public class DoubleDoubleFieldType implements FieldConvertType<Double, Double>
{
    @Override
    public Class<Double> getAttributeType()
    {
        return Double.class;
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
        double val = resultSet.getDouble(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
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
