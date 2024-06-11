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

public class Pageable
{
	private final int pageNumber;
	private final int pageSize;
	private final Sort sort;

	public static Pageable unpaged()
	{
		return new Pageable(0, Integer.MAX_VALUE, Sort.unsorted());
	}

	public static Pageable of(Sort sort)
	{
		return new Pageable(0, Integer.MAX_VALUE, sort);
	}

	public static Pageable of(int pageSize)
	{
		return new Pageable(0, pageSize, Sort.unsorted());
	}

	public static Pageable of(int pageNumber, int pageSize)
	{
		return new Pageable(pageNumber, pageSize, Sort.unsorted());
	}

	public static Pageable of(int pageNumber, int pageSize, Sort sort)
	{
		return new Pageable(pageNumber, pageSize, sort);
	}//of

	public static Pageable fromExpression(String expression)
	{
		SortParser parser = new SortParser(expression);
		return Pageable.of(parser.getOffset() / parser.getLimit(), parser.getLimit(), parser.getSort());
	}

	private Pageable(int pageNumber, int pageSize, Sort sort)
	{
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.sort = sort;
	}

	public int getPageNumber()
	{
		return pageNumber;
	}


	public int getPageSize()
	{
		return pageSize;
	}

	public Sort getSort()
	{
		return sort;
	}

	public int getPageIndex()
	{
		return pageNumber * pageSize;
	}

	public boolean isUnpaged()
	{
		return (pageSize == Integer.MAX_VALUE);
	}

	public Pageable next()
	{
		return new Pageable(getPageNumber() + 1, getPageSize(), getSort());
	}

	public Pageable previous()
	{
		return getPageNumber() == 0 ? this : new Pageable(getPageNumber() - 1, getPageSize(), getSort());
	}

	public Pageable first()
	{
		return new Pageable(0, getPageSize(), getSort());
	}

	public String getExpression()
	{
		StringBuilder stringBuilder = new StringBuilder();
		if (!sort.isUnsorted()) {
			stringBuilder.append("order by ").append(sort.getExpression());
		}//if
		if (!isUnpaged()) {
			stringBuilder.append(" offset ").append(getPageIndex())
					.append(" limit ").append(getPageSize());
		}//if
		return stringBuilder.toString();
	}

	@Override
	public String toString()
	{
		return getExpression();
	}
}
