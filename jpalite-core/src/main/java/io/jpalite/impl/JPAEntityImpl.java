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

package io.jpalite.impl;

import io.jpalite.PersistenceContext;
import io.jpalite.*;
import io.jpalite.impl.queries.JPALiteQueryImpl;
import io.jpalite.impl.queries.QueryImpl;
import io.jpalite.impl.serializers.JPAEntityMarshaller;
import io.jpalite.queries.QueryLanguage;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.persistence.spi.LoadState;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContext;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static jakarta.persistence.LockModeType.*;

/**
 * This class will be made the super class of all entity classes defined and managed by the TradeSwitch Entity Manager.
 * <p>
 * The JPA Maven plugin class will modify the bytecode of all entity classes change the super class to piont to
 * this class.
 * <p>
 * To prevent any mishaps with duplicate method names hiding access to the class all methods here will be prefixed with
 * '_' and attributes with '$$' knowing that it is considered a bad naming convention and be flagged as such by the IDE
 * and SonarQube (hoping that, you, the developer, do not pick the same method and variable names as what I have been
 * using here ;-) )
 */
@SuppressWarnings({"java:S100", "java:S116"})
public class JPAEntityImpl implements JPAEntity, GeneratedSchema
{
	public static final String SELECT_CLAUSE = "select ";
	public static final String FROM_CLAUSE = " from ";
	public static final String WHERE_CLAUSE = " where ";
	/**
	 * A set of fields that was modified
	 */
	private final transient Set<String> $$modifiedList = new HashSet<>();
	/**
	 * A set of fields that must be loaded on first access
	 */
	private final transient Set<String> $$fetchLazy = new HashSet<>();
	/**
	 * The current entity state
	 */
	private transient EntityState $$state = EntityState.TRANSIENT;
	/**
	 * The action to perform on this entity when it is flushed by the persistence context
	 */
	private transient PersistenceAction $$pendingAction = PersistenceAction.NONE;
	/**
	 * The lock mode for the entity
	 */
	private transient LockModeType $$lockMode = LockModeType.NONE;
	/**
	 * The persistence context this entity belongs too.
	 */
	private transient PersistenceContext $$persistenceContext = null;
	/**
	 * The metadata for the entity
	 */
	private final transient EntityMetaData<?> $$metadata;
	/**
	 * Set to true if the entity is being mapped
	 */
	private transient boolean $$mapping = false;
	/**
	 * Set to true if the entity is lazy loaded.
	 */
	private transient boolean $$lazyLoaded = false;
	/**
	 * Indicator that an entity was created but no fields has been set yet.
	 */
	private transient boolean $$blankEntity = true;

	/**
	 * Control value to prevent recursive iteration by toString
	 */
	private transient boolean inToString = false;

	protected JPAEntityImpl()
	{
		if (EntityMetaDataManager.isRegistered(getClass())) {
			$$metadata = EntityMetaDataManager.getMetaData(getClass());

			//Find all BASIC and ONE_TO_MANY fields that are flagged as being lazily fetched and add them to our $$fetchLazy list
			$$metadata.getEntityFields()
					.stream()
					.filter(f -> f.getFetchType() == FetchType.LAZY && (f.getMappingType() == MappingType.BASIC || f.getMappingType() == MappingType.ONE_TO_MANY))
					.forEach(f -> $$fetchLazy.add(f.getName()));

			//Force the default lock mode to OPTIMISTIC_FORCE_INCREMENT if the entity has a version field
			if ($$metadata.hasVersionField()) {
				$$lockMode = OPTIMISTIC_FORCE_INCREMENT;
			}//if
		}//if
		else {
			$$metadata = null;
		}//else
	}//JPAEntityImpl

	@Override
	public Class<?> get$$EntityClass()
	{
		return getClass();
	}

	@Override
	public String toString()
	{
		if ($$metadata == null) {
			return super.toString();
		}//if

		StringBuilder toString = new StringBuilder(_getEntityInfo())
				.append(" ::")
				.append(_getStateInfo()).append(", ");

		toString.append(_getDataInfo());

		return toString.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o instanceof JPAEntityImpl e) {
			return _getPrimaryKey() != null && _getPrimaryKey().equals(e._getPrimaryKey());
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(_getPrimaryKey());
	}

	@Override
	public String _getEntityInfo()
	{
		return "Entity " + $$metadata.getName();
	}//_getEntityInfo

