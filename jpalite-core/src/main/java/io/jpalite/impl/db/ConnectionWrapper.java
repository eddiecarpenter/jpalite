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
import io.jpalite.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@SuppressWarnings("SqlSourceToSinkFlow")
public class ConnectionWrapper implements Connection
{
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionWrapper.class);

	private final Connection realConnection;
	private final long slowQueryTimeout;
	private final PersistenceContext persistenceContext;
	private PrintWriter auditWriter;
	private boolean enableLogging;
	private String connectionName;
	private final DatabasePool databasePool;

	public ConnectionWrapper(PersistenceContext persistenceContext, Connection realConnection, long slowQueryTimeout)
	{
		this.realConnection = realConnection;
		this.slowQueryTimeout = slowQueryTimeout;
		this.persistenceContext = persistenceContext;
		databasePool = this.persistenceContext.unwrap(DatabasePool.class);
		enableLogging = false;
		LOG.trace("Opening Connection {}", this.realConnection);
	}

	@Override
	public String toString()
	{
		return "ConnectionWrapper[" + realConnection + "]";
	}

	public PersistenceContext getPersistenceContext()
	{
		return persistenceContext;
	}

	public void setName(String name)
	{
		connectionName = name;
	}

	public void realClose() throws SQLException
	{
		realConnection.close();
	}//realClose

	@Override
	public void close() throws SQLException
	{
		persistenceContext.close();
	}//close

	@Override
	public void commit() throws SQLException
	{
		realConnection.commit();
	}

	@Override
	public void rollback() throws SQLException
	{
		realConnection.rollback();
	}

	public long getSlowQueryTimeout()
	{
		return slowQueryTimeout;
	}

	/**
	 * Retrieve the current audit writer
	 *
	 * @return The audit writer or null
	 */
	public PrintWriter getAuditWriter()
	{
		return auditWriter;
	}

	/**
	 * Set the audit writer to use to record all executed queries in
	 *
	 * @param auditWriter the audit writer to record audit info
	 */
	public void setAuditWriter(PrintWriter auditWriter)
	{
		this.auditWriter = auditWriter;
	}//setAuditWriter

	public boolean isEnableLogging()
	{
		return enableLogging;
	}

	/**
	 * Set the logging state of the connection returning the previous state
	 *
	 * @param enableLogging The new logging state
	 * @return The previous state
	 */
	public boolean setEnableLogging(boolean enableLogging)
	{
		boolean vPrevState = this.enableLogging;
		this.enableLogging = enableLogging;
		return vPrevState;
	}

	public void setLastQuery(String lastQuery)
	{
		persistenceContext.setLastQuery(lastQuery);
	}

	@Override
	public Statement createStatement() throws SQLException
	{
		return new StatementWrapper(databasePool, connectionName, realConnection.createStatement(), this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql), sql, this);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException
	{
		return realConnection.prepareCall(sql);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException
	{
		return realConnection.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		realConnection.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException
	{
		return realConnection.getAutoCommit();
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return realConnection.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException
	{
		return realConnection.getMetaData();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException
	{
		realConnection.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException
	{
		return realConnection.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException
	{
		realConnection.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException
	{
		return realConnection.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException
	{
		realConnection.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException
	{
		return realConnection.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return realConnection.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		realConnection.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return new StatementWrapper(databasePool, connectionName, realConnection.createStatement(resultSetType, resultSetConcurrency), this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency), sql, this);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException
	{
		return realConnection.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException
	{
		realConnection.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException
	{
		realConnection.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException
	{
		return realConnection.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException
	{
		return realConnection.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException
	{
		return realConnection.setSavepoint();
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException
	{
		realConnection.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		realConnection.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return new StatementWrapper(databasePool, connectionName, realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetConcurrency), this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, this);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql, autoGeneratedKeys), sql, this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql, columnIndexes), sql, this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		return new PreparedStatementWrapper(databasePool, connectionName, realConnection.prepareStatement(sql, columnNames), sql, this);
	}

	@Override
	public Clob createClob() throws SQLException
	{
		return realConnection.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException
	{
		return realConnection.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException
	{
		return realConnection.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException
	{
		return realConnection.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		return realConnection.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException
	{
		realConnection.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException
	{
		realConnection.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException
	{
		return realConnection.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException
	{
		return realConnection.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException
	{
		return realConnection.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException
	{
		return realConnection.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(String schema) throws SQLException
	{
		realConnection.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException
	{
		return realConnection.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException
	{
		realConnection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
	{
		realConnection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException
	{
		return realConnection.getNetworkTimeout();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if (iface.isAssignableFrom(this.getClass())) {
			return (T) this;
		}//if

		return realConnection.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return realConnection.isWrapperFor(iface);
	}
}
