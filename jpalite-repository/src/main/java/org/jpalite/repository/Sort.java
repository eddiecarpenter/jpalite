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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sort
{
	private static final Sort UNSORTED = new Sort();

	private final List<SortOrder> sortOrders;

	public static Sort by(List<SortOrder> orders)
	{
		if (orders == null) {
			throw new IllegalArgumentException("Orders cannot by null");
		}//if

		return orders.isEmpty() ? Sort.unsorted() : new Sort(orders);
	}//by

	public static Sort by(SortOrder... orders)
	{
		if (orders == null) {
			throw new IllegalArgumentException("You have to provide at least one order to sort by");
		}

		return new Sort(Arrays.asList(orders));
	}//by

	public static Sort by(String... fields)
	{
		return (fields.length == 0) ? Sort.unsorted() : new Sort(SortOrder.DEFAULT_DIRECTION, Arrays.asList(fields));
	}//by

	public static Sort by(Direction direction, String... fields)
	{

		if (direction == null) {
			throw new IllegalArgumentException("Direction cannot be null");
		}
		if (fields == null || fields.length == 0) {
			throw new IllegalArgumentException("You have to provide at least one field to sort by");
		}

		return Sort.by(Arrays.stream(fields)
							   .map(it -> new SortOrder(direction, it))
							   .toList());
	}//by

	public static Sort byExpression(String orderByExpression)
	{
		SortParser vParser = new SortParser("order by " + orderByExpression);
		return vParser.getSort();
	}//byExpression

	public static Sort unsorted()
	{
		return UNSORTED;
	}//unsorted

	public boolean isUnsorted()
	{
		return sortOrders.isEmpty();
	}//isUnsorted

	private Sort()
	{
		sortOrders = Collections.emptyList();
	}

	private Sort(List<SortOrder> orders)
	{
		this.sortOrders = new ArrayList<>(orders);
	}//Sort

	private Sort(Direction direction, List<String> fields)
	{
		if (fields == null || fields.isEmpty()) {
			throw new IllegalArgumentException("You have to provide at least one field to sort by");
		}

		this.sortOrders = fields.stream()
				.map(field -> new SortOrder(direction, field))
				.toList();
	}//Sort

	public Sort and(Sort sort)
	{
		if (sort == null) {
			throw new IllegalArgumentException("Sort cannot be null");
		}

		List<SortOrder> newSortOrder = new ArrayList<>(sortOrders);
		newSortOrder.addAll(sort.sortOrders);

		return Sort.by(newSortOrder);
	}//and

	public Sort descending()
	{
		return withDirection(Direction.DESC);
	}//descending

	public Sort ascending()
	{
		return withDirection(Direction.ASC);
	}//ascending

	public Sort naturally()
	{
		return withDirection(Direction.NATURAL);
	}//naturally

	public Sort withDirection(Direction direction)
	{
		return Sort.by(sortOrders.stream().map(it -> it.with(direction)).toList());
	}//withDirection

	public Stream<SortOrder> stream()
	{
		return sortOrders.stream();
	}//stream

	public String getExpression()
	{
		return stream()
				.map(p -> p.getField() + (p.getDirection() == Direction.NATURAL ? "" : " " + p.getDirection()) + (p.getNullHandling() == NullHandling.NATIVE ? "" : " NULLS " + p.getNullHandling()))
				.collect(Collectors.joining(", "));
	}

	@Override
	public String toString()
	{
		return getExpression();
	}

	public String getOrderBy()
	{
		if (sortOrders.isEmpty()) {
			return "";
		}//if

		return " order by " + getExpression();
	}//getOrderBy
}//Sort
