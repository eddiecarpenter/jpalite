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

class SortParserTest
{

	@Test
	void withSimpleASCOrderBy()
	{
		SortParser parser = new SortParser("order by t1 asc");
		assertEquals("t1 ASC", parser.getSort().getExpression());
	}

	@Test
	void withSimpleDESCNullsFirstOrderBy()
	{
		SortParser parser = new SortParser("order by t1 asc nulls first");
		assertEquals("t1 ASC NULLS FIRST", parser.getSort().getExpression());
	}

	@Test
	void withSimpleNaturalNullsFirstOrderBy()
	{
		SortParser parser = new SortParser("order by t1 nulls first");
		assertEquals("t1 NULLS FIRST", parser.getSort().getExpression());
	}

	@Test
	void withMultipleColumnsSortAsc()
	{
		SortParser parser = new SortParser("order by t1 asc, t2 asc");
		assertEquals("t1 ASC, t2 ASC", parser.getSort().getExpression());
	}

	@Test
	void withMultipleColumnsSortAscDesc()
	{
		SortParser parser = new SortParser("order by t1 asc nulls first, t2 desc nulls last");
		assertEquals("t1 ASC NULLS FIRST, t2 DESC NULLS LAST", parser.getSort().getExpression());
	}

	@Test
	void withLimits()
	{
		SortParser parser = new SortParser("offset 5 limit 10");
		assertEquals(5, parser.getOffset());
		assertEquals(10, parser.getLimit());
	}
}
