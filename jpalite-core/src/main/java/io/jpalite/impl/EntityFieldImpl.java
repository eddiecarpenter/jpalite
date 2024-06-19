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

import io.jpalite.*;
import io.jpalite.impl.fieldtypes.EnumFieldType;
import io.jpalite.impl.fieldtypes.ObjectFieldType;
import io.jpalite.impl.fieldtypes.OrdinalEnumFieldType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.nativeimage.ImageInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static jakarta.persistence.GenerationType.AUTO;
import static jakarta.persistence.GenerationType.SEQUENCE;

@Data
@Slf4j
@SuppressWarnings({"unchecked", "java:S3740"})//Cannot use generics here
public class EntityFieldImpl implements EntityField
{
	private static final boolean NATIVE_IMAGE = ImageInfo.inImageCode();
	/**
	 * The entity class
	 */
	private final Class<?> enityClass;
	/**
	 * Identifier of the entity
	 */
	private final String name;
	/**
	 * A unique field number assigned to the field.
	 */
	private final int fieldNr;
	/**
	 * The java class of the field
	 */
	private Class<?> type;
	/**
	 * Set to true if the field points to an entity
	 */
	private boolean entityField;
	/**
	 * The SQL column linked to the field
	 */
	private String column;
	/**
	 * The mapping type specified by the field. See {@link MappingType}.
	 */
	private MappingType mappingType;
	/**
	 * True if the field is to be unique in the table
	 */
	private boolean unique;
	/**
	 * True of the field can be null
	 */
	private boolean nullable;
	/**
	 * True if the field is insertable
	 */
	private boolean insertable;
	/**
	 * True if the field is updatable.
	 */
	private boolean updatable;
	/**
	 * True if the field is an ID Field
	 */
	private boolean idField;
	/**
	 * True if the field is a Version Field
	 */
	private boolean versionField;
	/**
	 * The getter for the field
	 */
	private MethodHandle getter;
	/**
	 * The getter reflection method for the field
	 */
	private Method getterMethod;
	/**
	 * The setter for the field
	 */
	private MethodHandle setter;
	/**
	 * The setter reflection method for the field
	 */
	private Method setterMethod;
	/**
	 * The {@link CascadeType} assigned to the field.
	 */
	private Set<CascadeType> cascade;
	/**
	 * The {@link FetchType} assigned to the field.
	 */
	private FetchType fetchType;
	/**
	 * Only applicable to non-Basic fields and indicates that the field is linked the field specified in mappedBy in the
	 * entity represented by the field.
	 */
	private String mappedBy;
	/**
	 * The columnDefinition value defined in the JoinColumn annotation linked to the field
	 */
	private String columnDefinition;
	/**
	 * The table value defined in the JoinColumn annotation linked to the field
	 */
	private String table;
	/**
	 * The converter class used to convert the field to a SQL type
	 */
	private FieldConvertType<?, ?> converter;

	/**
	 * Create a new entity field definition
	 *
	 * @param field   The field
	 * @param fieldNr The field number
	 */
	public EntityFieldImpl(Class<?> enitityClass, Field field, int fieldNr)
	{
		type = field.getType();
		if (!Map.class.isAssignableFrom(type) && field.getGenericType() instanceof ParameterizedType) {
			type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		}//if

		enityClass = enitityClass;
		name = field.getName();
		this.fieldNr = fieldNr;
		entityField = (JPAEntity.class.isAssignableFrom(type));
		mappingType = MappingType.BASIC;
		unique = false;
		nullable = true;
		insertable = true;
		updatable = true;
		fetchType = FetchType.EAGER;
		cascade = new HashSet<>();
		mappedBy = null;
		columnDefinition = null;
		table = null;
		idField = false;
		versionField = false;

		//The order below is important
		processMappingType(field);

		findConverter(field);

		findGetterSetter(field);
	}//EntityField

