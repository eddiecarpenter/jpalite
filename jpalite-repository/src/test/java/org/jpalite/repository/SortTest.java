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

package org.jpalite.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortTest
{
	@Test
	void withUnsorted()
	{
		Sort sort = Sort.unsorted();
		assertTrue(sort.isUnsorted());
		assertEquals("", sort.getExpression());
	}

	@Test
	void withSimpleSortByFieldExpression()
	{
		Sort sort = Sort.by("test");
		assertEquals("test ASC", sort.getExpression());
	}

	@Test
	void withSimpleSortByField()
	{
		Sort sort = Sort.by("test");
		assertEquals(" order by test ASC", sort.getOrderBy());
	}

	@Test
	void withSimpleSortByDirectionFields()
	{
		Sort sort = Sort.by(Direction.DESC, "test", "test2");
		assertEquals(" order by test DESC, test2 DESC", sort.getOrderBy());
	}

	@Test
	void withSortOrderByDirectionFields()
	{
		Sort sort = Sort.by(new SortOrder(Direction.DESC, "test", NullHandling.FIRST))
				.and(Sort.by(new SortOrder(Direction.NATURAL, "test2", NullHandling.LAST)));
		assertEquals(" order by test DESC NULLS FIRST, test2 NULLS LAST", sort.getOrderBy());
	}

	@Test
	void withSortExpression()
	{
		Sort sort = Sort.byExpression("test ASC, test2 DESC NULLS FIRST");
		assertEquals(" order by test ASC, test2 DESC NULLS FIRST", sort.getOrderBy());
	}
}