	@Override
	@SuppressWarnings({"java:S3776", "java:S3740"}) //The method cannot be simplified without increasing its complexity
	public String _getDataInfo()
	{
		StringBuilder toString = new StringBuilder();

		if (inToString) {
			toString.append(" [Circular reference detected]");
		}//if
		else {
			try {
				inToString = true;

				if ($$lazyLoaded) {
					toString.append(" [Lazy on PK=")
							.append(_getPrimaryKey())
							.append("] ");
				}//if
				else {
					toString.append("Data(");

					boolean first = true;
					for (EntityField field : _getMetaData().getEntityFields()) {
						if (!first) {
							toString.append(", ");
						}//if
						first = false;

						if (field.isIdField()) {
							toString.append("*");
						}//if
						toString.append(field.getName()).append("=");
						if ($$fetchLazy.contains(field.getName())) {
							toString.append("[Lazy]");
						}//if
						else {
							Object val = _getField(field.getName());
							if (val instanceof Map mapVal) {
								val = "[Map " + mapVal.size() + "  items]";
							}//if
							else if (val instanceof List listVal) {
								val = "[List " + listVal.size() + " items]";
							}//else if
							toString.append(val);
						}//else
					}//for
					toString.append(")");
				}//if
			}//try
			finally {
				inToString = false;
			}//finally
		}//else

		return toString.toString();
	}//_getDataInfo

	@Override
	public String _getStateInfo()
	{
		return " State:" + $$state + ", " + "Action:" + $$pendingAction;
	}//_getStateInfo

	@Override
	public JPAEntity _clone()
	{
		JPAEntityImpl clone = (JPAEntityImpl) $$metadata.getNewEntity();
		clone.$$blankEntity = false;
		_getMetaData().getEntityFields()
				.stream()
				.filter(f -> !f.isIdField() && !f.isVersionField())
				.forEach(f ->
						 {
							 Object vVal = f.invokeGetter(this);
							 f.invokeSetter(clone, vVal);
						 });
		clone.$$fetchLazy.addAll($$fetchLazy);
		return clone;
	}//_clone

	@Override
	public void _replaceWith(JPAEntity entity)
	{
		if (!_getMetaData().getName().equals(entity._getMetaData().getName())) {
			throw new IllegalArgumentException("Attempting to replace entities of different types");
		}//if

		if (_getEntityState() != EntityState.DETACHED && _getEntityState() != EntityState.TRANSIENT) {
			throw new IllegalArgumentException("The content of an entity can only be replaced if it is DETACHED or TRANSIENT");
		}//if

		if (entity._getEntityState() != EntityState.MANAGED && entity._getEntityState() != EntityState.DETACHED) {
			throw new IllegalArgumentException("The provided entity must be in an MANAGED or DETACHED state");
		}//if

		$$mapping = true;
		try {
			_getMetaData().getEntityFields()
					.stream()
					.filter(f -> !_isLazyLoaded(f.getName()))
					.forEach(f -> f.invokeSetter(this, f.invokeGetter(entity)));
			$$fetchLazy.clear();
			$$fetchLazy.addAll(((JPAEntityImpl) entity).$$fetchLazy);
			$$blankEntity = false;
			_setPendingAction(entity._getPendingAction());
			$$modifiedList.clear();
			$$modifiedList.addAll(((JPAEntityImpl) entity).$$modifiedList);

			entity._getPersistenceContext().l1Cache().manage(this);
			entity._getPersistenceContext().l1Cache().detach(entity);
		}//try
		finally {
			$$mapping = false;
		}
	}//_replaceWith

