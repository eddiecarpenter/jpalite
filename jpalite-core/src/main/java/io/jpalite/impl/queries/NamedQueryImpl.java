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

import io.jpalite.PersistenceContext;
import io.jpalite.queries.QueryLanguage;
import jakarta.annotation.Nonnull;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

import java.util.HashMap;
import java.util.Map;

public class NamedQueryImpl<T> extends TypedQueryImpl<T>
{
	public static final @Nonnull Map<String, Object> buildHints(Map<String, Object> hints, QueryHint[] namedHints)
	{
		Map<String, Object> newHints = new HashMap<>();
		newHints.putAll(hints);
		for (QueryHint hint : namedHints) {
			newHints.put(hint.name(), hint.value());
		}//for
		return newHints;
	}//buildHints

	public NamedQueryImpl(@Nonnull NamedQuery namedQuery, PersistenceContext persistenceContext, Class<T> entityClass, @Nonnull Map<String, Object> hints)
	{
		super(namedQuery.query(), QueryLanguage.JPQL, persistenceContext, entityClass, buildHints(hints, namedQuery.hints()));
		setLockMode(namedQuery.lockMode());
	}//NamedQueryImpl
}//NamedQueryImpl
