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

import org.jpalite.DataSourceProvider;
import org.jpalite.DatabasePool;
import org.jpalite.JPALitePersistenceUnit;
import org.jpalite.PersistenceContext;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static org.jpalite.PersistenceContext.PERSISTENCE_JTA_MANAGED;

/**
 * The DatabasePoolImpl class is part of the JPA implementation
 */
public class DatabasePoolImpl implements DatabasePool
{
	private static final Logger LOG = LoggerFactory.getLogger(DatabasePoolImpl.class);

	private final ThreadLocal<Map<String, PersistenceContext>> connections = new ThreadLocal<>();
	private final String poolName;
	private final DataSource dataSource;
	/**
	 * The Database version
	 */
	private final String dbVersion;
	/**
	 * The name of the database
	 */
	private final String dbProductName;


	public DatabasePoolImpl(String dataSourceName) throws SQLException
	{
		poolName = dataSourceName;

		DataSource workingDataSource = null;
		ServiceLoader<DataSourceProvider> vLoader = ServiceLoader.load(DataSourceProvider.class);
		for (DataSourceProvider vDataSourceProvider : vLoader) {
			workingDataSource = vDataSourceProvider.getDataSource(dataSourceName);
			if (workingDataSource != null) {
				break;
			}//if
		}//for
		if (workingDataSource == null) {
			throw new IllegalArgumentException("The data source name '" + dataSourceName + "' is not defined");
		}//if

		dataSource = workingDataSource;
		try (Connection connection = dataSource.getConnection()) {
			dbProductName = connection.getMetaData().getDatabaseProductName();
			dbVersion = connection.getMetaData().getDatabaseProductVersion();
		}//try
	}//DatabasePoolImpl

	@Override
	public String toString()
	{
		return "DatabasePool[" + poolName + "]";
	}

	@Override
	public String getPoolName()
	{
		return poolName;
	}//getPoolname

	@Override
	public PersistenceContext getPersistenceContext(@Nonnull JPALitePersistenceUnit persistenceUnit) throws SQLException
	{
		if (persistenceUnit.getProperties().containsKey(PERSISTENCE_JTA_MANAGED) && Boolean.TRUE.equals(persistenceUnit.getProperties().get(PERSISTENCE_JTA_MANAGED))) {
			LOG.trace("Creating a container managed Persistence Context for thread {}", Thread.currentThread().getName());
			return new PersistenceContextImpl(this, persistenceUnit);
		}//if

		Map<String, PersistenceContext> connectionList = connections.get();
		PersistenceContext manager;
		if (connectionList == null || connectionList.get(persistenceUnit.getPersistenceUnitName()) == null) {
			LOG.trace("Creating a new Persistence Context for thread {}", Thread.currentThread().getName());
			manager = new PersistenceContextImpl(this, persistenceUnit);
			if (connectionList == null) {
				connectionList = new ConcurrentHashMap<>();
			}//if
			connectionList.put(persistenceUnit.getPersistenceUnitName(), manager);
			connections.set(connectionList);
		}//if
		else {
			manager = connectionList.get(persistenceUnit.getPersistenceUnitName());
			LOG.trace("Resuming Persistence Context created for thread {}", Thread.currentThread().getName());
		}//else

		return manager;
	}//getConnectionManager

	@Override
	public Connection getConnection() throws SQLException
	{
		return dataSource.getConnection();
	}//getConnection

	@Override
	public void cleanup()
	{
		Map<String, PersistenceContext> contextList = connections.get();
		if (contextList != null) {
			LOG.trace("Releasing Persistence Context created for thread {}", Thread.currentThread().getName());
			connections.remove();
			contextList.values().forEach(PersistenceContext::release);
		}//if
	}//cleanup

	@Override
	public String getDbVersion()
	{
		return dbVersion;
	}

	@Override
	public String getDbProductName()
	{
		return dbProductName;
	}
}//DatabasePool
