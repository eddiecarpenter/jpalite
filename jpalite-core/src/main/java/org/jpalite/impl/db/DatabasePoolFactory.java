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
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The DatabasePoolFactory class is part of the JPALite implementation
 */
public class DatabasePoolFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(DatabasePoolFactory.class);

	private static final Map<String, DatabasePool> POOLS = new HashMap<>();
	private static final ReentrantLock MUTEX = new ReentrantLock();

	private DatabasePoolFactory()
	{
	}

	public static DatabasePool getDatabasePool(String dataSourceName)
	{
		MUTEX.lock();
		try {
			return POOLS.computeIfAbsent(dataSourceName, ds -> {
				try {
					return new DatabasePoolImpl(ds);
				}//try
				catch (SQLException ex) {
					LOG.warn("Error loading Database Pool", ex);
					throw new PersistenceException("Error loading Database Pool");
				}//catch
			});
		}
		finally {
			MUTEX.unlock();
		}
	}//getDatabasePool

	public static void cleanup()
	{
		for (DatabasePool pool : POOLS.values()) {
			pool.cleanup();
		}//for
	}
}//DatabasePoolFactory
