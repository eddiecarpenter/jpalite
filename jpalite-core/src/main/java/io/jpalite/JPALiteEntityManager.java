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

package io.jpalite;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TransactionRequiredException;

import java.io.Closeable;
import java.sql.ResultSet;

/**
 * The JPALite implementation
 */
public interface JPALiteEntityManager extends EntityManager, Closeable
{
	/**
	 * The jpalite.persistence.log.slowQueries hint defines, in milliseconds, the maximum time that query is
	 * expected to run and request the persistence context to log any execution that exceeds that time.
	 * <p>
	 * Note that this will stop the query when the execution limit is reached. If the execution is to be stopped see
	 * {@link #PERSISTENCE_QUERY_TIMEOUT}
	 */
	String PERSISTENCE_QUERY_LOG_SLOWTIME = "jpalite.persistence.log.slowQueries";
	/**
	 * The javax.persistence.query.timeout hint defines, in seconds, how long a query is allowed to run before it gets
	 * cancelled. The JPA stack does’t handle this timeout itself but provides it to the JDBC driver via the JDBC
	 * Statement.setTimeout method.
	 */
	String PERSISTENCE_QUERY_TIMEOUT = "jakarta.persistence.query.timeout";
	/**
	 * This hint defines the timeout in milliseconds to acquire a pessimistic lock.
	 */
	String PERSISTENCE_LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";
	/**
	 * Valid values are USE or BYPASS. If the setting is not recognised, it defaults to USE.
	 * <p>
	 * The retrieveMode hint supports the values USE and BYPASS and tell Query implementation if it shall USE the second-level
	 * cache to retrieve an entity or if it shall BYPASS it and get it directly from the database.
	 */
	String PERSISTENCE_CACHE_RETRIEVEMODE = "jakarta.persistence.cache.retrieveMode";

	/**
	 * Valid values are USE, BYPASS or REFRESH. If the setting is not recognised, it defaults to USE.
	 * <p>
	 * The storeMode hint controls the storage and update of the second-level cache.
	 */
	String PERSISTENCE_CACHE_STOREMODE = "jakarta.persistence.cache.storeMode";

	/**
	 * If set to true entities retrieved in {@link Query#getResultList()} is also cached
	 */
	String PERSISTENCE_CACHE_RESULTLIST = "jpalite.cache.resultList";


	/**
	 * Hint the JQPL parser to ignore fetchtype setting on basic fields effectively setting all basic fields to be
	 * EAGERly fetched.
	 */
	String PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE = "jpalite.override.basicFetchType";
	/**
	 * Valid values are EAGER or LAZY. If the setting is not recognised it is ignored.
	 * <p>
	 * Hint the JQPL parser to ignore fetchtype settings on all fields and effectively setting all fields to be EAGERly
	 * or LAZYly fetched.
	 */
	String PERSISTENCE_OVERRIDE_FETCHTYPE = "jpalite.override.FetchType";
	/**
	 * Valid values are TRUE or FALSE. If the setting is not recognized it is ignored. A hint that can be passed to the
	 * Entity Manager or any Query to log the actual query that is executed.
	 */
	String PERSISTENCE_SHOW_SQL = "jpalite.showSql";

	/**
	 * Synchronize the entity to the underlying database.
	 *
	 * @throws TransactionRequiredException if there is no transaction or if the entity manager has not been joined to
	 *                                      the current transaction
	 * @throws PersistenceException         if the flush fails
	 * @throws IllegalStateException        if the connection is not open
	 */
	<T> void flushEntity(@Nonnull T entity);

	/**
	 * Synchronize the all entity of a given type belonging to the persistence context to the underlying database.
	 *
	 * @throws TransactionRequiredException if there is no transaction or if the entity manager has not been joined to
	 *                                      the current transaction
	 * @throws PersistenceException         if the flush fails
	 * @throws IllegalStateException        if the connection is not open
	 */
	void flushOnType(Class<?> entityClass);

	/**
	 * Given a ResultSet, map that to the given entity and attach the entity to the persistence context If there is an
	 * active transaction and the entity is already under management of the persistence context, the result will be
	 * merged with the existing entity and that entity will be return.
	 *
	 * @param entity    The entity
	 * @param resultSet The result
	 * @return The mapped entity.
	 */
	<X> X mapResultSet(@Nonnull X entity, ResultSet resultSet);

	/**
	 * Clone a given entity returning an entity in a transient state where all the fields are set to the values found
	 * the given entity. Note that identity and version fields are not cloned.
	 *
	 * @param entity The entity to clone
	 * @return The cloned entity
	 */
	<T> T clone(@Nonnull T entity);
}//JPALiteEntityManager
