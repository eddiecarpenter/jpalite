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

package io.jpalite.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageableTest
{
	@Test
	void getPageNumberAndSize()
	{
		Pageable test = Pageable.of(2, 10, Sort.by("test"));
		assertEquals(10, test.getPageSize());
		assertEquals(2, test.getPageNumber());
		assertEquals("order by test ASC offset 20 limit 10", test.getExpression());
	}

	@Test
	void isUnpaged()
	{
		Pageable test = Pageable.of(Sort.unsorted());
		assertTrue(test.isUnpaged());
		assertEquals("", test.getExpression());
	}

	@Test
	void testPaging()
	{
		Pageable test = Pageable.of(1, 10);
		assertEquals(10, test.getPageSize());
		assertEquals(1, test.getPageNumber());
		assertEquals(" offset 10 limit 10", test.getExpression());

		test = test.next();
		assertEquals(2, test.getPageNumber());
		assertEquals(20, test.getPageIndex());
		assertEquals(" offset 20 limit 10", test.getExpression());

		test = test.next();
		assertEquals(3, test.getPageNumber());
		assertEquals(" offset 30 limit 10", test.getExpression());

		test = test.previous();
		assertEquals(2, test.getPageNumber());
		assertEquals(" offset 20 limit 10", test.getExpression());

		test = test.first();
		assertEquals(0, test.getPageNumber());
		assertEquals(0, test.getPageIndex());
		assertEquals(" offset 0 limit 10", test.getExpression());
	}

	@Test
	void testOfExpression()
	{
		Pageable test = Pageable.fromExpression("order by test asc offset 100 limit 100");
		assertEquals(1, test.getPageNumber());
		assertEquals(100, test.getPageSize());
		assertEquals("order by test ASC offset 100 limit 100", test.getExpression());
	}
}
