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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jpalite.EntityMetaDataManager;
import org.jpalite.PersistenceUnit;
import org.jpalite.agroal.AgroalDataSourceProvider;
import org.jpalite.extension.JPALiteConfigMapping;
import org.jpalite.extension.JPALiteRecorder;
import org.jpalite.extension.PropertyPersistenceUnitProvider;
import org.jpalite.impl.fieldtypes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class JPALiteExtensionProcessor
{
    private static final DotName ENTITY_MANAGER_FACTORY = DotName.createSimple("jakarta.persistence.EntityManagerFactory");
    public static final DotName ENTITY_MANAGER = DotName.createSimple("jakarta.persistence.EntityManager");
    public static final DotName PERSISTENCE_UNIT_CONFIG = DotName.createSimple("org.jpalite.extension.PersistenceUnitConfig");

    private static final Logger LOG = LoggerFactory.getLogger(JPALiteExtensionProcessor.class);

    private static final String FEATURE = "jpalite";

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
        return ReflectiveClassBuildItem.builder(AgroalDataSourceProvider.class,
                                                PropertyPersistenceUnitProvider.class,
                                                EntityMetaDataManager.class,
                                                BooleanFieldType.class,
                                                IntegerFieldType.class,
                                                LongLongFieldType.class,
                                                DoubleDoubleFieldType.class,
                                                BoolFieldType.class,
                                                IntFieldType.class,
                                                LongFieldType.class,
                                                DoubleFieldType.class,
                                                StringFieldType.class,
                                                BytesFieldType.class,
                                                TimestampFieldType.class,
                                                LocalDateTimeFieldType.class,
                                                BigDecimalFieldType.class)
                                       .methods().fields().constructors()
                                       .build();
    }

    @BuildStep
    public void discoverInjectedClients(CombinedIndexBuildItem index,
                                        BuildProducer<PersistenceUnitBuildItem> persistenceUnits)
    {
        Set<String> persistenceUnitNames = new HashSet<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(ENTITY_MANAGER)) {
            if (annotation.value() == null) {
                persistenceUnitNames.add(JPALiteConfigMapping.DEFAULT_PERSISTENCE_UNIT_NAME);
            } else {
                persistenceUnitNames.add((String) annotation.value().value());
            }
        }

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(ENTITY_MANAGER_FACTORY)) {
            if (annotation.value() == null) {
                persistenceUnitNames.add(JPALiteConfigMapping.DEFAULT_PERSISTENCE_UNIT_NAME);
            } else {
                persistenceUnitNames.add((String) annotation.value().value());
            }
        }

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(PERSISTENCE_UNIT_CONFIG)) {
            if (annotation.value() == null) {
                persistenceUnitNames.add(JPALiteConfigMapping.DEFAULT_PERSISTENCE_UNIT_NAME);
            } else {
                persistenceUnitNames.add((String) annotation.value().value());
            }
        }

        persistenceUnitNames.forEach(n -> persistenceUnits.produce(new PersistenceUnitBuildItem(n)));
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void generateJPABeans(JPALiteRecorder recorder,
                          JPALiteConfigMapping jpaConfigMapping,
                          List<PersistenceUnitBuildItem> persistenceUnits,
                          BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer)
    {
        persistenceUnits.forEach(p -> {
            syntheticBeanBuildItemBuildProducer.produce(
                    createSyntheticBean(p.getPersistenceUnitName(),
                                        EntityManagerFactory.class,
                                        ENTITY_MANAGER_FACTORY,
                                        p.getPersistenceUnitName().equals(JPALiteConfigMapping.DEFAULT_PERSISTENCE_UNIT_NAME))
                            .createWith(recorder.entityManagerFactorySupplier(p.getPersistenceUnitName()))
                            .done());

            syntheticBeanBuildItemBuildProducer.produce(
                    createSyntheticBean(p.getPersistenceUnitName(),
                                        EntityManager.class,
                                        ENTITY_MANAGER,
                                        p.getPersistenceUnitName().equals(JPALiteConfigMapping.DEFAULT_PERSISTENCE_UNIT_NAME))
                            .createWith(recorder.entityManagerSupplier(p.getPersistenceUnitName()))
                            .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionManager.class)))
                            .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSynchronizationRegistry.class)))
                            .done());

        });
    }

    private static <T> SyntheticBeanBuildItem.ExtendedBeanConfigurator createSyntheticBean(String persistenceUnitName,
                                                                                           Class<T> type,
                                                                                           DotName exposedType,
                                                                                           boolean isDefaultConfig)
    {
        LOG.info("Creating Synthetic Bean for {}(\"{}\")", type.getSimpleName(), persistenceUnitName);
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit()
                .addType(exposedType);

        if (isDefaultConfig) {
            configurator.defaultBean();
            configurator.addQualifier(Default.class)
                        .addQualifier(PersistenceUnit.class);
        } else {
            configurator.addQualifier()
                        .annotation(PersistenceUnit.class)
                        .addValue("value", persistenceUnitName)
                        .done();
        }

        return configurator;
    }
}
