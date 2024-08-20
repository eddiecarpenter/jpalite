package org.jpalite.impl.fieldtypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jpalite.FieldConvertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BigDecimalFieldType is a class that implements the FieldConvertType interface for converting BigDecimal field types.
 * It provides methods for converting BigDecimal values to and from JSON, as well as to and from DataInputStream and DataOutputStream.
 * It also provides methods for converting BigDecimal values to and from database columns.
 * <p>
 * Note that BigDecimalFieldType implements the AttributeConverter interface with the types being BigDecimal
 * for both the entity attribute and the database column.
 *
 * @see FieldConvertType
 * @see AttributeConverter
 */
@Converter(autoApply = true)
public class BigDecimalFieldType implements FieldConvertType<BigDecimal, BigDecimal>
{
    /**
     * Empty constructor
     */
    public BigDecimalFieldType()
    {
        super();
    }

    @Override
    public Class<BigDecimal> getAttributeType()
    {
        return BigDecimal.class;
    }

    @Override
    public void toJson(JsonGenerator jsonGenerator, BigDecimal value) throws IOException
    {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("scale");
        jsonGenerator.writeNumber(value.scale());
        jsonGenerator.writeFieldName("value");
        jsonGenerator.writeString(value.toPlainString());
        jsonGenerator.writeEndObject();
    }

    @Override
    public BigDecimal fromJson(JsonNode json)
    {
        int scale = json.get("scale").asInt();
        return new BigDecimal(json.get("value").textValue()).setScale(scale, RoundingMode.HALF_DOWN);
    }

    @Override
    public void writeField(Object value, DataOutputStream out) throws IOException
    {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        out.writeInt(((BigDecimal) value).scale());
        out.writeUTF(((BigDecimal) value).toPlainString());
    }

    @Override
    public BigDecimal readField(DataInputStream in) throws IOException
    {
        int scale = in.readInt();
        return new BigDecimal(in.readUTF()).setScale(scale, RoundingMode.HALF_DOWN);
    }

    @Override
    public BigDecimal convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
    {
        BigDecimal val = resultSet.getBigDecimal(column);
        return (resultSet.wasNull() ? null : convertToEntityAttribute(val));
    }

    @Override
    public BigDecimal convertToDatabaseColumn(BigDecimal attribute)
    {
        return attribute;
    }

    @Override
    public BigDecimal convertToEntityAttribute(BigDecimal dbData)
    {
        return dbData;
    }
}
