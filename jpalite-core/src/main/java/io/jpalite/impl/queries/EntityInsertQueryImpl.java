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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EntityInsertQueryImpl implements EntityQuery
{
	private final EntityMetaData<?> metaData;
	private final List<Object> parameters;
	private final String query;

	public EntityInsertQueryImpl(JPAEntity entity, EntityMetaData<?> metaData)
	{
		this.metaData = metaData;
		parameters = new ArrayList<>();
		query = buildQuery(entity);
	}//EntityInsertQueryImpl

	@Override
	public QueryLanguage getLanguage()
	{
		return QueryLanguage.NATIVE;
	}

	private Object generateVersionValue(EntityField versionField, Object currentVal)
	{
		return switch (versionField.getType().getSimpleName()) {
			case "Long", "long" -> (currentVal == null) ? 1L : ((Long) currentVal) + 1;

			case "Integer", "int" -> (currentVal == null) ? 1 : ((Integer) currentVal) + 1;

			case "Timestamp" -> new Timestamp(System.currentTimeMillis());
			default -> throw new IllegalStateException("Version field has unsupported type");
		};
	}//generateVersionValue

	private String buildQuery(JPAEntity entity)
	{
		String sqlQuery = "insert into " + metaData.getTable() + "(";
		StringBuilder columns = new StringBuilder("");
		StringBuilder values = new StringBuilder("");

		for (EntityField field : metaData.getEntityFields()) {
			if (!field.isInsertable()
					|| field.getMappingType() == MappingType.ONE_TO_MANY
					|| (field.isNullable() && entity._isLazyLoaded(field.getName()))) {
				continue;
			}//if

			Object val = entity._getField(field.getName());
			if (field.isVersionField()) {
				val = generateVersionValue(metaData.getVersionField(), val);
			}//if

			//If the column is nullable always update it. If not nullable
			//and the value is null skip the column
			if (field.isNullable() || val != null) {
				if (columns.length() > 0) {
					columns.append(",");
					values.append(",");
				}//if

				columns.append(field.getColumn());
				values.append("?");
				if (val instanceof JPAEntity entityField) {
					val = entityField._getPrimaryKey();
				}//if
				parameters.add(val);
			}//if
		}//for

		String returnCols = "";
		if (metaData.getIdField() != null) {
			String versionCol = metaData.hasVersionField() ? "," + metaData.getVersionField().getColumn() : "";
			returnCols = "returning " + metaData.getIdField().getColumn() + versionCol;
		}//if

		sqlQuery += columns + ") values(" + values + ")" + returnCols;

		entity._clearModified();
		return sqlQuery;
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
}//EntityInsertQueryImpl