	@Override
	public void _refreshEntity(Map<String, Object> properties)
	{
		if ($$blankEntity) {
			throw new IllegalStateException("Entity is not initialised");
		}//if

		if (_getEntityState() == EntityState.TRANSIENT || _getEntityState() == EntityState.REMOVED || _getPersistenceContext() == null) {
			throw new IllegalStateException("Entity is not managed or detached");
		}//if

		if (_getPersistenceContext().isReleased()) {
			throw new LazyInitializationException("Entity is not attached to an active persistence context");
		}//if

		try {
			_clearModified();

			//Detach the entity from L1 cache
			PersistenceContext persistenceContext = _getPersistenceContext();
			persistenceContext.l1Cache().detach(this);

			String queryStr = SELECT_CLAUSE + $$metadata.getName() + FROM_CLAUSE + $$metadata.getName() + WHERE_CLAUSE + $$metadata.getIdField().getName() + "=:p";
			JPALiteQueryImpl<?> query = new JPALiteQueryImpl<>(queryStr,
															   QueryLanguage.JPQL,
															   persistenceContext,
															   $$metadata.getEntityClass(),
															   properties,
															   $$lockMode);
			query.setParameter("p", _getPrimaryKey());
			JPAEntity replaceEntity = (JPAEntity) query.getSingleResult();
			_replaceWith(replaceEntity);
			$$lazyLoaded = false;
		}//try
		catch (NoResultException ex) {
			throw new EntityNotFoundException(String.format("Lazy load of entity '%s' for key '%s' failed", $$metadata.getName(), _getPrimaryKey()));
		}
		catch (PersistenceException ex) {
			throw new LazyInitializationException("Error lazy fetching entity " + $$metadata.getName(), ex);
		}//catch
	}//_refreshEntity

	private void _queryOneToMany(EntityField entityField)
	{
		EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityField.getType());
		EntityField mappingField = metaData.getEntityField(entityField.getMappedBy());

