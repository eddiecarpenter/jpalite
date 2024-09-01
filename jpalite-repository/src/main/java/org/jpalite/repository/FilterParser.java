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
    public <S> Void visit(PlainSelect plainSelect, S context)
    {
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(this, context);
        }

        return null;
    }

    @Override
    public <S> Void visit(Addition addition, S context)
    {
        addition.getLeftExpression().accept(this, context);
        operationType = addition.getStringExpression();
        addition.getRightExpression().accept(this, context);

        return null;
    }

    @Override
    public <S> Void visit(Subtraction subtraction, S context)
    {
        subtraction.getLeftExpression().accept(this, context);
        operationType = subtraction.getStringExpression();
        subtraction.getRightExpression().accept(this, context);

        return null;
    }

    @Override
    public <S> Void visit(JdbcParameter jdbcParameter, S context)
    {
        params.add(parameters[paramNr++]);
        return null;
    }

    @Override
    public <S> Void visit(JdbcNamedParameter jdbcNamedParameter, S context)
    {
        params.add(parameters[paramNr++]);
        return null;
    }


    @Override
    public <S> Void visit(DoubleValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(LongValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(HexValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(DateValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(TimeValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(TimestampValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(StringValue pValue, S context)
    {
        params.add(pValue.getValue());
        return null;
    }

    @Override
    public <S> Void visit(Column tableColumn, S context)
    {
        col = tableColumn.getFullyQualifiedName();
        return null;
    }

    @Override
    public <S> Void visit(OrExpression orExpression, S context)
    {
        boolean vRoot = (currentFilter.isUnfiltered() && currentFilter == filter);

        orExpression.getLeftExpression().accept(this, context);
        Filter newFilter = currentFilter;
        currentFilter = Filter.noFilter();
        orExpression.getRightExpression().accept(this, context);

        currentFilter = Filter.of(newFilter).orWhere(currentFilter);
        if (vRoot) {
            this.filter = currentFilter;
        }//if

        return null;
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
    public <S> Void visit(Between between, S context)
    {
        between.getLeftExpression().accept(this, context);
        between.getBetweenExpressionStart().accept(this, context);
        between.getBetweenExpressionEnd().accept(this, context);
        addFilter(Operators.BETWEEN);
        return null;
    }

    @Override
    public <S> Void visit(EqualsTo equalsTo, S context)
    {
        equalsTo.getLeftExpression().accept(this, context);
        equalsTo.getRightExpression().accept(this, context);
        addFilter(Operators.EQUALS);
        return null;
    }

    @Override
    public <S> Void visit(GreaterThan greaterThan, S context)
    {
        greaterThan.getLeftExpression().accept(this, context);
        greaterThan.getRightExpression().accept(this, context);
        addFilter(Operators.BIGGER_THAN);
        return null;
    }

    @Override
    public <S> Void visit(GreaterThanEquals greaterThanEquals, S context)
    {
        greaterThanEquals.getLeftExpression().accept(this, context);
        greaterThanEquals.getRightExpression().accept(this, context);
        addFilter(Operators.BIGGER_OR_EQUAL);
        return null;
    }

    @Override
    public <S> Void visit(InExpression inExpression, S context)
    {
        if (inExpression.getLeftExpression() != null) {
            inExpression.getLeftExpression().accept(this, context);
        }//if

        if (inExpression.getRightExpression() != null) {
            inExpression.getRightExpression().accept(this, context);
        }//if

        addFilter(inExpression.isNot() ? Operators.NOTIN : Operators.IN);

        return null;
    }


    @Override
    public <S> Void visit(IsNullExpression isNullExpression, S context)
    {
        isNullExpression.getLeftExpression().accept(this, context);
        addFilter(isNullExpression.isNot() ? Operators.ISNOTNULL : Operators.ISNULL);
        return null;
    }

    @Override
    public <S> Void visit(LikeExpression likeExpression, S context)
    {
        likeExpression.getLeftExpression().accept(this, context);
        likeExpression.getRightExpression().accept(this, context);
        addFilter(likeExpression.isNot() ? Operators.CONTAINS_NOT : Operators.CONTAINS);
        return null;
    }

    @Override
    public <S> Void visit(MinorThan minorThan, S context)
    {
        minorThan.getLeftExpression().accept(this, context);
        minorThan.getRightExpression().accept(this, context);
        addFilter(Operators.SMALLER_THAN);
        return null;
    }

    @Override
    public <S> Void visit(MinorThanEquals minorThanEquals, S context)
    {
        minorThanEquals.getLeftExpression().accept(this, context);
        minorThanEquals.getRightExpression().accept(this, context);
        addFilter(Operators.SMALLER_OR_EQUAL);
        return null;
    }

    @Override
    public <S> Void visit(NotEqualsTo notEqualsTo, S context)
    {
        notEqualsTo.getLeftExpression().accept(this, context);
        notEqualsTo.getRightExpression().accept(this, context);
        addFilter(Operators.NOTEQUALS);
        return null;
    }

    @Override
    public <S> Void visit(IntervalExpression intervalExpression, S context)
    {
        if (operationType != null) {
            params.add(intervalExpression.getParameter());
            addFilter(operationType.equals("+") ? Operators.PLUS_INTERVAL : Operators.MINUS_INTERVAL);
            operationType = null;
        }//if

        return null;
    }
}
