/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jpalite;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for defining entities field types
 * <p>
 * Note that the X and Y types may be the same Java type.
 *
 * @param <X> the type of the entity attribute
 * @param <Y> the type of the database column
 */
public interface FieldConvertType<X, Y> extends AttributeConverter<X, Y>
{
    /**
     * Returns the type of the entity attribute.
     *
     * @return the type of the entity attribute
     */
    Class<?> getAttributeType();

    /**
     * Converts an object of type X to JSON representation using the provided {@link JsonGenerator}.
     *
     * @param jsonGenerator the JSON generator used for writing JSON data
     * @param value         the object to be converted to JSON
     * @throws IOException if an I/O error occurs during the conversion
     */
    void toJson(JsonGenerator jsonGenerator, X value) throws IOException;

    /**
     * Converts a {@link JsonNode} to an instance of type X using the specified attribute converter.
     *
     * @param json the JSON node to be converted
     * @return an instance of type X converted from the JSON node
     */
    X fromJson(JsonNode json);

    /**
     * Writes a field value to a DataOutputStream.
     *
     * @param value the value of the field to write
     * @param out   the DataOutputStream to write to
     * @throws IOException if an I/O error occurs while writing the field
     */
    void writeField(Object value, DataOutputStream out) throws IOException;

    /**
     * Reads a field value from a DataInputStream.
     *
     * @param in the DataInputStream to read from
     * @return the value of the field read from the DataInputStream
     * @throws IOException if an I/O error occurs while reading the field
     */
    X readField(DataInputStream in) throws IOException;


    /**
     * Converts a value from a database column to an entity attribute of type X using an attribute converter.
     *
     * @param resultSet the ResultSet object representing the database result set
     * @param column    the column index of the attribute in the ResultSet object
     * @return the converted entity attribute value of type X, or null if the column value is null
     * @throws SQLException if an error occurs while accessing the ResultSet object
     */
    @SuppressWarnings("unchecked")
    default X convertToEntityAttribute(ResultSet resultSet, int column) throws SQLException
    {
        return (resultSet.wasNull() ? null : convertToEntityAttribute((Y) resultSet.getObject(column)));
    }
}
