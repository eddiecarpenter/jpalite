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

package org.jpalite.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.jpalite.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("java:S3740")
@Slf4j
public class EntityMetaDataImpl<T> implements EntityMetaData<T>
{
    private final String entityName;
    private final EntityLifecycle lifecycleListeners;
    private final Class<T> entityClass;
    private final boolean legacyEntity;

    private final boolean cacheable;
    private long idleTime = 1;
    private TimeUnit cacheTimeUnit = TimeUnit.DAYS;

    private final String columns;
    private final String table;
    private EntityType entityType;

    private EntityMetaData<?> primaryKey;
    private final List<EntityField> idFields;
    private final Map<String, EntityField> entityFields;
    private EntityField versionField;


    @SuppressWarnings({"rawtypes", "unchecked"})
    public EntityMetaDataImpl(Class<T> entityClass)
    {
        entityType   = EntityType.ENTITY;
        entityFields = new LinkedHashMap<>();
        idFields     = new ArrayList<>();

        this.entityClass = entityClass;

        Entity entity = entityClass.getAnnotation(Entity.class);

        legacyEntity = (entity == null);
        if (entity != null && !entity.name().isEmpty()) {
            entityName = entity.name();
        }//if
        else {
            entityName = entityClass.getSimpleName();
        }//else

        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            this.table = tableAnnotation.name();
        }//if
        else {
            this.table = entityName;
        }//else

        Embeddable embeddable = entityClass.getAnnotation(Embeddable.class);
        if (embeddable != null) {
            entityType = EntityType.EMBEDDABLE;
        }//if

        Cacheable cacheableAnnotation = entityClass.getAnnotation(Cacheable.class);
        if (cacheableAnnotation != null) {
            this.cacheable = cacheableAnnotation.value();
            Caching vCaching = entityClass.getAnnotation(Caching.class);
            if (vCaching != null) {
                idleTime      = vCaching.idleTime();
                cacheTimeUnit = vCaching.unit();
            }//if
        }//if
        else {
            this.cacheable = false;
        }//else

        IdClass idClass = entityClass.getAnnotation(IdClass.class);
        if (idClass != null) {
            if (!EntityMetaDataManager.isRegistered(idClass.value())) {
                //TODO: Added support for @EmbeddedId and fix implementation of @IdClass
                primaryKey                                      = new EntityMetaDataImpl<>(idClass.value());
                ((EntityMetaDataImpl<?>) primaryKey).entityType = EntityType.ID_CLASS;
                EntityMetaDataManager.register(primaryKey);
            }//if

            if (primaryKey.getEntityType() != EntityType.ID_CLASS) {
                throw new IllegalArgumentException("Illegal IdClass specified. [" + idClass.value() + "] is already registered as an entity of type [" + primaryKey.getEntityType() + "]");
            }//if
        }//if

        versionField = null;
        StringBuilder stringBuilder = new StringBuilder();
        for (Field vField : entityClass.getDeclaredFields()) {
            if (!Modifier.isStatic(vField.getModifiers()) &&
                !Modifier.isFinal(vField.getModifiers()) &&
                !Modifier.isTransient(vField.getModifiers()) &&
                !vField.isAnnotationPresent(Transient.class)) {
                processEntityField(vField, stringBuilder);
            }//if
        }//for

        if (idFields.isEmpty()) {
            LOG.warn("Developer Warning - Entity [{}] have no ID Fields defined . This needs to be fixed as not having ID fields is not allowed!", entityName);
        }//if

        //if
        if (primaryKey == null && idFields.size() > 1) {
            throw new IllegalArgumentException("Missing @IdClass definition for Entity. @IdClass definition is required if you have more than one ID field");
        }//if

        lifecycleListeners = new EntityLifecycleImpl(entityClass);

