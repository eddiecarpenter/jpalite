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
import io.jpalite.FieldConvertType;
import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ConverterClassImpl implements ConverterClass
{
	private final boolean autoApply;
	private final FieldConvertType<?, ?> converter;
	private final Class<?> attributeType;


	public ConverterClassImpl(FieldConvertType<?, ?> converter)
	{
		this.converter = converter;

		Converter converterAnnotation = this.converter.getClass().getAnnotation(Converter.class);
		if (converterAnnotation == null) {
			autoApply = false;
		}//if
		else {
			autoApply = converterAnnotation.autoApply();
		}//else

		attributeType = converter.getAttributeType();
	}//ConverterClass

	@Override
	public String getName()
	{
		return converter.getClass().getCanonicalName();
	}
}//ConverterClass
