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

import jakarta.persistence.PersistenceException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.ArrayList;
import java.util.List;

class FilterParser extends ParserBase
{
    private Filter filter = Filter.noFilter();
    private final Object[] parameters;
    private int paramNr = 0;
    private Filter currentFilter;
    private String col = null;
    private String operationType = null;
    private final List<Object> params = new ArrayList<>();

    public FilterParser(String whereExpression, Object... params)
    {
        parameters    = params;
        currentFilter = filter;
        try {
            Statement statement = CCJSqlParserUtil.parse("select * from t where " + whereExpression);
            statement.accept(this);
        }//try
        catch (JSQLParserException ex) {
            throw new PersistenceException("Error parsing query", ex);
        }//catch
    }

    public Filter getFilter()
    {
        return filter;
    }

    @Override
    public void visit(PlainSelect plainSelect)
    {
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(this);
        }
    }

    @Override
    public void visit(Addition addition)
    {
        addition.getLeftExpression().accept(this);
        operationType = addition.getStringExpression();
        addition.getRightExpression().accept(this);
    }

    @Override
    public void visit(Subtraction subtraction)
    {
        subtraction.getLeftExpression().accept(this);
        operationType = subtraction.getStringExpression();
        subtraction.getRightExpression().accept(this);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter)
    {
        params.add(parameters[paramNr++]);
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter)
    {
        params.add(parameters[paramNr++]);
    }


    @Override
    public void visit(DoubleValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(LongValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(HexValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(DateValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(TimeValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(TimestampValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(StringValue pValue)
    {
        params.add(pValue.getValue());
    }

    @Override
    public void visit(Column tableColumn)
    {
        col = tableColumn.getFullyQualifiedName();
    }

    @Override
    public void visit(Parenthesis parenthesis)
    {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(AndExpression andExpression)
    {
        andExpression.getLeftExpression().accept(this);
        andExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(OrExpression orExpression)
    {
        boolean vRoot = (currentFilter.isUnfiltered() && currentFilter == filter);

        orExpression.getLeftExpression().accept(this);
        Filter newFilter = currentFilter;
        currentFilter = Filter.noFilter();
        orExpression.getRightExpression().accept(this);

        currentFilter = Filter.of(newFilter).orWhere(currentFilter);
        if (vRoot) {
            this.filter = currentFilter;
        }//if
    }

    private void addFilter(Operators pOperators)
    {
        Filter newFilter = Filter.of(col, pOperators, params.toArray());

        if (currentFilter.isUnfiltered() && currentFilter == this.filter) {
            currentFilter = newFilter;
            this.filter   = newFilter;
        }//if
        else {
            if (currentFilter.isUnfiltered()) {
                currentFilter = newFilter;
            }//if
            else {
                currentFilter.andWhere(newFilter);
            }//else
        }//else

        col = null;
        params.clear();
    }

    @Override
    public void visit(Between between)
    {
        between.getLeftExpression().accept(this);
        between.getBetweenExpressionStart().accept(this);
        between.getBetweenExpressionEnd().accept(this);
        addFilter(Operators.BETWEEN);
    }

    @Override
    public void visit(EqualsTo equalsTo)
    {
        equalsTo.getLeftExpression().accept(this);
        equalsTo.getRightExpression().accept(this);
        addFilter(Operators.EQUALS);
    }

    @Override
    public void visit(GreaterThan greaterThan)
    {
        greaterThan.getLeftExpression().accept(this);
        greaterThan.getRightExpression().accept(this);
        addFilter(Operators.BIGGER_THAN);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals)
    {
        greaterThanEquals.getLeftExpression().accept(this);
        greaterThanEquals.getRightExpression().accept(this);
        addFilter(Operators.BIGGER_OR_EQUAL);
    }

    @Override
    public void visit(InExpression inExpression)
    {
        if (inExpression.getLeftExpression() != null) {
            inExpression.getLeftExpression().accept(this);
        }//if

        if (inExpression.getRightItemsList() != null) {
            inExpression.getRightItemsList().accept(this);
        }//if

        addFilter(inExpression.isNot() ? Operators.NOTIN : Operators.IN);
    }


    @Override
    public void visit(IsNullExpression isNullExpression)
    {
        isNullExpression.getLeftExpression().accept(this);
        addFilter(isNullExpression.isNot() ? Operators.ISNOTNULL : Operators.ISNULL);
    }

    @Override
    public void visit(LikeExpression likeExpression)
    {
        likeExpression.getLeftExpression().accept(this);
        likeExpression.getRightExpression().accept(this);
        addFilter(likeExpression.isNot() ? Operators.CONTAINS_NOT : Operators.CONTAINS);
    }

    @Override
    public void visit(MinorThan minorThan)
    {
        minorThan.getLeftExpression().accept(this);
        minorThan.getRightExpression().accept(this);
        addFilter(Operators.SMALLER_THAN);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals)
    {
        minorThanEquals.getLeftExpression().accept(this);
        minorThanEquals.getRightExpression().accept(this);
        addFilter(Operators.SMALLER_OR_EQUAL);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo)
    {
        notEqualsTo.getLeftExpression().accept(this);
        notEqualsTo.getRightExpression().accept(this);
        addFilter(Operators.NOTEQUALS);
    }

    @Override
    public void visit(IntervalExpression intervalExpression)
    {
        if (operationType != null) {
            params.add(intervalExpression.getParameter());
            addFilter(operationType.equals("+") ? Operators.PLUS_INTERVAL : Operators.MINUS_INTERVAL);
            operationType = null;
        }//if
    }
}
