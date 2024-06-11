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

package io.jpalite.impl.db;

import io.jpalite.DatabasePool;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement
{
	private static final String NULL_STR = "(null)";
	private static final String ASCII_STREAM = "(ascii stream)";
	private static final String UNICODE_STREAM = "(unicode stream)";
	private static final String BINARY_STREAM = "(binary stream)";
	private static final String CHAR_STREAM = "(char stream)";
	private static final String BLOB = "(blob)";
	private static final String CLOB = "(clob)";
	private static final String ARRAY = "(array)";
	private static final String NCLOB = "(nclob)";
	private static final String NCHAR_STREAM = "(nchar stream)";
	private static final String ROWID = "(rowid)";

	private final PreparedStatement realPreparedStatement;
	protected String queryStr;
	protected Map<Integer, Object> params = new TreeMap<>();

	public PreparedStatementWrapper(DatabasePool pool, String connectName, PreparedStatement preparedStatement, String sql, ConnectionWrapper wrapper)
	{
		super(pool, connectName, preparedStatement, wrapper);
		queryStr = sql;
		connection.setLastQuery(sql);
		realPreparedStatement = preparedStatement;
	}

	private String buildParamList()
	{
		StringBuilder paramsStr = new StringBuilder();
		if (params.isEmpty()) {
			paramsStr.append(",");
		}//if
		else {
			for (Iterator<Integer> iterator = params.keySet().iterator(); iterator.hasNext(); ) {
				Integer key = iterator.next();
				Object value = params.get(key);
				if (value == null) {
					value = NULL_STR;
				}//if

				paramsStr.append(",:").append(key).append("=").append(value);
			}
		}//else
		return paramsStr.substring(1);
	}//buildParamList

	@Override
	protected void logError(String pmethod, String queryStr, Throwable exception)
	{
		if (connection.isEnableLogging()) {
			super.logError(pmethod, queryStr + " - (" + buildParamList() + ")", exception);
		}//if
	}//logError

	@Override
	protected void logExecution(String method, String queryStr, long executeTime, boolean update)
	{
		if (connection.isEnableLogging() || connection.getAuditWriter() != null) {
			StringBuilder paramsStr = new StringBuilder();
			if (params.isEmpty()) {
				paramsStr.append(",");
			}//if
			else {
				for (Iterator<Integer> iterator = params.keySet().iterator(); iterator.hasNext(); ) {
					Integer key = iterator.next();
					Object value = params.get(key);
					if (value == null) {
						value = NULL_STR;
					}//if

					paramsStr.append(",:").append(key).append("=").append(value);
				}
			}//else

			super.logExecution(method, queryStr + " - (" + buildParamList() + ")", executeTime, update);
		}//if
	}

	@Override
	public ResultSet executeQuery() throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			ResultSet resultSet = realPreparedStatement.executeQuery();
			logExecution(EXECUTE_QUERY_METHOD, queryStr, System.currentTimeMillis() - start, false);
			return resultSet;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_QUERY_METHOD, queryStr, ex);
			throw ex;
		}//catch
	}

	@Override
	public int executeUpdate() throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			int result = 0;
			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				result = realPreparedStatement.executeUpdate();
			}//if
			logExecution(EXECUTE_UPDATE_METHOD, queryStr, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_UPDATE_METHOD, queryStr, ex);
			throw ex;
		}//catch
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException
	{
		realPreparedStatement.setNull(parameterIndex, sqlType);
		params.put(parameterIndex, NULL_STR);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException
	{
		realPreparedStatement.setBoolean(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException
	{
		realPreparedStatement.setByte(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException
	{
		realPreparedStatement.setShort(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException
	{
		realPreparedStatement.setInt(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException
	{
		realPreparedStatement.setLong(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException
	{
		realPreparedStatement.setFloat(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException
	{
		realPreparedStatement.setDouble(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
	{
		realPreparedStatement.setBigDecimal(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException
	{
		realPreparedStatement.setString(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException
	{
		realPreparedStatement.setBytes(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException
	{
		realPreparedStatement.setDate(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException
	{
		realPreparedStatement.setTime(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
	{
		realPreparedStatement.setTimestamp(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		realPreparedStatement.setAsciiStream(parameterIndex, x, length);
		params.put(parameterIndex, ASCII_STREAM);
	}

	@Override
	@SuppressWarnings("java:S1874")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		realPreparedStatement.setUnicodeStream(parameterIndex, x, length);
		params.put(parameterIndex, UNICODE_STREAM);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		realPreparedStatement.setBinaryStream(parameterIndex, x, length);
		params.put(parameterIndex, BINARY_STREAM);
	}

	@Override
	public void clearParameters() throws SQLException
	{
		realPreparedStatement.clearParameters();
		params.clear();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
	{
		realPreparedStatement.setObject(parameterIndex, x, targetSqlType);
		params.put(parameterIndex, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException
	{
		realPreparedStatement.setObject(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public boolean execute() throws SQLException
	{
		try {
			long vStart = System.currentTimeMillis();
			boolean vResult = false;
			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				vResult = realPreparedStatement.execute();
			}//if
			logExecution(EXECUTE_METHOD, queryStr, System.currentTimeMillis() - vStart, true);
			return vResult;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_METHOD, queryStr, ex);
			throw ex;
		}//catch
	}

	@Override
	public void addBatch() throws SQLException
	{
		realPreparedStatement.addBatch();
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
	{
		realPreparedStatement.setCharacterStream(parameterIndex, reader, length);
		params.put(parameterIndex, CHAR_STREAM);
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException
	{
		realPreparedStatement.setRef(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException
	{
		realPreparedStatement.setBlob(parameterIndex, x);
		params.put(parameterIndex, BLOB);
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException
	{
		realPreparedStatement.setClob(parameterIndex, x);
		params.put(parameterIndex, CLOB);
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException
	{
		realPreparedStatement.setArray(parameterIndex, x);
		params.put(parameterIndex, ARRAY);
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException
	{
		return realPreparedStatement.getMetaData();
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
	{
		realPreparedStatement.setDate(parameterIndex, x, cal);
		params.put(parameterIndex, x);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
	{
		realPreparedStatement.setTime(parameterIndex, x, cal);
		params.put(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
	{
		realPreparedStatement.setTimestamp(parameterIndex, x, cal);
		params.put(parameterIndex, x);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException
	{
		realPreparedStatement.setNull(parameterIndex, sqlType, typeName);
		params.put(parameterIndex, NULL_STR);
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException
	{
		realPreparedStatement.setURL(parameterIndex, x);
		params.put(parameterIndex, x);
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException
	{
		return realPreparedStatement.getParameterMetaData();
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException
	{
		realPreparedStatement.setRowId(parameterIndex, x);
		params.put(parameterIndex, ROWID);
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException
	{
		realPreparedStatement.setNString(parameterIndex, value);
		params.put(parameterIndex, value);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
	{
		realPreparedStatement.setNCharacterStream(parameterIndex, value, length);
		params.put(parameterIndex, NCHAR_STREAM);
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException
	{
		realPreparedStatement.setNClob(parameterIndex, value);
		params.put(parameterIndex, NCLOB);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		realPreparedStatement.setClob(parameterIndex, reader, length);
		params.put(parameterIndex, CLOB);
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
	{
		realPreparedStatement.setBlob(parameterIndex, inputStream, length);
		params.put(parameterIndex, BLOB);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		realPreparedStatement.setNClob(parameterIndex, reader, length);
		params.put(parameterIndex, NCLOB);
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
	{
		realPreparedStatement.setSQLXML(parameterIndex, xmlObject);
		params.put(parameterIndex, xmlObject.getString());
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException
	{
		realPreparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		params.put(parameterIndex, x.toString());
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		realPreparedStatement.setAsciiStream(parameterIndex, x, length);
		params.put(parameterIndex, ASCII_STREAM);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		realPreparedStatement.setBinaryStream(parameterIndex, x, length);
		params.put(parameterIndex, BINARY_STREAM);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
	{
		realPreparedStatement.setCharacterStream(parameterIndex, reader, length);
		params.put(parameterIndex, CHAR_STREAM);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
	{
		realPreparedStatement.setAsciiStream(parameterIndex, x);
		params.put(parameterIndex, ASCII_STREAM);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
	{
		realPreparedStatement.setBinaryStream(parameterIndex, x);
		params.put(parameterIndex, BINARY_STREAM);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
	{
		realPreparedStatement.setCharacterStream(parameterIndex, reader);
		params.put(parameterIndex, CHAR_STREAM);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
	{
		realPreparedStatement.setNCharacterStream(parameterIndex, value);
		params.put(parameterIndex, NCHAR_STREAM);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException
	{
		realPreparedStatement.setClob(parameterIndex, reader);
		params.put(parameterIndex, CLOB);
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
	{
		realPreparedStatement.setBlob(parameterIndex, inputStream);
		params.put(parameterIndex, BLOB);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException
	{
		realPreparedStatement.setNClob(parameterIndex, reader);
		params.put(parameterIndex, NCLOB);
	}
}
