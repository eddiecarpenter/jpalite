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

import io.jpalite.parsers.QueryParser;
import io.jpalite.queries.QueryLanguage;
import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.jpalite.JPALiteEntityManager.PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE;
import static io.jpalite.JPALiteEntityManager.PERSISTENCE_OVERRIDE_FETCHTYPE;

public class QueryParserFactory
{
	private static final Map<String, QueryParser> PARSED_QUERIES = new ConcurrentHashMap<>();

	private QueryParserFactory()
	{
	}

	/**
	 * Factory for query parsers used in JPA Lite
	 *
	 * @param rawQuery   The JQPL query
	 * @param queryHints The query hints
	 */
	public static QueryParser getParser(QueryLanguage language, String rawQuery, Map<String, Object> queryHints)
	{
		/*
		 * If we override the fetching definition on the entity, we need to reparse the query.
		 */
		FetchType overrideFetch = (FetchType) queryHints.get(PERSISTENCE_OVERRIDE_FETCHTYPE);
		FetchType overrideBasicFetch = (FetchType) queryHints.get(PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE);
		String cacheKey = rawQuery +
				language +
				((overrideFetch == null) ? "NONE" : overrideFetch) +
				((overrideBasicFetch == null) ? "NONE" : overrideBasicFetch);

		QueryParser parser = PARSED_QUERIES.get(cacheKey);
		if (parser == null) {
			parser = switch (language) {
				case NATIVE -> new SQLParser(rawQuery, queryHints);
				case JPQL -> new JPQLParser(rawQuery, queryHints);
				default -> throw new PersistenceException("Not supported");
			};

			PARSED_QUERIES.put(cacheKey, parser);
		}//if

		return parser;
	}//getParser
}
