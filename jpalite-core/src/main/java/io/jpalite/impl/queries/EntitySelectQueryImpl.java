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

package io.jpalite.impl.queries;

import io.jpalite.EntityMetaData;
import io.jpalite.queries.EntityQuery;
import io.jpalite.queries.QueryLanguage;

import java.util.ArrayList;
import java.util.List;

public class EntitySelectQueryImpl implements EntityQuery
{
	private final EntityMetaData<?> metadata;
	private final List<Object> parameters;
	private final String query;
	private QueryLanguage language;

	public EntitySelectQueryImpl(Object primaryKey, EntityMetaData<?> metadata)
	{
		this.metadata = metadata;
		parameters = new ArrayList<>();
		parameters.add(primaryKey);
		language = QueryLanguage.NATIVE;

		query = buildQuery();
	}

	@Override
	public QueryLanguage getLanguage()
	{
		return language;
	}

	private String buildQuery()
	{
		StringBuilder queryString = new StringBuilder("select ");

		language = QueryLanguage.JPQL;

		queryString.append(" e from ")
				.append(metadata.getName())
				.append(" e where e.")
				.append(metadata.getIdField().getName())
				.append("=?");

		return queryString.toString();
	}

	@Override
	public String getQuery()
	{
		return query;
	}

	@Override
	public Object[] getParameters()
	{
		return parameters.toArray();
	}
}//EntitySelectQueryImpl
