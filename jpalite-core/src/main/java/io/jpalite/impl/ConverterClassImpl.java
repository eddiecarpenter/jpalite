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

import io.jpalite.ConverterClass;
import io.jpalite.TradeSwitchConvert;
import jakarta.persistence.Converter;
import jakarta.persistence.PersistenceException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Data
public class ConverterClassImpl implements ConverterClass
{
	private static final Logger LOG = LoggerFactory.getLogger(ConverterClassImpl.class);
	private Class<?> convertClass;
	private boolean autoApply;
	private TradeSwitchConvert<?, ?> converter;
	private Class<?> attributeType;
	private Class<?> dbType;

	public ConverterClassImpl(Class<?> convertClass)
	{
		this.convertClass = convertClass;
		Converter converter = this.convertClass.getAnnotation(Converter.class);
		if (converter == null) {
			LOG.warn("Missing @Converter annotation on {}", this.convertClass.getSimpleName());
			autoApply = false;
		}//if
		else {
			autoApply = converter.autoApply();
		}//else

		for (Method method : this.convertClass.getDeclaredMethods()) {
			//Not a SYNTHETIC (generated method)
			if (method.getName().equals("convertToDatabaseColumn") &&
					((method.getModifiers() & 0x00001000) != 0x00001000) &&
					method.getParameterTypes().length == 1) {
				attributeType = method.getParameterTypes()[0];
				dbType = method.getReturnType();
				break;
			}//if
		}//for
		if (attributeType == null) {
			LOG.warn("Error detecting the attribute type in {}", this.convertClass.getSimpleName());
			attributeType = Object.class;
			dbType = Object.class;
		}//if

		try {
			this.converter = (TradeSwitchConvert<?, ?>) this.convertClass.getConstructor().newInstance();
		}//try
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
			throw new PersistenceException(this.convertClass.getSimpleName() + " failed to instantiate", ex);
		}//catch
	}//ConverterClass

	@Override
	public String getName()
	{
		return convertClass.getCanonicalName();
	}
}//ConverterClass
