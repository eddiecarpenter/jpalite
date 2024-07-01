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

import jakarta.persistence.PersistenceException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import net.sf.jsqlparser.statement.select.PlainSelect;

class SortParser extends ParserBase implements OrderByVisitor
{
	private Sort sort = Sort.unsorted();
	private int limit = Integer.MAX_VALUE;
	private int offset = 0;

	public SortParser(String expression)
	{
		try {
			Statement statement = CCJSqlParserUtil.parse("select * from T " + expression);
			statement.accept(this);
		}//try
		catch (JSQLParserException ex) {
			throw new PersistenceException("Error parsing query", ex);
		}//catch
	}

	public Sort getSort()
	{
		return sort;
	}

	public int getLimit()
	{
		return limit;
	}

	public int getOffset()
	{
		return offset;
	}

	@Override
	public void visit(OrderByElement orderBy)
	{
		if (orderBy.getExpression() instanceof Column vColumn) {
			orderBy.getExpression().accept(this);

			Direction direction = Direction.NATURAL;
			if (orderBy.isAscDescPresent()) {
				direction = orderBy.isAsc() ? Direction.ASC : Direction.DESC;
			}//if
			NullHandling handling = NullHandling.NATIVE;
			if (orderBy.getNullOrdering() != null) {
				handling = (orderBy.getNullOrdering() == OrderByElement.NullOrdering.NULLS_FIRST) ? NullHandling.FIRST : NullHandling.LAST;
			}//if

			sort = sort.and(Sort.by(new SortOrder(direction, vColumn.getColumnName(), handling)));
		}//if
	}

	@Override
	public void visit(PlainSelect plainSelect)
	{
		if (plainSelect.getOrderByElements() != null) {
			plainSelect.getOrderByElements().forEach(o -> o.accept(this));
		}//if

		if (plainSelect.getLimit() != null && plainSelect.getLimit().getRowCount() instanceof LongValue aLimit) {
			this.limit = (int) aLimit.getValue();
		}//if

		if (plainSelect.getOffset() != null && plainSelect.getOffset().getOffset() instanceof LongValue aOffset) {
			this.offset = (int) aOffset.getValue();
		}//if
	}
}
