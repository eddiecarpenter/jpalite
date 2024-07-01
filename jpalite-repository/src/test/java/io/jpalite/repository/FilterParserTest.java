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

class FilterParserTest
{

	@Test
	void whenSingleEqualExpression()
	{
		FilterParser parser = new FilterParser("test = ?", "1234");
		Filter expectedFilter = Filter.of("test", Operators.EQUALS, "1234");
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenSingleNotEqualExpression()
	{
		FilterParser parser = new FilterParser("test <> ?", "1234");
		Filter expectedFilter = Filter.of("test", Operators.NOTEQUALS, "1234");
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenSingleBiggerThanExpression()
	{
		FilterParser parser = new FilterParser("test > ?", 1234);
		Filter expectedFilter = Filter.of("test", Operators.BIGGER_THAN, 1234);
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test >= ?", 1234);
		expectedFilter = Filter.of("test", Operators.BIGGER_OR_EQUAL, 1234);
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenSingleSmallerThanExpression()
	{
		FilterParser parser = new FilterParser("test < ?", 1234);
		Filter expectedFilter = Filter.of("test", Operators.SMALLER_THAN, 1234);
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test <= ?", 1234);
		expectedFilter = Filter.of("test", Operators.SMALLER_OR_EQUAL, 1234);
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenSingleContainsExpression()
	{
		FilterParser parser = new FilterParser("test ilike ?", "1234");
		Filter expectedFilter = Filter.of("test", Operators.CONTAINS, "1234");
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test not ilike ?", "1234");
		expectedFilter = Filter.of("test", Operators.CONTAINS_NOT, "1234");
		assertEquals(expectedFilter, parser.getFilter());
	}


	@Test
	void whenSingleBetweenExpression()
	{
		FilterParser parser = new FilterParser("test between ? and ?", 1234, 9999);
		Filter expectedFilter = Filter.of("test", Operators.BETWEEN, 1234, 9999);
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenPlusIntervalExpression()
	{
		FilterParser parser = new FilterParser("test + interval '5 days'");
		Filter expectedFilter = Filter.of("test", Operators.PLUS_INTERVAL, "'5 days'");
		assertEquals(expectedFilter, parser.getFilter());
		assertEquals("test + interval '5 days'", expectedFilter.getExpression());
	}

	@Test
	void whenMinusIntervalExpression()
	{
		FilterParser parser = new FilterParser("test - interval '5 days'");
		Filter expectedFilter = Filter.of("test", Operators.MINUS_INTERVAL, "'5 days'");
		assertEquals(expectedFilter, parser.getFilter());
		assertEquals("test - interval '5 days'", expectedFilter.getExpression());
	}

	@Test
	void whenSingleInExpression()
	{
		FilterParser parser = new FilterParser("test in (?,?,?)", 1234, 5555, 9999);
		Filter expectedFilter = Filter.of("test", Operators.IN, 1234, 5555, 9999);
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test not in (?,?,?)", 1234, 5555, 9999);
		expectedFilter = Filter.of("test", Operators.NOTIN, 1234, 5555, 9999);
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenSingleIsNullExpression()
	{
		FilterParser parser = new FilterParser("test is null");
		Filter expectedFilter = Filter.of("test", Operators.ISNULL);
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test is not null");
		expectedFilter = Filter.of("test", Operators.ISNOTNULL);
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenMultipleAndExpression()
	{
		FilterParser parser = new FilterParser("test = ? and id =?", "1234", 9999);
		Filter expectedFilter = Filter.of("test", Operators.EQUALS, "1234")
				.andWhere(Filter.of("id", Operators.EQUALS, 9999));
		assertEquals(expectedFilter, parser.getFilter());

		parser = new FilterParser("test = ? and id =? and try = ?", "1234", 9999, 4444);
		expectedFilter = Filter.of("test", Operators.EQUALS, "1234")
				.andWhere(Filter.of("id", Operators.EQUALS, 9999))
				.andWhere(Filter.of("try", Operators.EQUALS, 4444));
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenMultipleWithParenthesisAndExpression()
	{
		FilterParser parser = new FilterParser("(test = ? and id =? and try = ?)", "1234", 9999, 4444);
		Filter expectedFilter = Filter.of("test", Operators.EQUALS, "1234")
				.andWhere(Filter.of("id", Operators.EQUALS, 9999))
				.andWhere(Filter.of("try", Operators.EQUALS, 4444));
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenMultipleWithParenthesisAndOrExpression()
	{
		FilterParser parser = new FilterParser("((test = :test1 AND test2 = :test21) OR test3 != :test31)", "1234", 9999, 4444);
		Filter expectedFilter = Filter.of(Filter.of("test", Operators.EQUALS, "1234")
												  .andWhere(Filter.of("test2", Operators.EQUALS, 9999)))
				.orWhere(Filter.of("test3", Operators.NOTEQUALS, 4444));
		assertEquals(expectedFilter, parser.getFilter());
	}

	@Test
	void whenExpressionIncludesParameters()
	{
		FilterParser parser = new FilterParser("((test = '1234' AND test2 = 9999) OR test3 != 4444)");
		Filter expectedFilter = Filter.of(Filter.of("test", Operators.EQUALS, "1234")
												  .andWhere(Filter.of("test2", Operators.EQUALS, 9999L)))
				.orWhere(Filter.of("test3", Operators.NOTEQUALS, 4444L));
		assertEquals(expectedFilter, parser.getFilter());
	}

}
