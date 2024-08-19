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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest
{

	@Test
	void noFilter()
	{
		Filter filter = Filter.noFilter();
		assertTrue(filter.isUnfiltered());
		assertEquals("", filter.getExpression());

		filter = Filter.of(Filter.noFilter());
		assertTrue(filter.isUnfiltered());
		assertEquals("", filter.getExpression());

		filter = Filter.noFilter();
		filter.andWhere(Filter.of("test", Operators.EQUALS, 1));
		assertFalse(filter.isUnfiltered());
		assertEquals("(test = '1')", filter.getExpression());

		filter = Filter.of("test", Operators.EQUALS, 1);
		assertFalse(filter.isUnfiltered());
	}

	@Test
	void withSingleExpression_UsingEquals_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.EQUALS, 2);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test = :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));

		filter = Filter.of("test", Operators.NOTEQUALS, 2);
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test != :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));
	}

	@Test
	void withSingleExpression_UsingBiggerThan_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.BIGGER_THAN, 2);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test > :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));

		filter = Filter.of("test", Operators.BIGGER_OR_EQUAL, 2);
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test >= :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));
	}

	@Test
	void withSingleExpression_UsingSmallerThan_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.SMALLER_THAN, 2);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test < :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));

		filter = Filter.of("test", Operators.SMALLER_OR_EQUAL, 2);
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test <= :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals(2, parameters.get("test1"));
	}

	@Test
	void withSingleExpression_UsingIN_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.IN, 2, 3, 4);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test in (:test1,:test2,:test3)", expression);
		assertEquals(3, parameters.size());
		assertEquals(2, parameters.get("test1"));
		assertEquals(3, parameters.get("test2"));
		assertEquals(4, parameters.get("test3"));

		filter = Filter.of("test", Operators.NOTIN, 2, 3, 4);
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test not in (:test1,:test2,:test3)", expression);
		assertEquals(3, parameters.size());
		assertEquals(2, parameters.get("test1"));
		assertEquals(3, parameters.get("test2"));
		assertEquals(4, parameters.get("test3"));
	}

	@Test
	void withSingleExpression_UsingBetween_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.BETWEEN, 2, 4);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test between :test1 and :test2", expression);
		assertEquals(2, parameters.size());
		assertEquals(2, parameters.get("test1"));
		assertEquals(4, parameters.get("test2"));
	}


	@Test
	void withSingleExpression_UsingIsNUll_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.ISNULL);
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test is null", expression);
		assertTrue(parameters.isEmpty());

		filter = Filter.of("test", Operators.ISNOTNULL);
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test is not null", expression);
		assertTrue(parameters.isEmpty());
	}

	@Test
	void withSingleExpression_UsingContains_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.CONTAINS, "test");
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test ilike :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals("%test%", parameters.get("test1"));

		filter = Filter.of("test", Operators.CONTAINS_NOT, "test");
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("test not ilike :test1", expression);
		assertEquals(1, parameters.size());
		assertEquals("%test%", parameters.get("test1"));
	}

	@Test
	void withANDExpression_UsingEquals_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.EQUALS, "test")
				.andWhere(Filter.of("test2", Operators.EQUALS, "123"));
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("(test = :test1 AND test2 = :test21)", expression);
		assertEquals(2, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("(test = 'test' AND test2 = '123')", filter.getExpression());

		parameters = new HashMap<>();
		filter.andWhere(Filter.of("test3", Operators.NOTEQUALS, "testing"));

		expression = filter.getExpression(parameters);
		assertEquals("(test = :test1 AND test2 = :test21 AND test3 != :test31)", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
		assertEquals("(test = 'test' AND test2 = '123' AND test3 != 'testing')", filter.getExpression());

		filter.removeWhere("test3");
		parameters = new HashMap<>();
		expression = filter.getExpression(parameters);
		assertEquals("(test = :test1 AND test2 = :test21)", expression);
		assertEquals(2, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("(test = 'test' AND test2 = '123')", filter.getExpression());
	}

	@Test
	void withORExpression_UsingEquals_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.EQUALS, "test")
				.orWhere(Filter.of("test2", Operators.EQUALS, "123"));
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("(test = :test1 OR test2 = :test21)", expression);
		assertEquals(2, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));

		parameters = new HashMap<>();
		filter.orWhere(Filter.of("test3", Operators.NOTEQUALS, "testing"));

		expression = filter.getExpression(parameters);
		assertEquals("(test = :test1 OR test2 = :test21 OR test3 != :test31)", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
	}

	@Test
	void withORAndANDExpression_UsingEquals_GetExpression()
	{
		Filter filter = Filter.of(Filter.of("test", Operators.EQUALS, "test")
										  .andWhere(Filter.of("test2", Operators.EQUALS, "123")))
				.orWhere(Filter.of("test3", Operators.NOTEQUALS, "testing"));
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);

		assertEquals("((test = :test1 AND test2 = :test21) OR test3 != :test31)", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
	}

	@Test
	void withNestedExpression_UsingEquals_GetExpression()
	{
		Filter filter = Filter.of("test", Operators.EQUALS, "test")
				.andWhere(Filter.of("test2", Operators.EQUALS, "123")
								  .orWhere(Filter.of("test3", Operators.NOTEQUALS, "testing")));
		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);

		assertEquals("(test = :test1 AND (test2 = :test21 OR test3 != :test31))", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
	}

	@Test
	void withExpressionCreateFilterWithParameters()
	{
		Filter filter = Filter.fromExpression("((test = :t1 AND test2 = :t2) OR test3 != :t3)",
											  "test", "123", "testing");

		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("((test = :test1 AND test2 = :test21) OR test3 != :test31)", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
	}

	@Test
	void withExpressionCreateFilter()
	{
		Filter filter = Filter.fromExpression("((test = 'test' AND test2 = '123') OR test3 != 'testing')");

		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("((test = :test1 AND test2 = :test21) OR test3 != :test31)", expression);
		assertEquals(3, parameters.size());
		assertEquals("test", parameters.get("test1"));
		assertEquals("123", parameters.get("test21"));
		assertEquals("testing", parameters.get("test31"));
	}

	@Test
	void withPlusIntervalCreateFilter()
	{
		Filter filter = Filter.fromExpression("test + interval '5 days'");

		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test + interval '5 days'", expression);
		assertEquals(0, parameters.size());
	}

	@Test
	void withMinusIntervalCreateFilter()
	{
		Filter filter = Filter.fromExpression("test - interval '5 days'");

		Map<String, Object> parameters = new HashMap<>();
		String expression = filter.getExpression(parameters);
		assertEquals("test - interval '5 days'", expression);
		assertEquals(0, parameters.size());
	}
}
