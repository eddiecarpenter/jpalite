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

package org.jpalite.impl.db;

import org.jpalite.DatabasePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

@SuppressWarnings({"resource", "SqlSourceToSinkFlow"})
public class StatementWrapper implements Statement
{
	protected static final String EXECUTE_UPDATE_METHOD = "executeUpdate";
	protected static final String EXECUTE_QUERY_METHOD = "executeQuery";
	protected static final String EXECUTE_METHOD = "execute";
	private static final Logger LOG = LoggerFactory.getLogger(StatementWrapper.class);
	protected ConnectionWrapper connection;
	private final Statement realStatement;
	private final DatabasePool databasePool;

	public StatementWrapper(DatabasePool pool, String cursorName, Statement realStatement, ConnectionWrapper wrapper)
	{
		this.realStatement = realStatement;
		connection = wrapper;
		databasePool = pool;
		try {
			realStatement.setCursorName(cursorName);
		}//try
		catch (SQLException ex) {
			LOG.warn("Failed to set the cursor name to {}", cursorName, ex);
		}//catch
	}

	protected void logError(String method, String queryStr, Throwable exception)
	{
		if (connection.isEnableLogging()) {
			LOG.info("{} > {}: Query execution failed {}. Query {}", databasePool.getPoolName(), method, exception.getMessage(), queryStr);
		}//if
	}//logError

	protected void logExecution(String method, String queryStr, long executeTime, boolean update)
	{
		String vReadonlyStr = "";

		if (connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
			vReadonlyStr = " (Readonly) ";
		}//if

		if (update && connection.getAuditWriter() != null) {
			connection.getAuditWriter().printf("%s > %s%s: %s%n", databasePool.getPoolName(), method, vReadonlyStr, queryStr);
		}//if

		if (executeTime > connection.getSlowQueryTimeout()) {
			LOG.warn("{} > {}{}: Long running query detected [{} ms]: {}", databasePool.getPoolName(), method, vReadonlyStr, executeTime, queryStr);
		}//if
		else {
			if (connection.isEnableLogging()) {
				LOG.info("{} > {}{}: Query completed [{} ms]: {}", databasePool.getPoolName(), method, vReadonlyStr, executeTime, queryStr);
			}//else
		}//else
	}//logExecution

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			connection.setLastQuery(sql);
			ResultSet result = realStatement.executeQuery(sql);
			logExecution(EXECUTE_QUERY_METHOD, sql, System.currentTimeMillis() - start, false);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_QUERY_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public int executeUpdate(String sql) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			int result = 0;
			connection.setLastQuery(sql);
			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				result = realStatement.executeUpdate(sql);
			}//if
			logExecution(EXECUTE_UPDATE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_UPDATE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public void close() throws SQLException
	{
		realStatement.close();
	}

	@Override
	public int getMaxFieldSize() throws SQLException
	{
		return realStatement.getMaxFieldSize();
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException
	{
		realStatement.setMaxFieldSize(max);
	}

	@Override
	public int getMaxRows() throws SQLException
	{
		return realStatement.getMaxRows();
	}

	@Override
	public void setMaxRows(int max) throws SQLException
	{
		realStatement.setMaxRows(max);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		realStatement.setEscapeProcessing(enable);
	}

	@Override
	public int getQueryTimeout() throws SQLException
	{
		return realStatement.getQueryTimeout();
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException
	{
		realStatement.setQueryTimeout(seconds);
	}

	@Override
	public void cancel() throws SQLException
	{
		realStatement.cancel();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return realStatement.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		realStatement.clearWarnings();
	}

	@Override
	public void setCursorName(String name) throws SQLException
	{
		realStatement.setCursorName(name);
	}

	@Override
	public boolean execute(String sql) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			boolean result = false;
			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.execute(sql);
			}//if

			logExecution(EXECUTE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		return realStatement.getResultSet();
	}

	@Override
	public int getUpdateCount() throws SQLException
	{
		return realStatement.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException
	{
		return realStatement.getMoreResults();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException
	{
		realStatement.setFetchDirection(direction);
	}

	@Override
	public int getFetchDirection() throws SQLException
	{
		return realStatement.getFetchDirection();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException
	{
		realStatement.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException
	{
		return realStatement.getFetchSize();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException
	{
		return realStatement.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException
	{
		return realStatement.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException
	{
		realStatement.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException
	{
		realStatement.clearBatch();
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		return realStatement.executeBatch();
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return realStatement.getConnection();
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException
	{
		return realStatement.getMoreResults(current);
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException
	{
		return realStatement.getGeneratedKeys();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			int result = 0;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.executeUpdate(sql, autoGeneratedKeys);
			}//if

			logExecution(EXECUTE_UPDATE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_UPDATE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			int result = 0;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.executeUpdate(sql, columnIndexes);
			}//if

			logExecution(EXECUTE_UPDATE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_UPDATE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			int result = 0;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.executeUpdate(sql, columnNames);
			}//if

			logExecution(EXECUTE_UPDATE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_UPDATE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			boolean result = false;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.execute(sql, autoGeneratedKeys);
			}//if

			logExecution(EXECUTE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			boolean result = false;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.execute(sql, columnIndexes);
			}//if

			logExecution(EXECUTE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		try {
			long start = System.currentTimeMillis();
			boolean result = false;

			if (!connection.getPersistenceContext().unwrap(PersistenceContextImpl.class).isReadonly()) {
				connection.setLastQuery(sql);
				result = realStatement.execute(sql, columnNames);
			}//if

			logExecution(EXECUTE_METHOD, sql, System.currentTimeMillis() - start, true);
			return result;
		}//try
		catch (SQLException ex) {
			logError(EXECUTE_METHOD, sql, ex);
			throw ex;
		}//catch
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		return realStatement.getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return realStatement.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException
	{
		realStatement.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException
	{
		return realStatement.isPoolable();
	}

	@Override
	public void closeOnCompletion() throws SQLException
	{
		realStatement.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException
	{
		return realStatement.isCloseOnCompletion();
	}

	@Override
	public long getLargeUpdateCount() throws SQLException
	{
		return realStatement.getLargeUpdateCount();
	}

	@Override
	public void setLargeMaxRows(long max) throws SQLException
	{
		realStatement.setLargeMaxRows(max);
	}

	@Override
	public long getLargeMaxRows() throws SQLException
	{
		return realStatement.getLargeMaxRows();
	}

	@Override
	public long[] executeLargeBatch() throws SQLException
	{
		return realStatement.executeLargeBatch();
	}

	@Override
	public long executeLargeUpdate(String sql) throws SQLException
	{
		return realStatement.executeLargeUpdate(sql);
	}

	@Override
	public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		return realStatement.executeLargeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		return realStatement.executeLargeUpdate(sql, columnIndexes);
	}

	@Override
	public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException
	{
		return realStatement.executeLargeUpdate(sql, columnNames);
	}

	@Override
	public String enquoteLiteral(String val) throws SQLException
	{
		return realStatement.enquoteLiteral(val);
	}

	@Override
	public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException
	{
		return realStatement.enquoteIdentifier(identifier, alwaysQuote);
	}

	@Override
	public boolean isSimpleIdentifier(String identifier) throws SQLException
	{
		return realStatement.isSimpleIdentifier(identifier);
	}

	@Override
	public String enquoteNCharLiteral(String val) throws SQLException
	{
		return realStatement.enquoteNCharLiteral(val);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return realStatement.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return realStatement.isWrapperFor(iface);
	}
}
