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
import io.jpalite.MappingType;
import io.jpalite.queries.EntityQuery;
import io.jpalite.queries.QueryLanguage;
import jakarta.persistence.PersistenceException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EntityUpdateQueryImpl implements EntityQuery
{
	private final EntityMetaData<?> metaData;
	private final List<Object> parameters;
	private final String query;

	public EntityUpdateQueryImpl(JPAEntity entity, EntityMetaData<?> metaData)
	{
		this.metaData = metaData;
		parameters = new ArrayList<>();
		query = buildQuery(entity);
	}//EntityInsertQuery

	private Object generateVersionValue(EntityField versionField, Object currentVal)
	{
		return switch (versionField.getType().getSimpleName()) {
			case "Long", "long" -> (currentVal == null) ? 1L : ((Long) currentVal) + 1;

			case "Integer", "int" -> (currentVal == null) ? 1 : ((Integer) currentVal) + 1;

			case "Timestamp" -> new Timestamp(System.currentTimeMillis());
			default -> throw new IllegalStateException("Version field has unsupported type");
		};
	}//generateVersionValue

	@Override
	public QueryLanguage getLanguage()
	{
		return QueryLanguage.NATIVE;
	}

	@SuppressWarnings("java:S3776")//Complexity is reduced as far as possible
	private void addFields(JPAEntity entity, StringBuilder columns, StringBuilder where, List<Object> whereParams)
	{
		for (EntityField field : metaData.getEntityFields()) {
			if (!entity._isLazyLoaded(field.getName())) {
				Object val = entity._getDBValue(field.getName());

				if (field.getMappingType() == MappingType.EMBEDDED && val instanceof JPAEntity vLinkEntity && vLinkEntity._isEntityModified()) {
					addFields(vLinkEntity, columns, where, whereParams);
				}//if
				else {
					if (field.isIdField() || field.isVersionField()) {
						if (!where.isEmpty()) {
							where.append(" and ");
						}//if
						where.append(field.getColumn()).append("=?");
						whereParams.add(val);
					}//if

					if ((!field.isIdField() && entity._isFieldModified(field.getName()) && field.isUpdatable() && field.getMappingType() != MappingType.ONE_TO_MANY) || field.isVersionField()) {
						/*
						 The JPA Specification states that for versioned objects, it is permissible for an implementation to use
						 LockMode- Type.OPTIMISTIC_FORCE_INCREMENT where LockModeType.OPTIMISTIC was requested, but not vice versa.
						 We choose to handle Type.OPTIMISTIC as Type.OPTIMISTIC_FORCE_INCREMENT
						 */
						if (field.isVersionField()) {
							if (entity._isFieldModified(field.getName())) {
								throw new PersistenceException("Version field was modified!");
							}//if

							final Object newVersion = generateVersionValue(field, val);
							val = newVersion;
							entity._updateRestrictedField(e -> field.invokeSetter(e, newVersion));
						}//if

						if (val instanceof JPAEntity linkEntity) {
							val = linkEntity._getPrimaryKey();
						}//if

						if (val != null || field.isNullable()) {
							if (!columns.isEmpty()) {
								columns.append(",");
							}//if
							columns.append(field.getColumn()).append("=?");
							parameters.add(val);
						}//if
					}//if
				}//else
			}//if
		}//for
	}//addFields

	private String buildQuery(JPAEntity entity)
	{
		if (!entity._isEntityModified()) {
			return null;
		}//if

		String sqlQuery = "update " + metaData.getTable() + " set ";
		StringBuilder columns = new StringBuilder();
		StringBuilder where = new StringBuilder();
		List<Object> params = new ArrayList<>();

		addFields(entity, columns, where, params);

		if (columns.isEmpty()) {
			return null;
		}

		parameters.addAll(params);

		return sqlQuery + columns + " where " + where;
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
