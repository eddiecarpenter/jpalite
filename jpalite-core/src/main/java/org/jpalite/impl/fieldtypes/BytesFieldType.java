package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jpalite.CachingException;
import org.jpalite.FieldConvertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts between byte array attributes and database columns.
 * <p>
 * This class implements the {@link FieldConvertType} interface, which defines methods for converting between
 * entity attributes and database columns. It also serves as
 * an {@link AttributeConverter} for byte arrays.
 * <p>
 * The {@link BytesFieldType} class is annotated with {@link Converter} to indicate that it should be automatically
 * applied to entities.
 */
@Converter(autoApply = true)
public class BytesFieldType implements FieldConvertType<byte[], byte[]>
{
    @Override
    public Class<byte[]> getAttributeType()
    {
        return byte[].class;
    }

    @Override
    public void toJson(JsonGenerator jsonGenerator, byte[] value) throws IOException
    {
        jsonGenerator.writeBinary(value);
    }

    @Override
    public byte[] fromJson(JsonNode json)
    {
        try {
            return json.binaryValue();
        }
        catch (IOException ex) {
            throw new CachingException("Error reading byte[]", ex);
        }
    }

    @Override
    public void writeField(Object value, DataOutputStream out) throws IOException
    {
        byte[] bytes = (byte[]) value;
        out.writeInt(bytes.length);
        if (bytes.length > 0) {
            out.write(bytes);
        }
    }

    @Override
    public byte[] readField(DataInputStream in) throws IOException
    {
        byte[] bytes = null;

        int size = in.readInt();
        if (size > 0) {
            bytes = in.readNBytes(size);
        }//if

        return bytes;
    }

    @Override
    public byte[] convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
    {
        byte[] val = resultSet.getBytes(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
    }

    @Override
    public byte[] convertToDatabaseColumn(byte[] attribute)
    {
        return attribute;
    }

    @Override
    public byte[] convertToEntityAttribute(byte[] dbData)
    {
        return dbData;
    }
}
