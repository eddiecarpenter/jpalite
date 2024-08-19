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

package org.jpalite;

import jakarta.persistence.PersistenceException;

public enum EntityState
{
	/**
	 * This instance isn't, and never was, attached to an EntityManager. This instance has no corresponding rows in the
	 * database; it's usually just a new object that we created to save to the database.
	 */
	TRANSIENT,

	/**
	 * This instance is associated with a unique EntityManager object. Upon calling upon the EntityManager to persist
	 * the change to the database, this entity is guaranteed to have a corresponding consistent record in the database.
	 */
	MANAGED,

	/**
	 * This instance was once attached to the EntityManager (in a persistent state), but now it’s not. An instance
	 * enters this state if we evict it from the context, clear or close the EntityManager, or put the instance through
	 * serialization/deserialization process.
	 */
	DETACHED,
	/**
	 * The instance was deleted from the database and is scheduled to be removed from the entity manager. The entity is
	 * ignored and reference to an entity in this state will generate a {@link PersistenceException} exception
	 */
	REMOVED
}
