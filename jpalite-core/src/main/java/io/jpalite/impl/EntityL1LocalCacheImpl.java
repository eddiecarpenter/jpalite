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

import io.jpalite.EntityLocalCache;
import io.jpalite.EntityState;
import io.jpalite.JPAEntity;
import io.jpalite.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class EntityL1LocalCacheImpl implements EntityLocalCache
{
	private final PersistenceContext persistenceContext;

	/**
	 * All the entity attached to this persistence context
	 */
	private final List<JPAEntity> cache = new ArrayList<>();

	public EntityL1LocalCacheImpl(PersistenceContext pPersistenceContext)
	{
		persistenceContext = pPersistenceContext;
		LOG.trace("Creating L1 cache for {}", pPersistenceContext);
	}//EntityCacheImpl

	@Override
	public void clear()
	{
		cache.forEach(e -> e._setEntityState(EntityState.DETACHED));
		cache.clear();
		LOG.trace("Clearing L1 cache for {}", persistenceContext);
	}//clear

	@Override
	public <T> T find(Class<T> entityType, Object primaryKey)
	{
		return find(entityType, primaryKey, false);
	}

	@SuppressWarnings("unchecked") //We are doing a valid casting
	public <T> T find(Class<T> entityType, Object primaryKey, boolean checkIfRemoved)
	{
		if (persistenceContext.isActive() && primaryKey != null) {
			JPAEntity entity = cache.stream()
					.filter(e -> e._getMetaData().getName().equals(entityType.getName()))
					.filter(e -> primaryKey.equals(e._getField(e._getMetaData().getIdField().getName())))
					.findFirst()
					.orElse(null);
			if (entity != null) {
				if (entity._getEntityState() == EntityState.REMOVED) {
					if (checkIfRemoved) {
						throw new IllegalArgumentException("Entity has been removed");
					}//if
					return null;
				}//if

				return (T) entity;
			}//if
		}//if

		return null;
	}//find

	@Override
	@SuppressWarnings("unchecked") //We are doing a valid casting
	public <T> void foreachType(Class<T> entityType, Consumer<T> action)
	{
		if (persistenceContext.isActive()) {
			cache.stream()
					.filter(e -> e._getMetaData().getName().equals(entityType.getName()))
					.forEach(e -> action.accept((T) e));
		}//if
	}//foreachType

	@Override
	public void manage(JPAEntity entity)
	{
		entity._setPersistenceContext(persistenceContext);

		//We only manage entities if we are in a transaction and if the entity type is supported by the persistence context
		if (persistenceContext.isActive() && persistenceContext.supportedEntityType(entity._getMetaData().getEntityType())) {
			cache.add(entity);
			entity._setEntityState(EntityState.MANAGED);
			LOG.trace("Adding Entity to L1 cache. Context [{}], Entity [{}]", persistenceContext, entity);
		}//if
		else {
			entity._setEntityState(EntityState.DETACHED);
		}//else

	}//manage

	@Override
	public void detach(JPAEntity entity)
	{
		for (JPAEntity cachedEntity : cache) {
			if (cachedEntity == entity) {
				LOG.trace("Removing Entity from L1 cache. Context [{}], Entity [{}]", persistenceContext, entity);
				cache.remove(cachedEntity);
				break;
			}//if
		}//for

		LOG.trace("Detaching Entity from L1 cache. Entity [{}]", entity);
		if (entity._getEntityState() != EntityState.TRANSIENT) {
			entity._setEntityState(EntityState.DETACHED);
		}//if
	}//remove

	@Override
	public boolean contains(JPAEntity entity)
	{
		return (entity._getEntityState() == EntityState.MANAGED && persistenceContext == entity._getPersistenceContext());
	}

	@Override
	public void foreach(Consumer<Object> action)
	{
		cache.forEach(action);
	}
}
