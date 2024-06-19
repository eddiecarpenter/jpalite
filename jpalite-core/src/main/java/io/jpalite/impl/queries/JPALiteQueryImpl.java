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
import io.jpalite.*;
import io.jpalite.impl.db.ConnectionWrapper;
import io.jpalite.impl.parsers.QueryParserFactory;
import io.jpalite.parsers.QueryParser;
import io.jpalite.parsers.QueryStatement;
import io.jpalite.queries.QueryLanguage;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Date;
import java.util.*;

import static io.jpalite.JPALiteEntityManager.*;
import static jakarta.persistence.LockModeType.*;

@SuppressWarnings("DuplicatedCode")
@Slf4j
public class JPALiteQueryImpl<T> implements Query
{
	private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer(JPALiteQueryImpl.class.getName());
	public static final String SQL_QUERY = "query";
	public static final String MIXING_POSITIONAL_AND_NAMED_PARAMETERS_ARE_NOT_ALLOWED = "Mixing positional and named parameters are not allowed";
	/**
	 * The Persistence context link to the query
	 */
	private final PersistenceContext persistenceContext;
	/**
	 * The query (native) that will be executed
	 */
	private String query;
	/**
	 * The raw query that will be executed
	 */
	private final String rawQuery;
	/**
	 * The query language
	 */
	private final QueryLanguage queryLanguage;
	/**
	 * We may use either positional or named parameters but we cannot mix them within the same query.
	 */
	private boolean usingNamedParameters = false;
	/**
	 * True if selecting using primary key
	 */
	private boolean selectUsingPrimaryKey = false;
	private QueryStatement queryStatement = QueryStatement.OTHER;
	/**
	 * The parameters that have been set
	 */
	private List<QueryParameterImpl<?>> params;
	/**
	 * The query hints defined
	 */
	private final Map<String, Object> hints;
	/**
	 * The maximum number of rows to return for {@link #getResultList()}
	 */
	private int maxResults = Integer.MAX_VALUE;
	/**
	 * The number of rows in the cursor that should be skipped before returning a row.
	 */
	private int firstResult = 0;
	/**
	 * The lock mode of the returned item
	 */
	private LockModeType lockMode;
	/**
	 * The expected return type
	 */
	private final Class<?> resultClass;

	@Getter
	private String connectionName;
	private int queryTimeout;
	private int lockTimeout;
	private boolean bypassL2Cache;
	private boolean cacheResultList;
	private boolean showSql;
	private Class<?>[] queryResultTypes;
	private FieldType returnType;

