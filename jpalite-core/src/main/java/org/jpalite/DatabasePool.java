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

import jakarta.annotation.Nonnull;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabasePool
{
	/**
	 * Allocated a new connection. It is the caller's responsibility to close the connection. This call is for internal
	 * purposes only and should not be used.
	 *
	 * @return A new connection
	 * @throws SQLException
	 */
	Connection getConnection() throws SQLException;

	/**
	 * Create a new persistence context and allocate a connection to it. The result is thread local and only one
	 * connection manager will be created per thread.
	 * <p>
	 * If the properties in the persistence contains the {@link PersistenceContext#PERSISTENCE_JTA_MANAGED} property with a value of TRUE a
	 * new PersistenceContext will be created
	 *
	 * @param persistenceUnit The persistence unit for the context
	 * @return An instance of {@link PersistenceContext}
	 * @throws SQLException
	 */
	PersistenceContext getPersistenceContext(@Nonnull JPALitePersistenceUnit persistenceUnit) throws SQLException;

	/**
	 * Instruct the database pool to close all connections own by the thread calling the method
	 */
	void cleanup();

	/**
	 * The pool name
	 *
	 * @return
	 */
	String getPoolName();

	/**
	 * Return the version of the database the pool is connected to
	 *
	 * @return the database version
	 */
	String getDbVersion();

	/**
	 * Return product name of the database the pool is connected to
	 *
	 * @return the database name
	 */
	String getDbProductName();
}
