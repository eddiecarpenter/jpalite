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

import java.util.*;
import java.util.stream.Stream;

public class Filter
{
	public static final String AND = " AND ";
	public static final String OR = " OR ";

	private final List<Filter> filters;
	private FilterExpression filterExpressions;
	private String condOper;

	public static class FilterExpression
	{
		private final String field;
		private final Operators operator;
		private final Object[] parameter;

		public FilterExpression(String field, Operators operator, Object... parameters)
		{
			this.field = field;
			this.operator = operator;
			if (operator.getNrValues() > -1 && operator.getNrValues() != parameters.length) {
				throw new IllegalArgumentException("Operator " + operator.name() + " expects " + operator.getNrValues() + " parameters but " + parameters.length + " were received");
			}//if
			this.parameter = parameters;
		}//FilterExpression

		public String getExpression(Map<String, Object> params)
		{
			String paramName = field.replace('.', '_');
			return switch (operator) {
				case PLUS_INTERVAL -> field + " + " + operator.getOperator() + " " + parameter[0];
				case MINUS_INTERVAL -> field + " - " + operator.getOperator() + " " + parameter[0];
				case BETWEEN -> {
					params.put(paramName + "1", parameter[0]);
					params.put(paramName + "2", parameter[1]);
					yield field + " " + operator.getOperator() + " :" + paramName + "1 and :" + paramName + "2";
				}
				case IN, NOTIN -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator())
							.append(" (");

					int paramNr = 1;
					for (Object param : parameter) {
						params.put(paramName + paramNr, param);
						if (paramNr > 1) {
							expr.append(",");
						}//if
						expr.append(":").append(paramName).append(paramNr);
						paramNr++;
					}//for
					expr.append(")");
					yield expr.toString();
				}
				case CONTAINS, CONTAINS_NOT, BEGINS_WITH, ENDS_WITH -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator());