		JPALiteQueryImpl<?> query = new JPALiteQueryImpl<>(SELECT_CLAUSE + metaData.getName() + FROM_CLAUSE + metaData.getName() + WHERE_CLAUSE + mappingField.getName() + "=:p",
														   QueryLanguage.JPQL,
														   _getPersistenceContext(),
														   metaData.getEntityClass(),
														   Collections.emptyMap());
		query.setParameter("p", _getPrimaryKey());
		entityField.invokeSetter(this, query.getResultList());
	}//_fetchOneToMany

	private void _queryBasicField(EntityField entityField)
	{
		String queryStr = SELECT_CLAUSE + " E." + entityField.getName() + FROM_CLAUSE + $$metadata.getName() + " E " + WHERE_CLAUSE + " E." + $$metadata.getIdField().getName() + "=:p";
		Query query = new QueryImpl(queryStr,
									_getPersistenceContext(),
									entityField.getType(),
									new HashMap<>());
		query.setParameter("p", _getPrimaryKey());

		//Will call _markField which will remove the field from the list
		entityField.invokeSetter(this, query.getSingleResult());
	}//_queryBasicField

	@Override
	public void _lazyFetchAll(boolean forceEagerLoad)
	{
		Set<String> lazyFields = new HashSet<>($$fetchLazy);
		lazyFields.forEach(this::_lazyFetch);
		_getMetaData().getEntityFields()
				.stream()
				.filter(f -> f.getMappingType().equals(MappingType.MANY_TO_ONE) && (forceEagerLoad || f.getFetchType() == FetchType.EAGER))
				.forEach(f -> {
					JPAEntity manyToOneField = (JPAEntity) f.invokeGetter(this);
					if (manyToOneField != null) {
						_getPersistenceContext().l1Cache().manage(manyToOneField);
						manyToOneField._refreshEntity(Collections.emptyMap());
					}//if
				});
	}//_lazyFetchAll

	@Override
	public void _lazyFetch(String fieldName)
	{
		//Lazy fetching is only applicable for MANAGED and DETACHED entities
		if (_getEntityState() == EntityState.TRANSIENT || _getEntityState() == EntityState.REMOVED) {
			return;
		}//if

		if (_isLazyLoaded()) {
			//Refresh the entity. Refreshing will also clear the lazy loaded flag
			_refreshEntity(Collections.emptyMap());
		}//if

		if ($$fetchLazy.contains(fieldName)) {
			if (_getPersistenceContext().isReleased()) {
				throw new LazyInitializationException("Entity is not attached to an active persistence context");
			}//if

			EntityField entityField = $$metadata.getEntityField(fieldName);
			if (entityField.getMappingType() == MappingType.BASIC) {
				_queryBasicField(entityField);
			}//if
			else {
				_queryOneToMany(entityField);
			}//else
		}//if
	}//_lazyFetch

	@Override
	public boolean _isLazyLoaded()
	{
		return $$lazyLoaded;
	}//_isLazyLoaded

	@Override
	public boolean _isLazyLoaded(String fieldName)
	{
		return $$fetchLazy.contains(fieldName);
	}

	@Override
	public void _markLazyLoaded()
	{
		$$lazyLoaded = true;
	}//_markLazyLoaded

	@Override
	public void _makeReference(Object primaryKey)
	{
		if (!$$blankEntity) {
			throw new IllegalArgumentException("Entity must be blank to be made into a reference");
		}//if

		_setPrimaryKey(primaryKey);
		_markLazyLoaded();
		_clearModified();
		_setPendingAction(PersistenceAction.NONE);
	}//_makeReference

	@Override
	public EntityMetaData<?> _getMetaData()
	{
		if ($$metadata == null) {
			throw new IllegalArgumentException(getClass() + " is not a known entity or not yet registered");
		}//if

		return $$metadata;
	}

	@Override
	public Set<String> _getModifiedFields()
	{
		return $$modifiedList;
	}

	@Override
	public void _clearModified()
	{
		$$modifiedList.clear();
		if ($$pendingAction == PersistenceAction.UPDATE) {
			$$pendingAction = PersistenceAction.NONE;
		}//if
	}

	@Override
	public boolean _isLegacyEntity()
	{
		return $$metadata.isLegacyEntity();
	}

	@Override
	public LoadState _loadState()
	{
		return (_isLazyLoaded() || $$blankEntity) ? LoadState.NOT_LOADED : LoadState.LOADED;
	}

	@Override
	public boolean _isFieldModified(String fieldName)
	{
		return $$modifiedList.contains(fieldName);
	}

	@Override
	public void _clearField(String fieldName)
	{
		$$modifiedList.remove(fieldName);
		if ($$modifiedList.isEmpty() && $$pendingAction == PersistenceAction.UPDATE) {
			$$pendingAction = PersistenceAction.NONE;
		}//if
	}

	@Override
	public void _markField(String fieldName)
	{
		if ($$metadata.isEntityField(fieldName)) {
			EntityField vEntityField = $$metadata.getEntityField(fieldName);

			if (!$$mapping && !_getEntityState().equals(EntityState.TRANSIENT) && vEntityField.isIdField()) {
				if (!_isLegacyEntity()) {
					throw new PersistenceException("The ID field cannot be modified");
				}//if
				LoggerFactory.getLogger(JPAEntityImpl.class).warn("Legacy Mode :: Allowing modifying of ID Field {} in Entity {}", vEntityField.getName(), $$metadata.getName());
			}//if

			if (!$$mapping && !_getEntityState().equals(EntityState.TRANSIENT) && vEntityField.isVersionField()) {
				throw new PersistenceException("A VERSION field cannot be modified");
			}//if

			if (!$$mapping && !_getEntityState().equals(EntityState.TRANSIENT) && !vEntityField.isUpdatable()) {
				if (!_isLegacyEntity()) {
					throw new PersistenceException("Attempting to updated a field that is marked as NOT updatable");
				}//if
				LoggerFactory.getLogger(JPAEntityImpl.class).warn("Legacy Mode :: Allowing modifying of NOT updatable field {} in Entity {}", vEntityField.getName(), $$metadata.getName());
			}//if

			/*
			 * _markField is call whenever a field is updated
			 * When this happens we can clear the $$blankEntity flag (as it is not true anymore! :-) )
			 * We are also clearing the fetch lazy status for this field, if any
			 * Lastly we are marking this fields as modified
			 */
			$$blankEntity = false;
			$$fetchLazy.remove(fieldName);

			/*
			 * ONE_TO_MANY fields is not really part of the current entity and any change to a ONE_TO_MANY field
			 * do not trigger an update to the current entity.
			 */
			if (!$$mapping && vEntityField.getMappingType() != MappingType.ONE_TO_MANY) {
				$$modifiedList.add(fieldName);
				if ($$pendingAction == PersistenceAction.NONE) {
					_setPendingAction(PersistenceAction.UPDATE);
				}//if
			}//if
		}//if
	}

	@Override
	public boolean _isEntityModified()
	{
		return !$$modifiedList.isEmpty();
	}

	@Override
	public LockModeType _getLockMode()
	{
		return $$lockMode;
	}

	@Override
	public void _setLockMode(LockModeType lockMode)
	{
		if (lockMode == OPTIMISTIC || lockMode == OPTIMISTIC_FORCE_INCREMENT || lockMode == WRITE || lockMode == READ) {
			if (!_getMetaData().hasVersionField()) {
				throw new PersistenceException("Entity has not version field");
			}//if

			/*
			 If the entity is not new and is not dirty but is locked optimistically, we need to update the version
			 The JPA Specification states that for versioned objects, it is permissible for an implementation to use
			 LockMode- Type.OPTIMISTIC_FORCE_INCREMENT where LockModeType.OPTIMISTIC/READ was requested, but not vice versa.
			 We choose to handle Type.OPTIMISTIC/READ) as Type.OPTIMISTIC_FORCE_INCREMENT
			 */
			lockMode = OPTIMISTIC_FORCE_INCREMENT;
		}//if
		if (lockMode == NONE && _getMetaData().hasVersionField()) {
			throw new PersistenceException("Entity has version field and cannot be locked with LockModeType.NONE");
		}//if

		$$lockMode = lockMode;
	}

	@Override
	public EntityState _getEntityState()
	{
		return $$state;
	}

	@Override
	public void _setEntityState(EntityState newState)
	{
		if ($$state != newState && newState != EntityState.REMOVED) {
			$$metadata.getEntityFields().stream()
					.filter(f -> f.getFieldType() == FieldType.TYPE_ENTITY && f.getMappingType() != MappingType.ONE_TO_MANY)
					.forEach(f -> {
						JPAEntity vEntity = (JPAEntity) f.invokeGetter(this);
						if (vEntity != null) {
							vEntity._setEntityState(newState);
						}//if
					});
		}//if
		$$state = newState;
	}

	@Override
	public PersistenceContext _getPersistenceContext()
	{
		return $$persistenceContext;
	}

	@Override
	public void _setPersistenceContext(PersistenceContext persistenceContext)
	{
		if ($$persistenceContext != persistenceContext) {
			$$persistenceContext = persistenceContext;
			$$metadata.getEntityFields().stream()
					.filter(f -> f.getFieldType() == FieldType.TYPE_ENTITY && f.getMappingType() != MappingType.ONE_TO_MANY)
					.forEach(f -> {
						JPAEntity vEntity = (JPAEntity) f.invokeGetter(this);
						if (vEntity != null) {
							vEntity._setPersistenceContext(persistenceContext);
						}//if
					});
		}//if
	}

	@Override
	public PersistenceAction _getPendingAction()
	{
		return $$pendingAction;
	}

	@Override
	public void _setPendingAction(PersistenceAction pendingAction)
	{
		$$pendingAction = pendingAction;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X _getField(@Nonnull String fieldName)
	{
		EntityField entityField = _getMetaData().getEntityField(fieldName);

		Object value = entityField.invokeGetter(this);
		if (value == null) {
			return null;
		}//if

		return (X) switch (entityField.getFieldType()) {
			case TYPE_CUSTOMTYPE -> entityField.getConverterClass().convertToDatabaseColumn(value);
			case TYPE_ENUM -> ((Enum<?>) value).name();
			case TYPE_ORDINAL_ENUM -> ((Enum<?>) value).ordinal();

			default -> value;
		};
	}//getField

	@Override
	public void _updateRestrictedField(Consumer<JPAEntity> method)
	{
		boolean mappingStatus = $$mapping;
		try {
			$$mapping = true;
			method.accept(this);
		}
		finally {
			$$mapping = mappingStatus;
		}
	}

	@Override
	public void _merge(JPAEntity entity)
	{
		if (!_getMetaData().getName().equals(entity._getMetaData().getName())) {
			throw new IllegalArgumentException("Attempting to merge entities of different types");
		}//if

		if (!entity._getPrimaryKey().equals(_getPrimaryKey())) {
			throw new EntityMapException("Error merging entities, primary key mismatch. Expected " + _getPrimaryKey() + ", but got " + entity._getPrimaryKey());
		}//if

		/*
		 * If the entity has a version field, we need to check that the version of the entity
		 * being merged matches the current version, except if the entity was created by reference.
		 */
		if (!$$lazyLoaded && $$metadata.hasVersionField()) {
			EntityField field = $$metadata.getVersionField();
			Object val = field.invokeGetter(entity);
			if (val != null && !val.equals(field.invokeGetter(this))) {
				throw new OptimisticLockException("Error merging entities, version mismatch. Expected " + field.invokeGetter(this) + ", but got " + val);
			}//if
		}//if

		for (String fieldName : entity._getModifiedFields()) {
			EntityField field = $$metadata.getEntityField(fieldName);
			if (!field.isIdField()) {
				field.invokeSetter(this, field.invokeGetter(entity));
			}//if
		}//for
		$$lazyLoaded = false;
	}//merge

	@Override
	public Object _getPrimaryKey()
	{
		if ($$metadata == null || $$metadata.getIdFields().isEmpty()) {
			return null;
		}//if


		if ($$metadata.getIdFields().size() > 1) {
			EntityMetaData<?> primaryKey = $$metadata.getIPrimaryKeyMetaData();
			Object primKey = null;
			if (primaryKey != null) {
				primKey = primaryKey.getNewEntity();
				for (EntityField entityField : $$metadata.getIdFields()) {
					EntityField keyField = primaryKey.getEntityField(entityField.getName());
					keyField.invokeSetter(primKey, entityField.invokeGetter(this));
				}//for
			}//if
			return primKey;
		}//if
		else {
			return $$metadata.getIdFields().getFirst().invokeGetter(this);
		}//else
	}//_getPrimaryKey

	@Override
	public void _setPrimaryKey(Object primaryKey)
	{
		if (_getEntityState() != EntityState.TRANSIENT) {
			throw new IllegalStateException("The primary key can only be set for an entity with a TRANSIENT state");
		}//if

		if ($$metadata.getIdFields().isEmpty()) {
			throw new IllegalStateException("Entity [" + $$metadata.getName() + "] do not have any ID fields");
		}//if


		if ($$metadata.getIdFields().size() > 1) {
			EntityMetaData<?> primaryKeyMetaData = $$metadata.getIPrimaryKeyMetaData();
			if (primaryKeyMetaData == null) {
				throw new IllegalStateException("Missing IDClass for Entity [" + $$metadata.getName() + "]");
			}//if

			for (EntityField entityField : $$metadata.getIdFields()) {
				EntityField keyField = primaryKeyMetaData.getEntityField(entityField.getName());
				entityField.invokeSetter(this, keyField.invokeGetter(primaryKey));
			}//for
		}//if
		else {
			$$metadata.getIdFields().getFirst().invokeSetter(this, primaryKey);
		}//else
	}//_setPrimaryKey

	//<editor-fold desc="All the Reader methods">
	protected String _JPAReadString(ResultSet resultSet, int column) throws SQLException
	{
		String value = resultSet.getString(column);
		return (resultSet.wasNull()) ? null : value;
	}//_JPAReadString

	protected Long _JPAReadLong(ResultSet resultSet, int column) throws SQLException
	{
		long value = resultSet.getLong(column);
		return (resultSet.wasNull()) ? null : value;
	}//_JPAReadLong

	protected Boolean _JPAReadBoolean(ResultSet resultSet, int column) throws SQLException
	{
		boolean value = resultSet.getBoolean(column);
		return (resultSet.wasNull() ? null : value);
	}//_JPAReadBoolean

	protected Integer _JPAReadInteger(ResultSet resultSet, int column) throws SQLException
	{
		int value = resultSet.getInt(column);
		return (resultSet.wasNull() ? null : value);
	}//_JPAReadInteger

	protected Double _JPAReadDouble(ResultSet resultSet, int column) throws SQLException
	{
		double value = resultSet.getDouble(column);
		return (resultSet.wasNull() ? null : value);
	}//_JPAReadDouble

	protected LocalDateTime _JPAReadLocalDateTime(ResultSet resultSet, int column) throws SQLException
	{
		Timestamp value = resultSet.getTimestamp(column);
		return (resultSet.wasNull() ? null : value.toLocalDateTime());
	}//_JPAReadLocalDateTime

	protected Object _JPAReadCustomType(ResultSet resultSet, EntityField field, int column) throws SQLException
	{
		return field.getConverterClass().convertToEntityAttribute(resultSet, column);
	}//_JPAReadCustomType

	private Object _JPAReadENUM(EntityField field, ResultSet row, int column) throws SQLException
	{
		String enumName = row.getString(column);
		for (Object enumValue : field.getType().getEnumConstants()) {
			if (((Enum<?>) enumValue).name().equals(enumName)) {
				return enumValue;
			}//if
		}//for

		return null;
	}//_JPAReadENUM

	private Object _JPAReadOrdinalENUM(EntityField field, ResultSet row, int column) throws SQLException
	{
		int ordinal = row.getInt(column);
		return field.getType().getEnumConstants()[ordinal];
	}//_JPAReadOrdinalENUM

	public JPAEntity _JPAReadEntity(EntityField field, ResultSet resultSet, String colPrefix, int col) throws SQLException
	{
		EntityMetaData<?> fieldMetaData = EntityMetaDataManager.getMetaData(field.getType());

		//Read the primary key of the field and then check if the entity is not already managed
		JPAEntity entity = (JPAEntity) fieldMetaData.getNewEntity();
		entity._setPersistenceContext(_getPersistenceContext());
		((JPAEntityImpl) entity)._JPAReadField(resultSet, fieldMetaData.getIdField(), colPrefix, col);

		JPAEntity managedEntity = null;
		if (entity._getPrimaryKey() != null) {
			if (_getPersistenceContext() != null) {
				managedEntity = (JPAEntity) _getPersistenceContext().l1Cache().find(fieldMetaData.getEntityClass(), entity._getPrimaryKey(), true);
			}//if

			if (managedEntity == null) {
				if (field.getFetchType() == FetchType.LAZY && (colPrefix == null || colPrefix.equals(resultSet.getMetaData().getColumnName(col)))) {
					entity._markLazyLoaded();
				}//if
				else {
					entity._mapResultSet(colPrefix, resultSet);
				}//else

				if (_getPersistenceContext() != null) {
					_getPersistenceContext().l1Cache().manage(entity);
				}//if
				return entity;
			}//if
		}//if

		return managedEntity;
	}//_JPAReadEntity

	@SuppressWarnings("java:S6205") // False error
	public void _JPAReadField(ResultSet row, EntityField field, String colPrefix, int columnNr)
	{
		try {
			$$mapping = true;
			switch (field.getFieldType()) {
				case TYPE_BOOLEAN -> field.invokeSetter(this, _JPAReadBoolean(row, columnNr));
				case TYPE_INTEGER -> field.invokeSetter(this, _JPAReadInteger(row, columnNr));
				case TYPE_LONGLONG -> field.invokeSetter(this, _JPAReadLong(row, columnNr));
				case TYPE_DOUBLEDOUBLE -> field.invokeSetter(this, _JPAReadDouble(row, columnNr));
				case TYPE_BOOL -> field.invokeSetter(this, row.getBoolean(columnNr));
				case TYPE_INT -> field.invokeSetter(this, row.getInt(columnNr));
				case TYPE_LONG -> field.invokeSetter(this, row.getLong(columnNr));
				case TYPE_DOUBLE -> field.invokeSetter(this, row.getDouble(columnNr));
				case TYPE_STRING -> field.invokeSetter(this, _JPAReadString(row, columnNr));
				case TYPE_TIMESTAMP -> field.invokeSetter(this, row.getTimestamp(columnNr));
				case TYPE_LOCALTIME -> field.invokeSetter(this, _JPAReadLocalDateTime(row, columnNr));
				case TYPE_CUSTOMTYPE -> field.invokeSetter(this, _JPAReadCustomType(row, field, columnNr));
				case TYPE_ENUM -> field.invokeSetter(this, _JPAReadENUM(field, row, columnNr));
				case TYPE_ORDINAL_ENUM -> field.invokeSetter(this, _JPAReadOrdinalENUM(field, row, columnNr));
				case TYPE_BYTES -> field.invokeSetter(this, row.getBytes(columnNr));
				case TYPE_OBJECT -> field.invokeSetter(this, row.getObject(columnNr));
				case TYPE_ENTITY -> {
					if (field.getMappingType() == MappingType.ONE_TO_ONE || field.getMappingType() == MappingType.MANY_TO_ONE || field.getMappingType() == MappingType.EMBEDDED) {
						field.invokeSetter(this, _JPAReadEntity(field, row, colPrefix, columnNr));
					}//if
				}//case
			}//switch
		}//try
		catch (SQLException ex) {
			throw new EntityMapException("Error setting field '" + field.getName() + "'", ex);
		}//catch
		finally {
			$$mapping = false;
		}//finally
	}//setField

	public void _mapResultSet(String colPrefix, ResultSet resultSet)
	{
		try {
			ResultSetMetaData resultMetaData = resultSet.getMetaData();
			int columns = resultMetaData.getColumnCount();

			Set<String> columnsProcessed = new HashSet<>();
			for (int i = 1; i <= columns; i++) {
				String column = resultMetaData.getColumnName(i);

				EntityField field = null;
				String nextColPrefix = null;
				if (colPrefix == null) {
					field = $$metadata.getEntityFieldByColumn(column);
				}//if
				else {
					if (column.length() <= colPrefix.length() || !column.startsWith(colPrefix)) {
						continue;
					}//if

					String fieldName = column.substring(colPrefix.length() + 1).split("-")[0];
					if (!fieldName.isEmpty() && !columnsProcessed.contains(fieldName)) {
						columnsProcessed.add(fieldName);
						field = $$metadata.getEntityFieldByNr(Integer.parseInt(fieldName));
						nextColPrefix = colPrefix + "-" + fieldName;
					}//if
				}//else

				if (field != null) {
					_JPAReadField(resultSet, field, nextColPrefix, i);
					_clearField(field.getName());
				}//if
			}//for
			$$lazyLoaded = false;
		}//try
		catch (Exception ex) {
			throw new EntityMapException("Error extracting the ResultSet Metadata", ex);
		}//catch
	}//_maresultSet

	private void writeObjects(ObjectOutput outStream) throws IOException
	{
		Collection<EntityField> fieldList = $$metadata.getEntityFields();
		outStream.writeShort(fieldList.size());
		for (EntityField field : fieldList) {
			Object value = field.invokeGetter(this);
			outStream.writeUTF(field.getName());
			outStream.writeObject(value);
		}//for
	}//writeObjects

	@Override
	public byte[] _serialize()
	{
		try {
			ByteArrayOutputStream recvOut = new ByteArrayOutputStream();
			ObjectOutputStream stream = new ObjectOutputStream(recvOut);
			writeObjects(stream);
			stream.flush();

			return recvOut.toByteArray();
		}//try
		catch (IOException ex) {
			throw new PersistenceException("Error serialising entity", ex);
		}//catch
	}//_serialise

	@SuppressWarnings("java:S112") // Throwable is correct
	private void readObjects(ObjectInput inputStream) throws Throwable
	{
		int nrItems = inputStream.readShort();
		while (nrItems > 0) {
			nrItems--;
			String fieldName = inputStream.readUTF();
			EntityField field = $$metadata.getEntityField(fieldName);
			field.invokeSetter(this, inputStream.readObject());
		}//while
	}//readObjects

	@Override
	public void _deserialize(byte[] bytes)
	{
		try {
			ByteArrayInputStream recvOut = new ByteArrayInputStream(bytes);
			ObjectInputStream stream = new ObjectInputStream(recvOut);
			readObjects(stream);

			_clearModified();
		}//try
		catch (Throwable ex) {
			throw new PersistenceException("Error de-serialising the entity", ex);
		}//catch
	}//_deserialize

	@Override
	public boolean _entityEquals(JPAEntity entity)
	{
		return (entity._getMetaData().getEntityClass().equals(_getMetaData().getEntityClass()) &&
				entity._getPrimaryKey().equals(_getPrimaryKey()));
	}

	@Override
	public String getProtoFileName()
	{
		return getClass().getSimpleName() + ".proto";
	}

	@Override
	public String getProtoFile() throws UncheckedIOException
	{
		return _getMetaData().getProtoFile();
	}

	@Override
	public void registerSchema(SerializationContext serCtx)
	{
		/*
		 * Register the marshals and schemas for converter classes if not already registered
		 */
		_getMetaData().getEntityFields().stream()
				.filter(f -> f.getFieldType() == FieldType.TYPE_CUSTOMTYPE &&
						f.getConverterClass().prototypeLib() != null &&
						!f.getConverterClass().prototypeLib().isBlank() &&
						!serCtx.canMarshall(f.getConverterClass().prototypeLib()))
				.forEach(f -> {
					f.getConverterClass().getSchema().registerSchema(serCtx);
					f.getConverterClass().getSchema().registerMarshallers(serCtx);
				});

		serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
	}

	@Override
	public void registerMarshallers(SerializationContext serCtx)
	{
		serCtx.registerMarshaller(new JPAEntityMarshaller<>(getClass()));
	}
}//JPAEntityImpl
