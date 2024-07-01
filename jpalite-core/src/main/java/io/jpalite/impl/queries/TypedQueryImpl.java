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

import io.jpalite.PersistenceContext;
import io.jpalite.queries.QueryLanguage;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@SuppressWarnings("unchecked")
public class TypedQueryImpl<T> extends JPALiteQueryImpl<T> implements TypedQuery<T>
{
	public TypedQueryImpl(String queryText, QueryLanguage queryLanguage, PersistenceContext persistenceContext, Class<T> entityClass, @Nonnull Map<String, Object> hints)
	{
		super(queryText, queryLanguage, persistenceContext, entityClass, hints);
	}

	@Override
	public TypedQuery<T> setMaxResults(int maxResults)
	{
		return (TypedQuery<T>) super.setMaxResults(maxResults);
	}//setMaxResults

	@Override
	public TypedQuery<T> setFirstResult(int startPosition)
	{
		return (TypedQuery<T>) super.setFirstResult(startPosition);
	}//setFirstResult

	@Override
	public TypedQuery<T> setHint(String hintName, Object value)
	{
		return (TypedQuery<T>) super.setHint(hintName, value);
	}//setHint

	@Override
	public TypedQuery<T> setParameter(String name, Calendar value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(name, value, temporalType);
	}//setParameter

	@Override
	public TypedQuery<T> setFlushMode(FlushModeType flushMode)
	{
		return (TypedQuery<T>) super.setFlushMode(flushMode);
	}//setFlushMode

	@Override
	public TypedQuery<T> setLockMode(LockModeType lockMode)
	{
		return (TypedQuery<T>) super.setLockMode(lockMode);
	}//setLockMode

	@Override
	public <P> TypedQuery<T> setParameter(Parameter<P> param, P value)
	{
		return (TypedQuery<T>) super.setParameter(param, value);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(param, value, temporalType);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(param, value, temporalType);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(String name, Object value)
	{
		return (TypedQuery<T>) super.setParameter(name, value);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(String name, Date value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(name, value, temporalType);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(int position, Object value)
	{
		return (TypedQuery<T>) super.setParameter(position, value);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(int position, Calendar value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(position, value, temporalType);
	}//setParameter

	@Override
	public TypedQuery<T> setParameter(int position, Date value, TemporalType temporalType)
	{
		return (TypedQuery<T>) super.setParameter(position, value, temporalType);
	}//setParameter
}//TypedQueryImpl