					int paramNr = 1;
					for (Object param : parameter) {
						params.put(paramName + paramNr, operator.getPrefix() + param + operator.getPostfix());
						expr.append(" :").append(paramName).append(paramNr);
						paramNr++;
					}//for
					yield expr.toString();
				}
				default -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator());

					int paramNr = 1;
					for (Object param : parameter) {
						params.put(paramName + paramNr, param);
						if (paramNr > 1) {
							expr.append(",");
						}//if
						expr.append(" :").append(paramName).append(paramNr);
						paramNr++;
					}//for
					yield expr.toString();
				}
			};
		}//getExpression

		public String getExpression()
		{
			return switch (operator) {
				case PLUS_INTERVAL -> field + " + " + operator.getOperator() + " " + parameter[0];
				case MINUS_INTERVAL -> field + " - " + operator.getOperator() + " " + parameter[0];
				case BETWEEN ->
						field + " " + operator.getOperator() + " '" + parameter[0] + "' and '" + parameter[1] + "'";
				case IN, NOTIN -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator())
							.append(" (");

					for (Object param : parameter) {
						expr.append("'").append(param).append("'");
					}//for
					expr.append(")");
					yield expr.toString();
				}
				case CONTAINS, CONTAINS_NOT, BEGINS_WITH, ENDS_WITH -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator());

					int paramNr = 1;
					for (Object param : parameter) {
						if (paramNr > 1) {
							expr.append(",");
						}//if
						expr.append(" '")
								.append(operator.getPrefix())
								.append(param)
								.append(operator.getPostfix())
								.append("'");
						paramNr++;
					}//for
					yield expr.toString();
				}
				default -> {
					StringBuilder expr = new StringBuilder(field);
					expr.append(" ")
							.append(operator.getOperator());

					int paramNr = 1;
					for (Object param : parameter) {
						if (paramNr > 1) {
							expr.append(",");
						}//if
						expr.append(" '")
								.append(param)
								.append("'");
						paramNr++;
					}//for
					yield expr.toString();
				}
			};
		}//getExpression

		@Override
		public String toString()
		{
			return getExpression(new HashMap<>());
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other) return true;
			if (!(other instanceof FilterExpression expr)) return false;

			if (!Objects.equals(field, expr.field)) return false;
			if (operator != expr.operator) return false;
			if (parameter.length != expr.parameter.length) return false;
			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			return Arrays.equals(parameter, expr.parameter);
		}

		@Override
		public int hashCode()
		{
			int vresult = field != null ? field.hashCode() : 0;
			vresult = 31 * vresult + (operator != null ? operator.hashCode() : 0);
			vresult = 31 * vresult + Arrays.hashCode(parameter);
			return vresult;
		}
	}//FilterExpression

	public static Filter of(String field, Operators operator, Object... value)
	{
		return new Filter(field, operator, value);
	}//of

	public static Filter of(Filter filter)
	{
		if (filter.isUnfiltered()) {
			return new Filter();
		}//if
		return new Filter(filter);
	}//of

	public static Filter noFilter()
	{
		return new Filter();
	}//noFilter

	public boolean isUnfiltered()
	{
		return filters.isEmpty() && filterExpressions == null;
	}

	/**
	 * The constructor for the Filter class.
	 */
	private Filter()
	{
		condOper = "";
		filters = new ArrayList<>();
		filterExpressions = null;
	}//Filter

	/**
	 * The constructor for the Filter class.
	 *
	 * @param filter Array for filters
	 */
	private Filter(Filter filter)
	{
		this();
		filters.add(filter);
	}//Filter

	private Filter(String field, Operators operator, Object... parameters)
	{
		this();

		filterExpressions = new FilterExpression(field, operator, parameters);
	}//Filter

	List<Filter> getFilters()
	{
		return filters;
	}

	String getCondOper()
	{
		return condOper;
	}

	public Filter orWhere(Filter filter)
	{
		if (condOper.isBlank() && filters.isEmpty() && filterExpressions != null) {
			Filter orFilter = new Filter();
			orFilter.filterExpressions = filterExpressions;
			orFilter.condOper = OR;
			filterExpressions = null;
			filters.add(orFilter);
		}//if

		filter.condOper = OR;
		filters.add(filter);
		return this;
	}//orWhere

	public Filter andWhere(Filter filter)
	{
		if (condOper.isBlank() && filters.isEmpty() && filterExpressions != null) {
			Filter andFilter = new Filter();
			andFilter.filterExpressions = filterExpressions;
			andFilter.condOper = AND;
			filterExpressions = null;
			filters.add(andFilter);
		}//if
		filter.condOper = AND;
		filters.add(filter);
		return this;
	}//andWhere

	public Filter removeWhere(String field)
	{
		if (filterExpressions != null && filterExpressions.field.equals(field)) {
			filterExpressions = null;
		}
		else {
			filters.forEach(f -> f.removeWhere(field));
			filters.removeIf(Filter::isUnfiltered);
		}//else

		return this;
	}//removeWhere

	public Stream<Filter> stream()
	{
		return filters.stream();
	}//stream

	public String getExpression(Map<String, Object> params)
	{
		if (filterExpressions != null) {
			return filterExpressions.getExpression(params);
		}//if
		else {
			if (!filters.isEmpty()) {
				StringBuilder buffer = new StringBuilder("(");
				boolean first = true;
				for (Filter filter : filters) {
					if (!first) {
						buffer.append(filter.getCondOper());
					}//if
					else {
						first = false;
					}//else

					buffer.append(filter.getExpression(params));
				}//for
				buffer.append(")");
				return buffer.toString();
			}//if
		}//else

		return "";
	}//getExpression

	public String getExpression()
	{
		if (filterExpressions != null) {
			return filterExpressions.getExpression();
		}//if
		else {
			if (!filters.isEmpty()) {
				StringBuilder buffer = new StringBuilder("(");
				boolean first = true;
				for (Filter vFilter : filters) {
					if (!first) {
						buffer.append(vFilter.getCondOper());
					}//if
					else {
						first = false;
					}//else

					buffer.append(vFilter.getExpression());
				}//for
				buffer.append(")");
				return buffer.toString();
			}//if
		}//else

		return "";
	}

	@Override
	public String toString()
	{
		return getExpression(new HashMap<>());
	}//toString

	public static Filter fromExpression(String expression, Object... parameters)
	{
		if (expression == null || expression.isBlank()) {
			return noFilter();
		}//if
		FilterParser parser = new FilterParser(expression, parameters);
		return parser.getFilter();
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other) return true;
		if (!(other instanceof Filter filter)) return false;

		if (!Objects.equals(filters, filter.filters)) return false;
		if (!Objects.equals(filterExpressions, filter.filterExpressions)) return false;
		return Objects.equals(condOper, filter.condOper);
	}

	@Override
	public int hashCode()
	{
		int vresult = filters != null ? filters.hashCode() : 0;
		vresult = 31 * vresult + (filterExpressions != null ? filterExpressions.hashCode() : 0);
		vresult = 31 * vresult + (condOper != null ? condOper.hashCode() : 0);
		return vresult;
	}
}//Filter
