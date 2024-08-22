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

package org.jpalite.extension;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
@Recorder
public class JPALiteRecorder
{
    public Function<SyntheticCreationalContext<EntityManagerFactory>, EntityManagerFactory> entityManagerFactorySupplier(String persistenceUnitName)
    {
        return context -> {
            EntityManagerProducer producer = context.getInjectedReference(EntityManagerProducer.class);
            return producer.getEntityManagerFactory(persistenceUnitName);
        };
    }

    public Function<SyntheticCreationalContext<EntityManager>, EntityManager> entityManagerSupplier(String persistenceUnitName)
    {
        return context -> {
            EntityManagerProducer producer = context.getInjectedReference(EntityManagerProducer.class);
            return producer.getEntityManager(persistenceUnitName);
        };
    }
}
