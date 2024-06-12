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

package io.jpalite;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, FIELD, METHOD, PARAMETER, PACKAGE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface PersistenceUnit
{
	String value() default "<default>";


	@SuppressWarnings("ClassExplicitlyAnnotation")
	class PersistenceUnitLiteral extends AnnotationLiteral<PersistenceUnit> implements PersistenceUnit
	{

		private final String name;

		public PersistenceUnitLiteral(String name)
		{
			this.name = name;
		}

		@Override
		public String value()
		{
			return name;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof PersistenceUnitLiteral that)) return false;
			if (!super.equals(o)) return false;

			return Objects.equals(name, that.name);
		}

		@Override
		public int hashCode()
		{
			int result = super.hashCode();
			result = 31 * result + (name != null ? name.hashCode() : 0);
			return result;
		}
	}
}
