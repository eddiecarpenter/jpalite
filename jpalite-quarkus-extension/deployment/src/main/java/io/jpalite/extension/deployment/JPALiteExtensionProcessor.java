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

package io.jpalite.extension.deployment;

import io.jpalite.agroal.AgroalDataSourceProvider;
import io.jpalite.extension.PropertyPersistenceUnitProvider;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class JPALiteExtensionProcessor
{

	private static final String FEATURE = "jpalite-extension";

	@BuildStep
	FeatureBuildItem feature()
	{
		return new FeatureBuildItem(FEATURE);
	}

	@BuildStep
	NativeImageResourceBuildItem nativeImageResourceBuildItem()
	{
		return new NativeImageResourceBuildItem("META-INF/services/io.jpalite.DataSourceProvider",
												"META-INF/services/io.jpalite.PersistenceUnitProvider",
												"META-INF/services/io.jpalite.FieldConvertType");
	}

	@BuildStep
	ReflectiveClassBuildItem reflection()
	{
		return ReflectiveClassBuildItem.builder(AgroalDataSourceProvider.class,
												PropertyPersistenceUnitProvider.class)
				.build();
	}
}