	private void findGetterSetter(Field field)
	{
		String vMethod = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		String reflectionMethod = null;
		try {
			reflectionMethod = "set" + vMethod;
			setterMethod = enityClass.getMethod(reflectionMethod, field.getType());
			setter = lookup.unreflect(setterMethod);

			reflectionMethod = ((field.getType() == Boolean.class || field.getType() == boolean.class) ? "is" : "get") + vMethod;
			getterMethod = enityClass.getMethod(reflectionMethod);
			getter = lookup.unreflect(getterMethod);
		}//try
		catch (IllegalAccessException | NoSuchMethodException | SecurityException ex) {
			/*
			 * Special case for Boolean that could be either isXXX or
			 * getXXXX
			 */
			if (field.getType() == Boolean.class || field.getType() == boolean.class) {
				try {
					reflectionMethod = "get" + vMethod;
					getterMethod = enityClass.getMethod(reflectionMethod);
					getter = lookup.unreflect(getterMethod);
				}//try
				catch (IllegalAccessException | NoSuchMethodException | SecurityException ex1) {
					throw new IllegalCallerException(String.format("Error finding %s::%s", enityClass.getSimpleName(), reflectionMethod), ex);
				}//catch
			}//if
			else {
				throw new IllegalCallerException(String.format("Error finding %s::%s", enityClass.getSimpleName(), reflectionMethod), ex);
			}//else
		}//catch
	}//findGetterSetter

	private void processMappingType(Field pField)
	{
		if (checkEmbeddedField(pField) || checkOneToOneField(pField) ||
				checkOneToManyField(pField) || checkManyToOneField(pField) ||
				checkManyToManyField(pField)) {
			JoinColumn joinColumn = pField.getAnnotation(JoinColumn.class);
			if (joinColumn != null) {
				setInsertable(joinColumn.insertable());
				setNullable(joinColumn.nullable());
				setUnique(joinColumn.unique());
				setUpdatable(joinColumn.updatable());
				setColumn(joinColumn.name());
			}//if
		}//if
		else {
			prosesBasicField(pField);
		}//if
	}//processMappingType

	private void prosesBasicField(Field field)
	{
		Basic basic = field.getAnnotation(Basic.class);
		if (basic != null) {
			if (isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is referencing an Entity type and cannot be annotated with @Basic.");
			}//if
			setFetchType(basic.fetch());
			setNullable(basic.optional());
		}//if

		Column col = field.getAnnotation(Column.class);
		if (col != null) {
			setColumn(col.name());
			setInsertable(col.insertable());
			setNullable(col.nullable());
			setUnique(col.unique());
			setUpdatable(col.updatable());
			setTable(col.table());
			setColumnDefinition(col.columnDefinition());
		}//if

		setIdField((field.getAnnotation(Id.class) != null));
		if (isIdField()) {
			GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
			if (generatedValue != null) {
				if (generatedValue.strategy() != AUTO && generatedValue.strategy() != SEQUENCE) {
					throw new PersistenceException(enityClass.getName() + "::" + getName() + "@GeneratedValue is not AUTO or SEQUENCE");
				}//if
				insertable = false;
				updatable = false;
			}//if
			nullable = false;
		}//if

		setVersionField(field.getAnnotation(Version.class) != null);
	}//prosesBasicField

	private boolean checkEmbeddedField(Field field)
	{
		Embedded embedded = field.getAnnotation(Embedded.class);
		if (embedded != null) {
			if (!isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is NOT referencing an Entity type and cannot NOT be annotated with @Embedded.");
			}//if

			setMappingType(MappingType.EMBEDDED);
			return true;
		}//if
		return false;
	}//checkEmbeddedField

	private boolean checkOneToOneField(Field field)
	{
		OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		if (oneToOne != null) {
			if (!isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is NOT referencing an Entity type and cannot NOT be annotated with @OneToOne.");
			}//if
			setMappingType(MappingType.ONE_TO_ONE);
			setFetchType(oneToOne.fetch());
			setCascade(new HashSet<>(Arrays.asList(oneToOne.cascade())));
			setMappedBy(oneToOne.mappedBy());
			return true;
		}//if
		return false;
	}//checkOneToOneField

	private boolean checkOneToManyField(Field field)
	{
		OneToMany oneToMany = field.getAnnotation(OneToMany.class);
		if (oneToMany != null) {
			if (!isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is NOT referencing an Entity type and cannot NOT be annotated with @OneToMany.");
			}//if

			setMappingType(MappingType.ONE_TO_MANY);
			setFetchType(oneToMany.fetch());
			setCascade(new HashSet<>(Arrays.asList(oneToMany.cascade())));
			setMappedBy(oneToMany.mappedBy());
			return true;
		}//if
		return false;
	}//checkOneToManyField

