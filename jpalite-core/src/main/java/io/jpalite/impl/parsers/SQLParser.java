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

package io.jpalite.impl.parsers;

import io.jpalite.impl.queries.QueryParameterImpl;
import io.jpalite.parsers.QueryParser;
import io.jpalite.parsers.QueryStatement;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S1452") //generic wildcard is required
public class SQLParser implements QueryParser
{
	/**
	 * We may use either positional or named parameters, but we cannot mix them within the same query.
	 */
	private boolean usingNamedParameters;
	/**
	 * Map of parameters used in the query
	 */
	private final List<QueryParameterImpl<?>> queryParameters;
	/**
	 * The parsed SQL statement
	 */
	private final String query;
	/**
	 * Thw query statement
	 */
	private final QueryStatement queryStatement;
	/**
	 * The query hints
	 */
	@SuppressWarnings({"java:S1068", "unused", "MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal"})
//Will be used later
	private final Map<String, Object> queryHints;
	/**
	 * Indicator that only primary keys are used
	 */
	private final boolean selectUsingPrimaryKey;

	/**
	 * Constructor for the class. The method takes as input a JQPL Statement and converts it to a Native Statement. Note
	 * that the original pStatement is modified
	 *
	 * @param rawQuery   The sql query
	 * @param queryHints The query hints
	 */
	public SQLParser(String rawQuery, Map<String, Object> queryHints)
	{
		this.queryHints = new HashMap<>(queryHints);
		usingNamedParameters = false;
		queryParameters = new ArrayList<>();
		selectUsingPrimaryKey = false;

		query = processSQLParameterLabels(rawQuery);
		String statement = query.substring(0, query.indexOf(' ')).toUpperCase();
		queryStatement = switch (statement) {
			case "INSERT" -> QueryStatement.INSERT;
			case "UPDATE" -> QueryStatement.UPDATE;
			case "SELECT" -> QueryStatement.SELECT;
			case "DELETE" -> QueryStatement.DELETE;
			default -> QueryStatement.OTHER;
		};
	}//SQLParser

	@Override
	public QueryStatement getStatement()
	{
		return queryStatement;
	}

	@SuppressWarnings("java:S127") // We need to update the counter to skip the next character
	private String processSQLParameterLabels(String inputQuery)
	{
		String sql = inputQuery;

		boolean inSingleQuote = false;
		boolean inDblQuote = false;
		int nrParams = 0;
		for (int i = 0; i < sql.length(); i++) {
			switch (sql.charAt(i)) {
				case ':':
					if (!inSingleQuote && !inDblQuote) {
						//Check if we have a double colon. If so, skip it
						if (sql.length() > i + 1 && sql.charAt(i + 1) == ':') {
							i++;
							break;
						}//if

						//Find first space after the name eg :Param1 , :Param2
						int end = sql.indexOf(',', i) - i;
						int closeBracket = sql.indexOf(')', i) - i;
						int space = sql.indexOf(' ', i) - i;
						if (end < -1 || (closeBracket > -1 && end > closeBracket)) {
							end = closeBracket;
						}//if
						if (end < -1 || space > -1 && end > space) {
							end = space;
						}//if
						if (end < -1) {
							end = sql.length();
						}//if
						else {
							end += i;
						}//else

						nrParams++;

						/*
						 * Catch case where Oracle syntax is used (col=:1, col=:2) and flag
						 * that as also using number based parameters
						 */
						String parameterName = sql.substring(i + 1, end);
						if (StringUtils.isNumeric(parameterName)) {
							parameterName = null;
						}//if
						addQueryParameter(nrParams, parameterName);
						sql = sql.substring(0, i) + "?" + sql.substring(end);
					}//if
					break;

				case '?':
					if (!inSingleQuote && !inDblQuote) {
						nrParams++;
						addQueryParameter(nrParams, null);
					}//if
					break;

				case '\'':
					inSingleQuote = !inSingleQuote;
					break;

				case '"':
					inDblQuote = !inDblQuote;
					break;

				default:
					break;
			}//switch
		}//for

		return sql;
	}//checkQuery

	@Override
	public String getQuery()
	{
		return query;
	}//getQuery

	@Override
	public boolean isUsingNamedParameters()
	{
		return usingNamedParameters;
	}

	@Override
	public int getNumberOfParameters()
	{
		return queryParameters.size();
	}//getNumberOfParameters

	@Override
	public boolean isSelectUsingPrimaryKey()
	{
		return selectUsingPrimaryKey;
	}

	@Override
	public List<QueryParameterImpl<?>> getQueryParameters()
	{
		return queryParameters;
	}

	private void addQueryParameter(int index, String name)
	{
		if (name == null) {
			if (queryParameters.isEmpty()) {
				usingNamedParameters = false;
			}//if
			else if (usingNamedParameters) {
				throw new IllegalArgumentException("Mixing positional and named parameters are not allowed");
			}//else if

			queryParameters.add(new QueryParameterImpl<>(index, Object.class));
		}//if
		else {
			if (queryParameters.isEmpty()) {
				usingNamedParameters = true;
			}//if
			else if (!usingNamedParameters) {
				throw new IllegalArgumentException("Mixing positional and named parameters are not allowed");
			}//else if

			queryParameters.add(new QueryParameterImpl<>(name, queryParameters.size() + 1, Object.class));
		}//else
	}//addQueryParameter
}
