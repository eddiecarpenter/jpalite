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

package io.jpalite.parsers;

import io.jpalite.impl.queries.JPALiteQueryImpl;
import io.jpalite.impl.queries.QueryParameterImpl;

import java.util.Collections;
import java.util.List;

/**
 * The QueryParser interface is to be implemented by all classes that implements Query Parsers used by
 * {@link JPALiteQueryImpl} to parser query of various languages into a format that can be executed by the
 * Persistent context linked to query
 */
@SuppressWarnings("java:S1452") //generic wildcard is required
public interface QueryParser
{
	/**
	 * Get the query statement
	 *
	 * @return The query statement
	 */
	QueryStatement getStatement();

	/**
	 * Return the parsed query
	 *
	 * @return The parsed and converted query
	 */
	String getQuery();

	/**
	 * Provides a check to determine the type of query parameters was used in the raw query
	 *
	 * @return True for named and false for positional base parameters
	 */
	boolean isUsingNamedParameters();

	/**
	 * Indicates if the query has parameters
	 */
	int getNumberOfParameters();

	/**
	 * The query parameters found in the raw query. The map is in the order that the parameter appeared in the query.
	 * The key of the map is either the named parameter or the positional number.
	 *
	 * @return A parameter map
	 */
	List<QueryParameterImpl<?>> getQueryParameters();

	/**
	 * The return types return for each select item. The list specifies the java class type for each of the select items
	 * in the raw query.
	 * <p>
	 * Note that the parsed query could have more select items that what is contained in the list.
	 * <p>
	 * If the parser do not support this an empty list is returned
	 * </p>
	 *
	 * @return The list
	 */
	default List<Class<?>> getReturnTypes()
	{
		return Collections.emptyList();
	}

	/**
	 * Check to see if only PK was used in the where clause.
	 *
	 * @return True if PK was used
	 */
	default boolean isSelectUsingPrimaryKey()
	{
		return false;
	}

	/**
	 * Check if the given return type is provided by the JQPL guery. If not an IllegalArgumentException exception is
	 * generated
	 * <p>
	 * If not used the method always return true
	 * </p>
	 *
	 * @param typeToCheck The class to check
	 * @throws IllegalArgumentException if the type is not provided
	 */
	default void checkType(Class<?> typeToCheck)
	{
	}
}//QueryParser