	private boolean checkManyToOneField(Field field)
	{
		ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		if (manyToOne != null) {
			if (!isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is NOT referencing an Entity type and cannot NOT be annotated with @ManyToOne.");
			}//if

			setMappingType(MappingType.MANY_TO_ONE);
			setFetchType(manyToOne.fetch());
			setCascade(new HashSet<>(Arrays.asList(manyToOne.cascade())));
			return true;
		}//if
		return false;
	}//checkManyToOneField

	private boolean checkManyToManyField(Field field)
	{
		ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
		if (manyToMany != null) {
			if (!isEntityField()) {
				throw new PersistenceException(enityClass.getName() + "::" + getName() + " is NOT referencing an Entity type and cannot NOT be annotated with @ManyToMany.");
			}//if

			setMappingType(MappingType.MANY_TO_MANY);
			setFetchType(manyToMany.fetch());
			setCascade(new HashSet<>(Arrays.asList(manyToMany.cascade())));
			setMappedBy(manyToMany.mappedBy());
			return true;
		}//if
		return false;
	}//checkManyToManyField

	private void findConverter(Field field)
	{
		Convert customType = field.getAnnotation(Convert.class);
		if (customType != null) {
			try {
				//Check if the converter class was explicitly overridden
				if (customType.converter() != null) {
					converter = (FieldConvertType<?, ?>) customType.converter().getConstructor().newInstance();
					return;
				}//if
			}//try
			catch (InvocationTargetException | InstantiationException | IllegalAccessException |
				   NoSuchMethodException ex) {
				throw new IllegalArgumentException(getName() + "::" + field.getName() + " failed to instantiate the referenced converter", ex);
			}//catch

			//If conversion is not required, exit here
			if (customType.disableConversion()) {
				return;
			}//if
		}//if

		ConverterClass converterClass = EntityMetaDataManager.getConvertClass(type);
		if (converterClass != null) {
			converter = converterClass.getConverter();
		}//if
		else {
			if (type.isAssignableFrom(Enum.class)) {
				Enumerated enumField = field.getAnnotation(Enumerated.class);
				if (enumField == null) {
					LOG.warn("{}: Field '{}' is not annotated as an enum, assuming it to be one - Developers must fix this", enityClass.getName(), field.getName());
					converter = new EnumFieldType((Class<Enum<?>>) type);
				}//if
				else {
					if (isEntityField()) {
						throw new PersistenceException(enityClass.getName() + "::" + getName() + " is referencing an Entity type and cannot be annotated with @Enumerated.");
					}//if

					converter = (enumField.value() == EnumType.ORDINAL ? new OrdinalEnumFieldType((Class<Enum<?>>) type) : new EnumFieldType((Class<Enum<?>>) type));
				}//if
			}
			else {
				if (!isEntityField()) {
					converter = new ObjectFieldType();
				}
			}
		}
	}//checkForConvert

	@Override
	public Object invokeGetter(Object entity)
	{
		try {
			if (getter == null) {
				throw new PersistenceException("No getter method found for " + enityClass.getName() + "::" + getName());
			}//if

			return NATIVE_IMAGE ? getterMethod.invoke(entity) : getter.invoke(entity);
		}//try
		catch (Throwable ex) {
			throw new PersistenceException("Failed to invoke getter for " + enityClass.getName() + "::" + getName(), ex);
		}//catch
	}//invokeGetter


	@Override
	public void invokeSetter(Object entity, Object value)
	{
		try {
			if (setter == null) {
				throw new PersistenceException("No setter method found for " + enityClass.getName() + "::" + getName());
			}//if

			if (NATIVE_IMAGE) {
				setterMethod.invoke(entity, value);
			}//if
			else {
				setter.invoke(entity, value);
			}//else
		}//try
		catch (Throwable ex) {
			throw new PersistenceException("Failed to invoke setter for " + enityClass.getName() + "::" + getName(), ex);
		}//catch
	}//invokeSetter
}//EntityFieldImpl
