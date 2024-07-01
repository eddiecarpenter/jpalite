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

package io.jpalite.impl.queries;

import jakarta.persistence.Parameter;

public class QueryParameterImpl<T> implements Parameter<T>
{
	private Integer position;
	private final String name;
	private final Class<T> parameterType;
	private T value;
	private boolean bounded = false;

	public QueryParameterImpl(Integer position, Class<T> parameterType)
	{
		name = String.valueOf(position);
		this.position = position;
		this.parameterType = parameterType;
	}

	public QueryParameterImpl(String name, Integer position, Class<T> parameterType)
	{
		this.name = name;
		this.position = position;
		this.parameterType = parameterType;
	}

	public boolean isBounded()
	{
		return bounded;
	}

	public T getValue()
	{
		if (!bounded) {
			throw new IllegalStateException("Parameter [" + (name != null ? name : position) + "] is not set");
		}//if

		return value;
	}

	public void setValue(T pValue)
	{
		value = pValue;
		bounded = true;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Integer getPosition()
	{
		return position;
	}

	public void setPosition(Integer position)
	{
		this.position = position;
	}

	@Override
	public Class<T> getParameterType()
	{
		return parameterType;
	}

	public QueryParameterImpl<T> copyAndSet(Object value)
	{
		QueryParameterImpl<T> copy = new QueryParameterImpl<>(name, position, parameterType);
		copy.setValue(copy.parameterType.cast(value));
		return copy;
	}//copyAndSet
}
