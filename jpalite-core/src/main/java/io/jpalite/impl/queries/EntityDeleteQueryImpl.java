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

import io.jpalite.EntityField;
import io.jpalite.EntityMetaData;
import io.jpalite.JPAEntity;
import io.jpalite.queries.EntityQuery;
import io.jpalite.queries.QueryLanguage;
import jakarta.persistence.PersistenceException;

import java.util.ArrayList;
import java.util.List;

public class EntityDeleteQueryImpl implements EntityQuery
{
	private final EntityMetaData<?> metaData;
	private final List<Object> parameters;
	private final String query;

	public EntityDeleteQueryImpl(JPAEntity entity, EntityMetaData<?> metaData)
	{
		this.metaData = metaData;
		parameters = new ArrayList<>();
		query = buildQuery(entity);
	}//EntityDeleteQueryImpl

	@Override
	public QueryLanguage getLanguage()
	{
		return QueryLanguage.NATIVE;
	}

	private String buildQuery(JPAEntity entity)
	{
		EntityField[] idFields = metaData.getIdFields().toArray(new EntityField[0]);
		if (idFields.length == 0) {
			throw new PersistenceException("The entity have no @Id columns and cannot be deleted");
		}//if

		StringBuilder query = new StringBuilder();
		query.append("delete from ")
				.append(metaData.getTable())
				.append(" where ");

		int paramNr = 0;
		for (EntityField field : idFields) {
			if (paramNr > 0) {
				query.append(" and ");
			}//if
			query.append(field.getColumn()).append("=?");
			parameters.add(entity._getField(field.getName()));
			paramNr++;
		}//for

		/*
		 The JPA Specification states that for versioned objects, it is permissible for an implementation to use
		 LockMode- Type.OPTIMISTIC_FORCE_INCREMENT where LockModeType.OPTIMISTIC was requested, but not vice versa.
		 We choose to handle Type.OPTIMISTIC as Type.OPTIMISTIC_FORCE_INCREMENT
		 */
		if (entity._getMetaData().hasVersionField()) {
			EntityField field = entity._getMetaData().getVersionField();
			if (entity._isFieldModified(field.getName())) {
				throw new PersistenceException("Version field was modified!");
			}//if

			query.append(" and ")
					.append(field.getColumn()).append("=?");
			parameters.add(entity._getField(field.getName()));
		}//if

		return query.toString();
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
}//EntityInsertInsertQuery
