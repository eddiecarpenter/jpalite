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

package org.jpalite.extension.deployment;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jpalite.*;
import org.jpalite.extension.JPALiteConfigMapping;
import org.jpalite.extension.JPALiteRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class JPALiteExtensionProcessor
{
    private static final DotName ENTITY_MANAGER_FACTORY = DotName.createSimple("jakarta.persistence.EntityManagerFactory");
    public static final DotName ENTITY_MANAGER = DotName.createSimple("jakarta.persistence.EntityManager");
    private static final Logger LOG = LoggerFactory.getLogger(JPALiteExtensionProcessor.class);

    private static final String DEFAULT_NAME = "<default>";

    private static final String FEATURE = "jpalite-extension";

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem()
    {
        return new NativeImageResourceBuildItem("META-INF/services/org.jpalite.DataSourceProvider",
                                                "META-INF/services/org.jpalite.PersistenceUnitProvider",
                                                "META-INF/services/org.jpalite.FieldConvertType");
    }

    @BuildStep
    ReflectiveClassBuildItem reflection()
    {
        return ReflectiveClassBuildItem.builder(DataSourceProvider.class,
                                                PersistenceUnitProvider.class,
                                                FieldConvertType.class,
                                                EntityMetaDataManager.class)
                                       .build();
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void generateJPABeans(JPALiteRecorder recorder,
                          JPALiteConfigMapping jpaConfigMapping,
                          BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer)
    {
        if (jpaConfigMapping.defaultPersistenceUnit() != null) {
            //Define the default EntityManagerFactory producer
            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(DEFAULT_NAME,
                                                 false,
                                                 EntityManagerFactory.class,
                                                 List.of(ENTITY_MANAGER_FACTORY)
                            , true)
                                     .createWith(recorder.entityManagerFactorySupplier(DEFAULT_NAME))
                                     .done());

            //Define the default EntityManager producer
            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(DEFAULT_NAME,
                                                 false,
                                                 EntityManager.class,
                                                 List.of(ENTITY_MANAGER)
                            , true)
                                     .createWith(recorder.entityManagerSupplier(DEFAULT_NAME))
                                     .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionManager.class)))
                                     .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSynchronizationRegistry.class)))
                                     .done());
        }//if


        if (jpaConfigMapping.namedPersistenceUnits() != null) {
            for (String unitName : jpaConfigMapping.namedPersistenceUnits().keySet()) {
                //Define the named EntityManagerFactory producer
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(unitName,
                                                     true,
                                                     EntityManagerFactory.class,
                                                     List.of(ENTITY_MANAGER_FACTORY)
                                , false)
                                         .createWith(recorder.entityManagerFactorySupplier(unitName))
                                         .done());

                //Define the named EntityManager producer
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(unitName,
                                                     true,
                                                     EntityManager.class,
                                                     List.of(ENTITY_MANAGER)
                                , false)
                                         .createWith(recorder.entityManagerSupplier(unitName))
                                         .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionManager.class)))
                                         .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSynchronizationRegistry.class)))
                                         .done());

            }
        }//if
    }

    private static <T> SyntheticBeanBuildItem.ExtendedBeanConfigurator createSyntheticBean(String persistenceUnitName,
                                                                                           boolean isNamedPersistenceUnit,
                                                                                           Class<T> type,
                                                                                           List<DotName> allExposedTypes,
                                                                                           boolean defaultBean)
    {
        LOG.info("Creating Synthetic Bean for {}(\"{}\")", type.getSimpleName(), persistenceUnitName);
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit();

        for (DotName exposedType : allExposedTypes) {
            configurator.addType(exposedType);
        }

        if (defaultBean) {
            configurator.defaultBean();
        }

        if (isNamedPersistenceUnit) {
            configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", persistenceUnitName).done();
        } else {
            configurator.addQualifier(Default.class);
            configurator.addQualifier().annotation(PersistenceUnit.class).done();
        }

        return configurator;
    }
}
