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

package io.jpalite.extension;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Recorder
public class JPALiteRecorder
{
	private final Map<String, EntityManagerFactory> entityManagerFactoryList = new ConcurrentHashMap<>();

	@Inject
	TransactionManager transactionManager;

	@Inject
	TransactionSynchronizationRegistry transactionSynchronizationRegistry;

	public Function<SyntheticCreationalContext<EntityManagerFactory>, EntityManagerFactory> entityManagerFactorySupplier(String persistenceUnitName)
	{
		return context -> entityManagerFactoryList.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
	}

	public EntityManager getEntityManager(String persistenceUnit)
	{
		EntityManagerFactory factory = entityManagerFactoryList.computeIfAbsent(persistenceUnit, Persistence::createEntityManagerFactory);
		return new TransactionScopedEntityManagerImpl(factory,
													  transactionManager,
													  transactionSynchronizationRegistry);
	}//getEntityManager

	public Function<SyntheticCreationalContext<EntityManager>, EntityManager> entityManagerSupplier(String persistenceUnitName)
	{
		return context -> {
			TransactionManager transactionManager = context.getInjectedReference(TransactionManager.class);
			TransactionSynchronizationRegistry transactionSynchronizationRegistry = context.getInjectedReference(TransactionSynchronizationRegistry.class);
			EntityManagerFactory factory = entityManagerFactoryList.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
			return new TransactionScopedEntityManagerImpl(factory,
														  transactionManager,
														  transactionSynchronizationRegistry);
		};
	}
}