        if (stringBuilder.length() > 1) {
            columns = stringBuilder.substring(1);
        }//if
        else {
            columns = "";
        }//else
    }//EntityMetaDataImpl

    private void processEntityField(Field field, StringBuilder stringBuilder)
    {
        EntityField entityField = new EntityFieldImpl(entityClass, field, entityFields.size() + 1);

        if (entityField.getMappingType() == MappingType.BASIC) {
            if (entityField.getColumn() == null) {
                return;
            }//if

            if (!entityField.getColumnDefinition().isEmpty() && !entityField.getTable().isEmpty()) {
                stringBuilder.append(",");
                stringBuilder.append(entityField.getTable()).append(".");
                stringBuilder.append(entityField.getColumnDefinition()).append(" ").append(entityField.getColumn());
            }//if
            else {
                //Ignore columns that have a '-' in the column definition
                if (!"-".equals(entityField.getColumnDefinition())) {
                    stringBuilder.append(",");
                    stringBuilder.append(entityField.getColumn());
                }//if
            }//else

            if (entityField.isIdField()) {
                idFields.add(entityField);
            }//if

            if (entityField.isVersionField()) {
                versionField = entityField;
            }//if
        }//if
        else {
            //JoinColumn is not required (or used) if getMappedBy is provided
            if (entityField.getMappingType() != MappingType.EMBEDDED && entityField.getMappedBy() == null && entityField.getColumn() == null) {
                return;
            }//if
        }//if

        entityFields.put(entityField.getName(), entityField);
    }//processEntityField

    @Override
    public String toString()
    {
        String primKeyClass;
        if (primaryKey == null) {
            if (getIdField() != null) {
                primKeyClass = getIdField().getType().getName();
            }//if
            else {
                primKeyClass = "N/A";
            }//else
        }//if
        else {
            primKeyClass = primaryKey.getEntityClass().getName();
        }//else
        return "[" + entityName + "] Metadata -> Type:" + entityType + ", Entity Class:" + entityClass.getName() + ", Primary Key Class:" + primKeyClass;
    }//toString

    @Override
    public EntityType getEntityType()
    {
        return entityType;
    }//getEntityType

    @Override
    public String getName()
    {
        return entityName;
    }//getName

    @Override
    public boolean isCacheable()
    {
        return cacheable;
    }//isCacheable

    /**
     * The time the entity is to remain in cache before expiring it. Only used if cacheable is true
     *
     * @return The idle time setting
     */
    public long getIdleTime()
    {
        return idleTime;
    }

    /**
     * The TimeUnit the idle time is expressed in
     *
     * @return The time units
     */
    public TimeUnit getCacheTimeUnit()
    {
        return cacheTimeUnit;
    }

    @Nonnull
    @Override
    public T getNewEntity()
    {
        try {
            return entityClass.getConstructor().newInstance();
        }//try
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ex) {
            throw new EntityMapException("Error instantiating instance of " + entityClass.getSimpleName());
        }//catch
    }//getNewEntity

    @Override
    public Class<T> getEntityClass()
    {
        return entityClass;
    }//getEntityClass

    @Override
    public EntityLifecycle getLifecycleListeners()
    {
        return lifecycleListeners;
    }//getLifecycleListeners

    @Override
    public String getTable()
    {
        return table;
    }//getTable

    @Override
    @Nonnull
    public EntityField getEntityField(String fieldName)
    {
        EntityField entityField = entityFields.get(fieldName);
        if (entityField == null) {
            throw new EntityNotFoundException(fieldName + " is not defined as a field in entity " + this.entityName);
        }//if

        return entityField;
    }//getEntityField

    @Override
    public boolean isEntityField(String fieldName)
    {
        return entityFields.containsKey(fieldName);
    }//isEntityField

    @Nullable
    public EntityField getEntityFieldByColumn(String column)
    {
        for (EntityField field : entityFields.values()) {
            if (column.equalsIgnoreCase(field.getColumn())) {
                return field;
            }//if
        }//for

        return null;
    }//getEntityFieldByColumn

    @Override
    @Nonnull
    public EntityField getEntityFieldByNr(int fieldNr)
    {
        Optional<EntityField> entityField = entityFields.values()
                                                        .stream()
                                                        .filter(f -> f.getFieldNr() == fieldNr)
                                                        .findFirst();
        if (entityField.isEmpty()) {
            throw new EntityNotFoundException("There is no entity field with a fields number of " + fieldNr + " in entity " + this.entityName);
        }//if

        return entityField.get();
    }//getEntityFieldByNr

    @Override
    public Collection<EntityField> getEntityFields()
    {
        return entityFields.values();
    }//getEntityFields

    @Override
    public boolean hasMultipleIdFields()
    {
        return false;
    }//hasMultipleIdFields

    @Override
    public EntityField getIdField()
    {
        if (hasMultipleIdFields()) {
            throw new IllegalArgumentException("Multiple id fields exists");
        }//if

        if (idFields.isEmpty()) {
            return null;
        }//if

        return idFields.getFirst();
    }//getIdField

    @Override
    public boolean hasVersionField()
    {
        return versionField != null;
    }//hasVersionField

    @Override
    public EntityField getVersionField()
    {
        if (versionField == null) {
            throw new IllegalArgumentException("The entity does not have a version field");
        }//if

        return versionField;
    }//getVersionField

    @Override
    @Nullable
    public EntityMetaData<?> getPrimaryKeyMetaData()
    {
        return primaryKey;
    }//getIPrimaryKeyMetaData


    @Override
    @Nonnull
    public List<EntityField> getIdFields()
    {
        return idFields;
    }//getIdFields

    @Override
    @Deprecated
    public boolean isLegacyEntity()
    {
        return legacyEntity;
    }

    @Override
    @Deprecated
    public String getColumns()
    {
        return columns;
    }//getColumns
}//EntityMetaDataImpl
