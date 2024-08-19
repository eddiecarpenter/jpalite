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

import org.jpalite.EntityLifecycle;
import org.jpalite.EntityMapException;
import jakarta.persistence.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S3011")//Changing accessibility mode is needed
public class EntityLifecycleImpl implements EntityLifecycle
{
	private final List<Methods> listeners;

	private static class Methods
	{
		private final Class<?> listenerClass;
		private Method postLoad;
		private Method prePersist;
		private Method postPersist;
		private Method preUpdate;
		private Method postUpdate;
		private Method preRemove;
		private Method postRemove;

		public Methods(Class<?> aClass)
		{
			listenerClass = aClass;
			for (Method method : aClass.getMethods()) {
				if (method.isAnnotationPresent(PostLoad.class)) {
					postLoad = method;
					postLoad.setAccessible(true);
				}//if
				else if (method.isAnnotationPresent(PrePersist.class)) {
					prePersist = method;
					prePersist.setAccessible(true);
				}//if
				else if (method.isAnnotationPresent(PostPersist.class)) {
					postPersist = method;
					postPersist.setAccessible(true);
				}//if
				else if (method.isAnnotationPresent(PreUpdate.class)) {
					preUpdate = method;
					preUpdate.setAccessible(true);
				}//if
				else if (method.isAnnotationPresent(PostUpdate.class)) {
					postUpdate = method;
					postUpdate.setAccessible(true);
				}//if
				else if (method.isAnnotationPresent(PreRemove.class)) {
					preRemove = method;
				}//if

				if (method.isAnnotationPresent(PostRemove.class)) {
					postRemove = method;
				}//if
			}//for
		}
	}

	@FunctionalInterface
	private interface LifeCycleFunction<O, M>
	{
		void accept(O o, M m) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException;
	}

	public EntityLifecycleImpl(Class<?> entityClass)
	{
		listeners = new ArrayList<>();

		EntityListeners listeners = entityClass.getAnnotation(EntityListeners.class);
		if (listeners != null) {
			for (Class<?> aClass : listeners.value()) {
				this.listeners.add(new Methods(aClass));
			}//if
		}//if
		this.listeners.add(new Methods(entityClass));
	}//EntityLifecycleImpl

	private void invokeCallback(LifeCycleFunction<Object, Methods> func)
	{
		for (Methods method : listeners) {
			try {
				Object listener;
				if (method.listenerClass != null) {
					listener = method.listenerClass.getConstructor().newInstance();
				}//if
				else {
					listener = null;
				}//else

				func.accept(listener, method);
			}//try
			catch (InvocationTargetException | InstantiationException | IllegalAccessException |
				   NoSuchMethodException ex) {
				throw new EntityMapException("Error executing callback handler");
			}//catch
		}//for
	}//invokeCallback

	@Override
	public void postLoad(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.postLoad != null) {
							   if (listener == null) {
								   methods.postLoad.invoke(entity);
							   }//if
							   else {
								   methods.postLoad.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void prePersist(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.prePersist != null) {
							   if (listener == null) {
								   methods.prePersist.invoke(entity);
							   }//if
							   else {
								   methods.prePersist.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void postPersist(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.postPersist != null) {
							   if (listener == null) {
								   methods.postPersist.invoke(entity);
							   }//if
							   else {
								   methods.postPersist.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void preUpdate(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.preUpdate != null) {
							   if (listener == null) {
								   methods.preUpdate.invoke(entity);
							   }//if
							   else {
								   methods.preUpdate.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void postUpdate(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.postUpdate != null) {
							   if (listener == null) {
								   methods.postUpdate.invoke(entity);
							   }//if
							   else {
								   methods.postUpdate.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void preRemove(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.preRemove != null) {
							   if (listener == null) {
								   methods.preRemove.invoke(entity);
							   }//if
							   else {
								   methods.preRemove.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}

	@Override
	public void postRemove(Object entity)
	{
		invokeCallback((listener, methods) ->
					   {
						   if (methods.postRemove != null) {
							   if (listener == null) {
								   methods.postRemove.invoke(entity);
							   }//if
							   else {
								   methods.postRemove.invoke(listener, entity);
							   }//else
						   }//if
					   });
	}
}