	/**
	 * This method supports both Native and JPQL based queries.
	 * <p>
	 * resultClass defined the class the result will be mapped into and can be either and Entity Class or a base class
	 * or an array of base class types.
	 * <p>
	 * The query language parameter defined the type of query. The following types are supported:
	 * <p>
	 * <b>JPQL queries</b><br>
	 * JPQL queries can either be a single or a multi select query.
	 * <p>
	 * A Single Select query is a query that only have one entity (eg select e from Employee e) or a specific field in
	 * an entity (eg select e.name from Employee E). In the case of a single select query resultClass MUST match the
	 * type of select expression.
	 * <p>
	 * A Multi select query is a query that have more than one entity or entity fields eg (select e, d from Employee e
	 * JOIN e.department d) or (select e.name, e.department from Employee e). In the case of a multi select query
	 * resultClass must be an Object array (Object[].class).
	 * <p>
	 * An exception to the above is if the selection return different unique types of only entities ( eg select e,
	 * e.department from Employee e) in which case resultClass could be the specific Entity in the multi select result
	 * set. This only applies to Entities and not entity fields!
	 * <br>
	 * <br>
	 * <p>
	 * <b>Native Queries</b><br>
	 * Native Queries are normal SQL queries and can also have a single or multi select query. resultClass can either
	 * be a specific Entity class, a specific base class or a base type array. If an entity class is specified as the
	 * result class, the result set mapping process will try and use the column names found in the result set to map the
	 * result to the entity class.
	 * <p>
	 * NOTE: Only the @Basic fields in the entity will (or can) be mapped.
	 *
	 * @param queryText          The query to execute
	 * @param queryLanguage      The query language
	 * @param persistenceContext The persistence context to use for the query
	 * @param resultClass        The expected result class
	 */
	public JPALiteQueryImpl(String queryText, QueryLanguage queryLanguage, PersistenceContext persistenceContext, Class<T> resultClass, @Nonnull Map<String, Object> hints, LockModeType lockMode)
	{
		Span span = TRACER.spanBuilder("JPAQuery::Init").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (queryText == null || queryText.isEmpty()) {
				throw new IllegalArgumentException("No query was specified");
			}//if

			Boolean globalShowSQL = (Boolean) persistenceContext.getProperties().get(PERSISTENCE_SHOW_SQL);
			showSql = globalShowSQL != null && globalShowSQL;
			this.lockMode = lockMode;
			rawQuery = queryText;
			this.queryLanguage = queryLanguage;
			this.persistenceContext = persistenceContext;
			this.resultClass = resultClass;
			connectionName = persistenceContext.getConnectionName();
			bypassL2Cache = false;
			queryTimeout = 0;
			lockTimeout = 0;
			params = new ArrayList<>();
			queryResultTypes = null;
			query = null;
			cacheResultList = false;

			//Check that a valid return class was specified
			checkResultClass(resultClass);

			this.hints = new HashMap<>();
			hints.forEach(this::setHint);

			span.setAttribute("queryLang", this.queryLanguage.name());
			span.setAttribute(SQL_QUERY, queryText);
		}//try
		finally {
			span.end();
		}
	}//JpaLiteQueryImpl

	public JPALiteQueryImpl(String queryText, QueryLanguage queryLanguage, PersistenceContext persistenceContext, Class<T> resultClass, @Nonnull Map<String, Object> hints)
	{
		this(queryText, queryLanguage, persistenceContext, resultClass, hints, NONE);
	}//JpaLiteQueryImpl

	private void checkResultClass(Class<?> returnClass)
	{
		Class<?> checkedClass = returnClass;
		if (checkedClass.isArray()) {
			checkedClass = checkedClass.arrayType();
		}//if

		returnType = FieldType.fieldType(checkedClass);
	}//checkResultClass

	private void checkUsingPositionalParameters()
	{
		if (params.isEmpty()) {
			usingNamedParameters = false;
		}
		else if (usingNamedParameters) {
			throw new IllegalArgumentException(MIXING_POSITIONAL_AND_NAMED_PARAMETERS_ARE_NOT_ALLOWED);
		}//if
	}

	private void checkUsingNamedParameters()
	{
		if (params.isEmpty()) {
			usingNamedParameters = true;
		}
		else if (!usingNamedParameters) {
			throw new IllegalArgumentException(MIXING_POSITIONAL_AND_NAMED_PARAMETERS_ARE_NOT_ALLOWED);
		}//if
	}

	private Object getColumnValue(Object entity, ResultSet resultSet, int columnNr)
	{
		try {
			return switch (returnType) {
				case TYPE_BOOLEAN -> resultSet.getBoolean(columnNr);
				case TYPE_INTEGER -> resultSet.getInt(columnNr);
				case TYPE_LONGLONG -> resultSet.getLong(columnNr);
				case TYPE_DOUBLEDOUBLE -> resultSet.getDouble(columnNr);
				case TYPE_STRING -> resultSet.getString(columnNr);
				case TYPE_TIMESTAMP -> resultSet.getTimestamp(columnNr);
				case TYPE_ENTITY -> persistenceContext.mapResultSet(entity, "c" + columnNr + "_", resultSet);
				default -> resultSet.getObject(columnNr);
			};
		}//try
		catch (SQLException ex) {
			throw new PersistenceException("SQL Error reading column from result set", ex);
		}//catch
	}//getColumnValue

	@Nonnull
	private Object[] buildArray(@Nonnull ResultSet resultSet)
	{
		List<Object> resultList = new ArrayList<>();
		try {
			if (queryResultTypes.length == 0) {
				if (resultClass.isArray()) {
					ResultSetMetaData metaData = resultSet.getMetaData();
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						resultList.add(getColumnValue(null, resultSet, i));
					}//for
				}//if
			}//if
			else {
				for (int i = 1; i <= queryResultTypes.length; i++) {
					resultList.add(getColumnValue(getNewObject(queryResultTypes[i - 1]), resultSet, i));
				}//for
			}//else
		}//try
		catch (SQLException ex) {
			throw new PersistenceException("SQL Error mapping result to entity", ex);
		}//catch

		return resultList.toArray();
	}//buildArray

	private Object getNewObject(Class<?> returnClass)
	{
		if (returnType == FieldType.TYPE_ENTITY) {
			try {
				return returnClass.getConstructor().newInstance();
			}//try
			catch (InstantiationException | IllegalAccessException | InvocationTargetException |
				   NoSuchMethodException pE) {
				throw new PersistenceException("Error creating a new entity from class type " + returnClass.getName());
			}//catch
		}//if
		return new Object();
	}//getNewObject

	protected Object mapResultSet(ResultSet resultSet)
	{
		if (resultClass.isArray() && !resultClass.isAssignableFrom(byte[].class)) {
			return buildArray(resultSet);
		}//if
		else {
			if (returnType == FieldType.TYPE_ENTITY) {

				JPAEntity entity = (JPAEntity) getNewObject(resultClass);
				if (queryResultTypes.length == 0) {
					entity._mapResultSet(null, resultSet);
				}//if
				else {
					entity._mapResultSet("c1", resultSet);
				}//else

				//Check if the entity is not already in L1 Cache
				JPAEntity l1Entity = (JPAEntity) persistenceContext.l1Cache().find(entity.get$$EntityClass(), entity._getPrimaryKey());
				if (l1Entity == null) {
					persistenceContext.l1Cache().manage(entity);
				}//if
				else {
					entity = l1Entity;
				}

				return entity;
			}//if
			else {
				return getColumnValue(null, resultSet, 1);
			}
		}//else
	}//mapResultSet

	private PreparedStatement bindParameters(PreparedStatement statement) throws SQLException
	{
		for (QueryParameterImpl<?> parameter : params) {
			if (parameter.getValue() != null) {
				if (parameter.getValue().getClass().isAssignableFrom(Boolean.class)) {
					statement.setObject(parameter.getPosition(), Boolean.TRUE.equals(parameter.getValue()) ? 1 : 0, Types.OTHER);
				}
				else {
					if (parameter.getParameterType().equals(Object.class)) {
						statement.setObject(parameter.getPosition(), parameter.getValue(), Types.OTHER);
					}//if
					else {
						EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(parameter.getParameterType());
						for (EntityField entityField : metaData.getEntityFields()) {
							Object value = entityField.invokeGetter(parameter.getValue());
							statement.setObject(parameter.getPosition(), value, Types.OTHER);
						}//for
					}//else
				}//else
			}//if
			else {
				statement.setNull(parameter.getPosition(), Types.OTHER);
			}//else
		}//for

		return statement;
	}//bindParameters

	private String getQuery()
	{
		if (query == null) {
			processQuery();
		}//if

		return query;
	}//getQuery

	private String getQueryWithLimits(int firstResult, int maxResults)
	{
		String queryStr = getQuery();
		if (queryStatement == QueryStatement.SELECT && (firstResult > 0 || maxResults < Integer.MAX_VALUE)) {
			queryStr = "select * from (" + queryStr + ") __Q";
			if (firstResult > 0) {
				queryStr += " offset " + firstResult;
			}//if

			if (maxResults < Integer.MAX_VALUE) {
				queryStr += " limit " + maxResults;
			}//else
		}//if

		return queryStr;
	}//applyLimits

	private boolean isPessimisticLocking(LockModeType lockMode)
	{
		return (lockMode == PESSIMISTIC_READ || lockMode == PESSIMISTIC_FORCE_INCREMENT || lockMode == PESSIMISTIC_WRITE);
	}//isPessimisticLocking

	private String applyLocking(String sqlQuery)
	{
		if (queryStatement == QueryStatement.SELECT && isPessimisticLocking(lockMode)) {
			return sqlQuery + switch (lockMode) {
				case PESSIMISTIC_READ -> " FOR SHARE ";
				case PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_WRITE -> " FOR UPDATE ";
				default -> "";
			};
		}//if
		return sqlQuery;
	}//applyLocking

	@SuppressWarnings("java:S2077") // Dynamic formatted SQL is verified to be safe
	private void applyLockTimeout(Statement statement)
	{
		if (queryStatement == QueryStatement.SELECT && lockTimeout > 0 && isPessimisticLocking(lockMode)) {
			try {
				statement.execute("SET LOCAL lock_timeout = '" + lockTimeout + "s'");
			}//try
			catch (SQLException ex) {
				LOG.warn("Error setting lock timeout.", ex);
			}//catch
		}//if
	}//applyLockTimeout

	private Object executeQuery(String sqlQuery, SQLFunction<ResultSet, Object> function)
	{
		Span span = TRACER.spanBuilder("JPAQuery::executeQuery").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent();
			 Connection connection = persistenceContext.getConnection(getConnectionName());
			 PreparedStatement vStatement = bindParameters(connection.prepareStatement(sqlQuery))) {

			span.setAttribute(SQL_QUERY, sqlQuery);

			if (JPAEntity.class.isAssignableFrom(resultClass)) {
				persistenceContext.flushOnType(resultClass);
			}//if

			applyLockTimeout(vStatement);
			vStatement.setQueryTimeout(queryTimeout);

			boolean currentState = connection.unwrap(ConnectionWrapper.class).setEnableLogging(showSql);
			try (ResultSet vResultSet = vStatement.executeQuery()) {
				return function.apply(vResultSet);
			}//try
			finally {
				connection.unwrap(ConnectionWrapper.class).setEnableLogging(currentState);
			}//finally

		}//try
		catch (SQLTimeoutException ex) {
			throw new QueryTimeoutException("Query timeout after " + queryTimeout + " seconds");
		}//catch
		catch (SQLException ex) {
			if ("57014".equals(ex.getSQLState())) { //Postgresql state for query that timed out
				throw new QueryTimeoutException("Query timeout after " + queryTimeout + " seconds");
			}//if
			else {
				throw new PersistenceException("SQL Error executing the query: " + query, ex);
			}//else
		}//catch
		finally {
			span.end();
		}//finally
	}//executeQuery

	@Override
	@SuppressWarnings("unchecked")
	public List<T> getResultList()
	{
		Span span = TRACER.spanBuilder("JPAQuery::getResultList").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			span.setAttribute("resultType", resultClass.getSimpleName());

			if (lockMode != LockModeType.NONE && !persistenceContext.getTransaction().isActive()) {
				throw new TransactionRequiredException("No transaction is in progress");
			}//if

			if (maxResults < 0) {
				return Collections.emptyList();
			}//if

			String queryStr = applyLocking(getQueryWithLimits(firstResult, maxResults));
			return (List<T>) executeQuery(queryStr, r ->
			{
				List<T> resultList = new ArrayList<>();
				while (r.next()) {
					T entity = (T) mapResultSet(r);

					if (isPessimisticLocking(lockMode)) {
						((JPAEntity) entity)._setLockMode(lockMode);
					}//if

					if (cacheResultList && entity instanceof JPAEntity jpaEntity && jpaEntity._getMetaData().isCacheable()) {
						persistenceContext.l2Cache().add(jpaEntity);
					}//if
					resultList.add(entity);
				}//while
				return resultList;
			});
		}//try
		finally {
			span.end();
		}
	}//getResultList

	@SuppressWarnings("unchecked")
	private T checkCache()
	{
		T result = null;

		if (selectUsingPrimaryKey) {
			QueryParameterImpl<?> firstParam = params.stream().findFirst().orElse(null);

			//Only check L1 cache if the primaryKey is set
			if (firstParam != null) {
				Object primaryKey = firstParam.getValue();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Checking L1 cache for Entity [{}] using key [{}]", resultClass.getSimpleName(), primaryKey);
				}//if

				result = (T) persistenceContext.l1Cache().find(resultClass, primaryKey);
				if (result == null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Not found in L1 cache");
					}//if
					result = checkL2Cache(primaryKey);
				}//if
			}//if
		}//if

		return result;
	}//checkCache

	@SuppressWarnings("unchecked")
	private T checkL2Cache(Object primaryKey)
	{
		T result = null;

		if (selectUsingPrimaryKey && !bypassL2Cache) {
			EntityMetaData<T> metaData = EntityMetaDataManager.getMetaData(resultClass);
			if (metaData.isCacheable()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Checking L2 cache for Entity [{}] using key [{}]", resultClass.getSimpleName(), primaryKey);
				}//if

				result = (T) persistenceContext.l2Cache().find(resultClass, primaryKey);
				if (result instanceof JPAEntity entity) {
					persistenceContext.l1Cache().manage(entity);

					FetchType hintValue = (FetchType) hints.get(PERSISTENCE_OVERRIDE_FETCHTYPE);
					if (hintValue == null || hintValue.equals(FetchType.EAGER)) {
						entity._lazyFetchAll(hintValue != null);
					}//if
				}//if
				else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Not found in L2 cache");
					}//if
				}
			}//if
		}//if

		return result;
	}//checkCache

	@Override
	@SuppressWarnings("unchecked")
	public T getSingleResult()
	{
		Span span = TRACER.spanBuilder("JPAQuery::getSingleResult").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			span.setAttribute("resultType", resultClass.getSimpleName());

			//Must parse the query before check the cache
			String queryStr = applyLocking(getQueryWithLimits(firstResult, maxResults));

			if (returnType == FieldType.TYPE_ENTITY) {
				T result = checkCache();
				if (result != null) {
					return result;
				}//if
			}//if


			return (T) executeQuery(queryStr, r ->
			{
				if (r.next()) {
					T result = (T) mapResultSet(r);

					if (r.next()) {
						throw new NonUniqueResultException("Query did not return a unique result");
					}//if

					if (result instanceof JPAEntity jpaEntity) {
						if (jpaEntity._getMetaData().isCacheable() && !bypassL2Cache) {
							persistenceContext.l2Cache().add(jpaEntity);
						}//if

						if (isPessimisticLocking(lockMode)) {
							jpaEntity._setLockMode(lockMode);
						}//if
					}//if

					span.setAttribute("result", "Result found");
					return result;
				}//if
				else {
					span.setAttribute("result", "No Result found");
					throw new NoResultException("No Result found");
				}//else
			});
		}//try
		finally {
			span.end();
		}
	}//getSingleResult

	@Override
	public int executeUpdate()
	{
		Span span = TRACER.spanBuilder("JPAQuery::executeUpdate").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			span.setAttribute(SQL_QUERY, getQuery());

			if (queryStatement == QueryStatement.SELECT || queryStatement == QueryStatement.INSERT) {
				throw new IllegalStateException("SELECT and INSERT is not allowed in executeUpdate");
			}//if

			try (Connection connection = persistenceContext.getConnection(getConnectionName());
				 PreparedStatement statement = bindParameters(connection.prepareStatement(getQuery()))) {
				statement.setEscapeProcessing(false);

				boolean currentState = connection.unwrap(ConnectionWrapper.class).setEnableLogging(showSql);
				try {
					return statement.executeUpdate();
				}//try
				finally {
					connection.unwrap(ConnectionWrapper.class).setEnableLogging(currentState);
				}//finally
			}//try
			catch (SQLException ex) {
				throw new PersistenceException("SQL Error executing the update: " + query, ex);
			}//catch
		}//try
		finally {
			span.end();
		}
	}//executeUpdate

	@Override
	public Query setMaxResults(int maxResults)
	{
		if (maxResults < 0) {
			throw new IllegalArgumentException("The max results value cannot be negative");
		}//if

		this.maxResults = maxResults;
		return this;
	}//setMaxResults

	@Override
	public int getMaxResults()
	{
		return maxResults;
	}//getMaxResults

	@Override
	public Query setFirstResult(int startPosition)
	{
		if (startPosition < 0) {
			throw new IllegalArgumentException("The first results value cannot be negative");
		}//if

		firstResult = startPosition;
		return this;
	}//setFirstResult

	@Override
	public int getFirstResult()
	{
		return firstResult;
	}

	@Override
	@SuppressWarnings({"java:S6205"}) // This improves the readability of the assignment
	public Query setHint(String hintName, Object value)
	{
		hints.put(hintName, value);
		switch (hintName) {
			case PERSISTENCE_QUERY_TIMEOUT -> {
				if (value instanceof Long aLong) {
					queryTimeout = aLong.intValue();
				}
				else if (value instanceof Integer anInteger) {
					queryTimeout = anInteger;
				}
				else if (value instanceof String aString) {
					queryTimeout = Integer.parseInt(aString);
				}
			}
			case PERSISTENCE_LOCK_TIMEOUT -> {
				if (value instanceof Long aLong) {
					lockTimeout = aLong.intValue();
				}
				else if (value instanceof Integer anInteger) {
					lockTimeout = anInteger;
				}
				else if (value instanceof String aString) {
					lockTimeout = Integer.parseInt(aString);
				}
			}
			case PERSISTENCE_CACHE_RETRIEVEMODE -> {
				if (value instanceof CacheRetrieveMode mode) {
					bypassL2Cache = CacheRetrieveMode.BYPASS.equals(mode);
				}
				else {
					bypassL2Cache = CacheRetrieveMode.BYPASS.equals(CacheRetrieveMode.valueOf(value.toString()));
				}
			}
			case PERSISTENCE_SHOW_SQL -> {
				if (value instanceof Boolean showSqlHint) {
					this.showSql = showSqlHint;
				}//if
				else {
					showSql = Boolean.parseBoolean(value.toString());
				}
			}
			case PERSISTENCE_CACHE_RESULTLIST -> {
				EntityMetaData<T> vMetaData = EntityMetaDataManager.getMetaData(resultClass);
				if (vMetaData.isCacheable()) {
					cacheResultList = Boolean.parseBoolean(value.toString());
				}//if
				else {
					cacheResultList = false;
				}//else
			}
			case PERSISTENCE_PRIMARYKEY_USED -> selectUsingPrimaryKey = true;
			case PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE, PERSISTENCE_OVERRIDE_FETCHTYPE -> {
				if (value instanceof FetchType fetchType) {
					hints.put(hintName, fetchType);
				}//if
				else {
					hints.put(hintName, FetchType.valueOf(value.toString()));
				}
			}
			default -> LOG.trace("Unknown Query Hint[{}] - Ignored", hintName);
		}//switch

		return this;
	}

	@Override
	public Map<String, Object> getHints()
	{
		return hints;
	}

	@SuppressWarnings("java:S6126") // IDE adds tabs and spaces in a text block
	private void processQuery()
	{
		if (isPessimisticLocking(lockMode)) {
			/*
			  It is illegal to do a "SELECT FOR UPDATE" query that contains joins.
			  We are forcing the parser to generate a query that do not have any joins.
			 */
			hints.put(PERSISTENCE_OVERRIDE_FETCHTYPE, FetchType.LAZY);
		}//if

		try {
			QueryParser parser = QueryParserFactory.getParser(queryLanguage, rawQuery, hints);
			parser.checkType(resultClass);
			queryResultTypes = parser.getReturnTypes().toArray(new Class<?>[0]);
			query = parser.getQuery();

			if (usingNamedParameters != parser.isUsingNamedParameters()) {
				throw new IllegalArgumentException(MIXING_POSITIONAL_AND_NAMED_PARAMETERS_ARE_NOT_ALLOWED);
			}//if

			/*
			  Check that the correct parameters are have value.
			  Create a new list of parameters such that for every parameter used in the query
			  an entry exists.
			  The problem here is that for named parameters the same name could
			  be used more than once in the query (which is okay)
			 */
			List<QueryParameterImpl<?>> parameters = new ArrayList<>();
			parser.getQueryParameters().forEach(templateParam -> {
				QueryParameterImpl<?> providedParameter = params.stream()
						.filter(p -> p.getName().equals(templateParam.getName()))
						.findFirst()
						.orElse(null);

				if (providedParameter == null) {
					throw new IllegalArgumentException(String.format("Parameter '%s' is not set", templateParam.getName()));
				}//if

				parameters.add(templateParam.copyAndSet(providedParameter.getValue()));
			});
			params = parameters;

			selectUsingPrimaryKey = parser.isSelectUsingPrimaryKey();
			queryStatement = parser.getStatement();

			if (showSql) {
				LOG.info("\n------------ Query Parser -------------\n" +
								 "Query language: {}\n" +
								 "----------- Raw ----------\n" +
								 "{}\n" +
								 "---------- Parsed --------\n" +
								 "{}\n" +
								 "--------------------------------------",
						 queryLanguage, rawQuery, query);
			}//if
		}//try
		catch (PersistenceException ex) {
			LOG.error("Error parsing query. Language: {}, query: {}", queryLanguage, rawQuery);
			throw new QueryParsingException("Error parsing query", ex);
		}//catch
	}//processQuery

	@Override
	public <X> Query setParameter(Parameter<X> param, X value)
	{
		if (param.getName() != null) {
			return setParameter(param.getName(), value);
		}//if

		return setParameter(param.getPosition(), value);
	}//setParameter

	@Override
	public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType)
	{
		if (param.getName() != null) {
			return setParameter(param.getName(), value, temporalType);
		}//if

		return setParameter(param.getPosition(), value, temporalType);
	}//setParameter

	@Override
	public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType)
	{
		if (param.getName() != null) {
			return setParameter(param.getName(), value, temporalType);
		}//if

		return setParameter(param.getPosition(), value, temporalType);
	}//setParameter

	@SuppressWarnings("unchecked")
	private QueryParameterImpl<Object> findOrCreateParameter(String name)
	{
		checkUsingNamedParameters();

		QueryParameterImpl<?> param = params.stream()
				.filter(p -> p.getName().equals(name))
				.findFirst()
				.orElse(null);
		if (param == null) {
			param = new QueryParameterImpl<>(name, params.size() + 1, Object.class);
			params.add(param);
		}

		return (QueryParameterImpl<Object>) param;
	}

	@SuppressWarnings("unchecked")
	private QueryParameterImpl<Object> findOrCreateParameter(int position)
	{
		checkUsingPositionalParameters();
		QueryParameterImpl<?> param = params.stream()
				.filter(p -> p.getPosition() == position)
				.findFirst()
				.orElse(null);
		if (param == null) {
			param = new QueryParameterImpl<>(position, Object.class);
			params.add(param);
		}

		return (QueryParameterImpl<Object>) param;
	}

	@Override
	public Query setParameter(String pName, Object value)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(pName);
		parameter.setValue(value);

		return this;
	}//setParameter

	@Override
	public Query setParameter(String name, Calendar value, TemporalType temporalType)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(name);

		switch (temporalType) {
			case DATE -> parameter.setValue(new java.sql.Date(value.getTimeInMillis()));
			case TIME -> parameter.setValue(new java.sql.Time(value.getTimeInMillis()));
			case TIMESTAMP -> parameter.setValue(new Timestamp(value.getTimeInMillis()));
		}//switch

		return this;
	}//setParameter

	@Override
	public Query setParameter(String name, Date value, TemporalType temporalType)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(name);

		switch (temporalType) {
			case DATE -> parameter.setValue(new java.sql.Date(value.getTime()));
			case TIME -> parameter.setValue(new java.sql.Time(value.getTime()));
			case TIMESTAMP -> parameter.setValue(new Timestamp(value.getTime()));
		}//switch

		return this;
	}//setParameter

	@Override
	public Query setParameter(int position, Object value)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(position);
		parameter.setValue(value);

		return this;
	}//setParameter

	@Override
	public Query setParameter(int position, Calendar value, TemporalType temporalType)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(position);

		switch (temporalType) {
			case DATE -> parameter.setValue(new java.sql.Date(value.getTimeInMillis()));
			case TIME -> parameter.setValue(new java.sql.Time(value.getTimeInMillis()));
			case TIMESTAMP -> parameter.setValue(new Timestamp(value.getTimeInMillis()));
		}//switch

		return this;
	}//setParameter

	@Override
	public Query setParameter(int position, Date value, TemporalType temporalType)
	{
		QueryParameterImpl<Object> parameter = findOrCreateParameter(position);

		switch (temporalType) {
			case DATE -> parameter.setValue(new java.sql.Date(value.getTime()));
			case TIME -> parameter.setValue(new java.sql.Time(value.getTime()));
			case TIMESTAMP -> parameter.setValue(new Timestamp(value.getTime()));
		}//switch

		return this;
	}//setParameter

	@Override
	public Set<Parameter<?>> getParameters()
	{
		return new HashSet<>(params);
	}//getParameters

	@Override
	@Nonnull
	public Parameter<?> getParameter(String name)
	{
		checkUsingNamedParameters();

		Parameter<?> param = params.stream()
				.filter(p -> p.getName().equals(name))
				.findFirst()
				.orElse(null);
		if (param == null) {
			throw new IllegalArgumentException("Named parameter [" + name + "] does not exist");
		}//if

		return param;
	}//getParameters

	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public <X> Parameter<X> getParameter(String name, Class<X> type)
	{
		Parameter<?> parameter = getParameter(name);

		if (!type.isAssignableFrom(parameter.getParameterType())) {
			throw new IllegalArgumentException("Parameter [" + parameter.getParameterType().getName() + "] is not assignable to type " + type.getName());
		}//if

		return (Parameter<X>) parameter;
	}//getParameters

	@Override
	@Nonnull
	public Parameter<?> getParameter(int position)
	{
		checkUsingPositionalParameters();
		Parameter<?> param = params.stream()
				.filter(p -> p.getPosition() == position)
				.findFirst().orElse(null);
		if (param == null) {
			throw new IllegalArgumentException("Positional parameter [" + position + "] does not exist");
		}//if

		return param;
	}//getParameters

	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public <X> Parameter<X> getParameter(int position, Class<X> type)
	{
		Parameter<X> parameter = (Parameter<X>) getParameter(position);

		if (!type.isAssignableFrom(parameter.getParameterType())) {
			throw new IllegalArgumentException("Parameter [" + parameter.getParameterType().getName() + "] is not assignable to type " + type.getName());
		}//if

		return parameter;
	}//getParameters

	@Override
	public boolean isBound(Parameter<?> param)
	{
		if (param.getName() != null) {
			return ((QueryParameterImpl<?>) getParameter(param.getName())).isBounded();
		}//if

		return ((QueryParameterImpl<?>) getParameter(param.getPosition())).isBounded();
	}//isBound

	@Override
	@SuppressWarnings("unchecked")
	public <X> X getParameterValue(Parameter<X> param)
	{
		if (param.getName() != null) {
			return (X) getParameterValue(param.getName());
		}//if

		return (X) getParameterValue(param.getPosition());
	}//getParameterValue

	@Override
	public Object getParameterValue(String name)
	{
		QueryParameterImpl<?> vParameter = (QueryParameterImpl<?>) getParameter(name);

		return vParameter.getValue();
	}//getParameterValue

	@Override
	public Object getParameterValue(int position)
	{
		QueryParameterImpl<?> vParameter = (QueryParameterImpl<?>) getParameter(position);

		return vParameter.getValue();
	}//getParameterValue

	@Override
	public Query setFlushMode(FlushModeType flushMode)
	{
		throw new UnsupportedOperationException("FlushMode is not supported");
	}

	@Override
	public FlushModeType getFlushMode()
	{
		return FlushModeType.AUTO;
	}

	@Override
	public Query setLockMode(LockModeType lockMode)
	{
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public LockModeType getLockMode()
	{
		return lockMode;
	}

	@Override
	public <X> X unwrap(Class<X> cls)
	{
		throw new IllegalArgumentException("Could not unwrap this [" + this + "] as requested Java type [" + cls.getName() + "]");
	}
}
