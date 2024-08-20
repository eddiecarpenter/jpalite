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

package org.jpalite.impl.queries;

import org.jpalite.PersistenceContext;
import org.jpalite.queries.QueryLanguage;
import jakarta.annotation.Nonnull;
import jakarta.persistence.NamedNativeQuery;

import java.util.Map;

public class NamedNativeQueryImpl<T> extends TypedQueryImpl<T>
{
	public NamedNativeQueryImpl(@Nonnull NamedNativeQuery namedQuery, PersistenceContext persistenceContext, Class<T> entityClass, @Nonnull Map<String, Object> hints)
	{
		super(namedQuery.query(), QueryLanguage.NATIVE, persistenceContext, entityClass, NamedQueryImpl.buildHints(hints, namedQuery.hints()));
	}//NamedNativeQueryImpl
}//NamedNativeQueryImpl
