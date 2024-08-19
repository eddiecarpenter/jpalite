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

import io.smallrye.config.SmallRyeConfigProviderResolver;

import java.util.Optional;

public class JPAConfig
{
	public static String getValue(String propertyName, String defaultValue)
	{
		SmallRyeConfigProviderResolver config = new SmallRyeConfigProviderResolver();
		Optional<String> optionalValue = config.getConfig().getOptionalValue(propertyName, String.class);
		return optionalValue.orElse(defaultValue);
	}//getValue


	public static Boolean getValue(String propertyName, Boolean defaultValue)
	{
		SmallRyeConfigProviderResolver config = new SmallRyeConfigProviderResolver();
		Optional<Boolean> optionalValue = config.getConfig().getOptionalValue(propertyName, Boolean.class);
		return optionalValue.orElse(defaultValue);
	}//getValue


	public static Long getValue(String propertyName, Long defaultValue)
	{
		SmallRyeConfigProviderResolver config = new SmallRyeConfigProviderResolver();
		Optional<Long> optionalValue = config.getConfig().getOptionalValue(propertyName, Long.class);
		return optionalValue.orElse(defaultValue);
	}//getValue


	public static Integer getValue(String propertyName, Integer defaultValue)
	{
		SmallRyeConfigProviderResolver config = new SmallRyeConfigProviderResolver();
		Optional<Integer> optionalValue = config.getConfig().getOptionalValue(propertyName, Integer.class);
		return optionalValue.orElse(defaultValue);
	}//getValue

	private JPAConfig()
	{
		//Hide the constructor
	}
}
