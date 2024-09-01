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

package org.jpalite.impl.parsers;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterSession;
import net.sf.jsqlparser.statement.alter.AlterSystemStatement;
import net.sf.jsqlparser.statement.alter.RenameTableStatement;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.analyze.Analyze;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.jpalite.JpqlSyntaxException;

import java.util.List;
import java.util.Map;

public class JPQLAdaptor implements SelectVisitor<Void>, FromItemVisitor<Void>, ExpressionVisitor<Void>,
                                    SelectItemVisitor<Void>, StatementVisitor<Void>, GroupByVisitor<Void>, OrderByVisitor<Void>
{
    @Override
    public <S> Void visit(Select select, S context)
    {
        List<WithItem> withItemsList = select.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<?>) this, context);
            }
        }
        select.accept((SelectVisitor<?>) this, context);
        return null;
    }

    public void visit(Select select)
    {
        this.visit(select, null);
    }

    @Override
    public <S> Void visit(TranscodingFunction transcodingFunction, S context)
    {
        transcodingFunction.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(TrimFunction trimFunction, S context)
    {
        if (trimFunction.getExpression() != null) {
            trimFunction.getExpression().accept(this, context);
        }
        if (trimFunction.getFromExpression() != null) {
            trimFunction.getFromExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(RangeExpression rangeExpression, S context)
    {
        rangeExpression.getStartExpression().accept(this, context);
        rangeExpression.getEndExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(WithItem withItem, S context)
    {
        withItem.getSelect().accept((SelectVisitor<?>) this, context);
        return null;
    }

    @Override
    public void visit(WithItem withItem)
    {
        SelectVisitor.super.visit(withItem);
    }

    @Override
    public <S> Void visit(ParenthesedSelect select, S context)
    {
        List<WithItem> withItemsList = select.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<?>) this, context);
            }
        }
        select.getSelect().accept((SelectVisitor<?>) this, context);
        return null;
    }

    public void visit(ParenthesedSelect parenthesedSelect)
    {
        this.visit(parenthesedSelect, null);
    }

    @Override
    public <S> Void visit(PlainSelect plainSelect, S context)
    {
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<?>) this, context);
            }
        }
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                item.accept(this, context);
            }
        }

        if (plainSelect.getFromItem() != null) {
            plainSelect.getFromItem().accept(this, context);
        }

        visitJoins(plainSelect.getJoins(), context);
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(this, context);
        }

        if (plainSelect.getHaving() != null) {
            plainSelect.getHaving().accept(this, context);
        }

        if (plainSelect.getOracleHierarchical() != null) {
            plainSelect.getOracleHierarchical().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(Table tableName, S context)
    {
        return null;
    }

    @Override
    public void visit(Table tableName)
    {
        FromItemVisitor.super.visit(tableName);
    }

    @Override
    public <S> Void visit(Addition addition, S context)
    {
        return visitBinaryExpression(addition, context);
    }

    @Override
    public <S> Void visit(AndExpression andExpression, S context)
    {
        return visitBinaryExpression(andExpression, context);
    }

    @Override
    public <S> Void visit(Between between, S context)
    {
        between.getLeftExpression().accept(this, context);
        between.getBetweenExpressionStart().accept(this, context);
        between.getBetweenExpressionEnd().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(OverlapsCondition overlapsCondition, S context)
    {
        overlapsCondition.getLeft().accept(this, context);
        overlapsCondition.getRight().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(Column tableColumn, S context)
    {
        if (tableColumn.getTable() != null
            && tableColumn.getTable().getName() != null) {
            visit(tableColumn.getTable(), context);
        }
        return null;
    }

    @Override
    public <S> Void visit(Division division, S context)
    {
        return visitBinaryExpression(division, context);
    }

    @Override
    public <S> Void visit(IntegerDivision division, S context)
    {
        return visitBinaryExpression(division, context);
    }

    @Override
    public <S> Void visit(DoubleValue doubleValue, S context)
    {

        return null;
    }

    @Override
    public <S> Void visit(EqualsTo equalsTo, S context)
    {
        return visitBinaryExpression(equalsTo, context);
    }

    @Override
    public <S> Void visit(Function function, S context)
    {
        ExpressionList<?> exprList = function.getParameters();
        if (exprList != null) {
            visit(exprList, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(GreaterThan greaterThan, S context)
    {
        return visitBinaryExpression(greaterThan, context);
    }

    @Override
    public <S> Void visit(GreaterThanEquals greaterThanEquals, S context)
    {
        return visitBinaryExpression(greaterThanEquals, context);
    }

    @Override
    public <S> Void visit(InExpression inExpression, S context)
    {
        inExpression.getLeftExpression().accept(this, context);
        inExpression.getRightExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(IncludesExpression includesExpression, S context)
    {
        includesExpression.getLeftExpression().accept(this, context);
        includesExpression.getRightExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(ExcludesExpression excludesExpression, S context)
    {
        excludesExpression.getLeftExpression().accept(this, context);
        excludesExpression.getRightExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(FullTextSearch fullTextSearch, S context)
    {
        throw new JpqlSyntaxException("MATCH ACCEPT not supported in JQPL");
    }

    @Override
    public <S> Void visit(SignedExpression signedExpression, S context)
    {
        signedExpression.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(IsNullExpression isNullExpression, S context)
    {
        if (isNullExpression.getLeftExpression() != null) {
            isNullExpression.getLeftExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(IsBooleanExpression isBooleanExpression, S context)
    {
        if (isBooleanExpression.getLeftExpression() != null) {
            isBooleanExpression.getLeftExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(JdbcParameter jdbcParameter, S context)
    {

        return null;
    }

    @Override
    public <S> Void visit(LikeExpression likeExpression, S context)
    {
        return visitBinaryExpression(likeExpression, context);
    }

    @Override
    public <S> Void visit(ExistsExpression existsExpression, S context)
    {
        existsExpression.getRightExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(MemberOfExpression memberOfExpression, S context)
    {
        memberOfExpression.getLeftExpression().accept(this, context);
        memberOfExpression.getRightExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(LongValue longValue, S context)
    {

        return null;
    }

    @Override
    public <S> Void visit(MinorThan minorThan, S context)
    {
        return visitBinaryExpression(minorThan, context);
    }

    @Override
    public <S> Void visit(MinorThanEquals minorThanEquals, S context)
    {
        return visitBinaryExpression(minorThanEquals, context);
    }

    @Override
    public <S> Void visit(Multiplication multiplication, S context)
    {
        return visitBinaryExpression(multiplication, context);
    }

    @Override
    public <S> Void visit(NotEqualsTo notEqualsTo, S context)
    {
        return visitBinaryExpression(notEqualsTo, context);
    }

    @Override
    public <S> Void visit(DoubleAnd doubleAnd, S context)
    {
        return visitBinaryExpression(doubleAnd, context);
    }

    @Override
    public <S> Void visit(Contains contains, S context)
    {
        return visitBinaryExpression(contains, context);
    }

    @Override
    public <S> Void visit(ContainedBy containedBy, S context)
    {
        return visitBinaryExpression(containedBy, context);
    }

    @Override
    public <S> Void visit(NullValue nullValue, S context)
    {

        return null;
    }

    @Override
    public <S> Void visit(OrExpression orExpression, S context)
    {
        return visitBinaryExpression(orExpression, context);
    }

    @Override
    public <S> Void visit(XorExpression xorExpression, S context)
    {
        return visitBinaryExpression(xorExpression, context);
    }

    @Override
    public <S> Void visit(StringValue stringValue, S context)
    {

        return null;
    }

    @Override
    public <S> Void visit(Subtraction subtraction, S context)
    {
        return visitBinaryExpression(subtraction, context);
    }

    @Override
    public <S> Void visit(NotExpression notExpr, S context)
    {
        notExpr.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(NextValExpression nextValExpression, S context)
    {
        throw new JpqlSyntaxException("NEXT VALUE FOR is not supported in JPQL");
    }

    @Override
    public <S> Void visit(BitwiseRightShift expr, S context)
    {
        return visitBinaryExpression(expr, context);
    }

    @Override
    public <S> Void visit(BitwiseLeftShift expr, S context)
    {
        return visitBinaryExpression(expr, context);
    }

    public <S> Void visitBinaryExpression(BinaryExpression binaryExpression, S context)
    {
        binaryExpression.getLeftExpression().accept(this, context);
        binaryExpression.getRightExpression().accept(this, context);
        return null;
    }


    @Override
    public <S> Void visit(ExpressionList<?> expressionList, S context)
    {
        for (Expression expression : expressionList) {
            expression.accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(DateValue dateValue, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(TimestampValue timestampValue, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(TimeValue timeValue, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(CaseExpression caseExpression, S context)
    {
        if (caseExpression.getSwitchExpression() != null) {
            caseExpression.getSwitchExpression().accept(this, context);
        }
        if (caseExpression.getWhenClauses() != null) {
            for (WhenClause when : caseExpression.getWhenClauses()) {
                when.accept(this, context);
            }
        }
        if (caseExpression.getElseExpression() != null) {
            caseExpression.getElseExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(WhenClause whenClause, S context)
    {
        if (whenClause.getWhenExpression() != null) {
            whenClause.getWhenExpression().accept(this, context);
        }

        whenClause.getThenExpression().accept(this, context);

        return null;
    }

    @Override
    public <S> Void visit(AnyComparisonExpression anyComparisonExpression, S context)
    {
        anyComparisonExpression.getSelect().accept((ExpressionVisitor<?>) this, context);
        return null;
    }

    @Override
    public <S> Void visit(Concat concat, S context)
    {
        return visitBinaryExpression(concat, context);
    }

    @Override
    public <S> Void visit(Matches matches, S context)
    {
        return visitBinaryExpression(matches, context);
    }

    @Override
    public <S> Void visit(BitwiseAnd bitwiseAnd, S context)
    {
        return visitBinaryExpression(bitwiseAnd, context);
    }

    @Override
    public <S> Void visit(BitwiseOr bitwiseOr, S context)
    {
        return visitBinaryExpression(bitwiseOr, context);
    }

    @Override
    public <S> Void visit(BitwiseXor bitwiseXor, S context)
    {
        return visitBinaryExpression(bitwiseXor, context);
    }

    @Override
    public <S> Void visit(CastExpression cast, S context)
    {
        cast.getLeftExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(Modulo modulo, S context)
    {
        return visitBinaryExpression(modulo, context);
    }

    @Override
    public <S> Void visit(AnalyticExpression analytic, S context)
    {
        if (analytic.getExpression() != null) {
            analytic.getExpression().accept(this, context);
        }
        if (analytic.getDefaultValue() != null) {
            analytic.getDefaultValue().accept(this, context);
        }
        if (analytic.getOffset() != null) {
            analytic.getOffset().accept(this, context);
        }
        if (analytic.getKeep() != null) {
            analytic.getKeep().accept(this, context);
        }
        if (analytic.getFuncOrderBy() != null) {
            for (OrderByElement element : analytic.getOrderByElements()) {
                element.getExpression().accept(this, context);
            }
        }

        if (analytic.getWindowElement() != null) {
            analytic.getWindowElement().getRange().getStart().getExpression().accept(this,
                                                                                     context);
            analytic.getWindowElement().getRange().getEnd().getExpression().accept(this,
                                                                                   context);
            analytic.getWindowElement().getOffset().getExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(SetOperationList list, S context)
    {
        List<WithItem> withItemsList = list.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<?>) this, context);
            }
        }
        for (Select selectBody : list.getSelects()) {
            selectBody.accept((SelectVisitor<?>) this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(ExtractExpression eexpr, S context)
    {
        if (eexpr.getExpression() != null) {
            eexpr.getExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(LateralSubSelect lateralSubSelect, S context)
    {
        throw new JpqlSyntaxException("LATERAL is not supported in JPQL");
    }

    @Override
    public <S> Void visit(TableStatement tableStatement, S context)
    {
        throw new JpqlSyntaxException("TABLE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(IntervalExpression intervalExpression, S context)
    {
        if (intervalExpression.getExpression() != null) {
            intervalExpression.getExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(JdbcNamedParameter jdbcNamedParameter, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(OracleHierarchicalExpression hierarchicalExpression, S context)
    {
        throw new JpqlSyntaxException("CONNECT BY is not supported in JPQL");
    }

    @Override
    public <S> Void visit(RegExpMatchOperator regExpMatchOperator, S context)
    {
        return visitBinaryExpression(regExpMatchOperator, context);
    }

    @Override
    public <S> Void visit(JsonExpression jsonExpr, S context)
    {
        if (jsonExpr.getExpression() != null) {
            jsonExpr.getExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(JsonOperator jsonExpr, S context)
    {
        return visitBinaryExpression(jsonExpr, context);
    }

    @Override
    public <S> Void visit(AllColumns allColumns, S context)
    {
        throw new JpqlSyntaxException("SELECT * FROM is not supported in JPQL");
    }

    @Override
    public <S> Void visit(AllTableColumns allTableColumns, S context)
    {
        throw new JpqlSyntaxException("SELECT * FROM is not supported in JPQL");
    }

    @Override
    public <S> Void visit(AllValue allValue, S context)
    {
        throw new JpqlSyntaxException("SELECT * FROM is not supported in JPQL");
    }

    @Override
    public <S> Void visit(IsDistinctExpression isDistinctExpression, S context)
    {
        return visitBinaryExpression(isDistinctExpression, context);
    }

    @Override
    public <S> Void visit(SelectItem<?> item, S context)
    {
        item.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(UserVariable userVariable, S context)
    {
        throw new JpqlSyntaxException("User variables are not supported in JPQL");
    }

    @Override
    public <S> Void visit(NumericBind numericBind, S context)
    {
        throw new JpqlSyntaxException("Type casting is not supported in JPQL");
    }

    @Override
    public <S> Void visit(KeepExpression keepExpression, S context)
    {
        throw new JpqlSyntaxException("KEEP is not supported in JPQL");
    }

    @Override
    public <S> Void visit(MySQLGroupConcat groupConcat, S context)
    {
        throw new JpqlSyntaxException("GROUP_CONCAT is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Delete delete, S context)
    {
        visit(delete.getTable(), context);

        if (delete.getUsingList() != null) {
            for (Table using : delete.getUsingList()) {
                visit(using, context);
            }
        }

        visitJoins(delete.getJoins(), context);

        if (delete.getWhere() != null) {
            delete.getWhere().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(Update update, S context)
    {
        visit(update.getTable(), context);
        if (update.getWithItemsList() != null) {
            for (WithItem withItem : update.getWithItemsList()) {
                withItem.accept((SelectVisitor<?>) this, context);
            }
        }

        if (update.getStartJoins() != null) {
            for (Join join : update.getStartJoins()) {
                join.getRightItem().accept(this, context);
            }
        }
        if (update.getExpressions() != null) {
            for (Expression expression : update.getExpressions()) {
                expression.accept(this, context);
            }
        }

        if (update.getFromItem() != null) {
            update.getFromItem().accept(this, context);
        }

        if (update.getJoins() != null) {
            for (Join join : update.getJoins()) {
                join.getRightItem().accept(this, context);
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(this, context);
                }
            }
        }

        if (update.getWhere() != null) {
            update.getWhere().accept(this, context);
        }
        return null;
    }


    @Override
    public <S> Void visit(Insert insert, S context)
    {
        throw new JpqlSyntaxException("INSERT is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Analyze analyze, S context)
    {
        throw new JpqlSyntaxException("ANALYZE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Drop drop, S context)
    {
        throw new JpqlSyntaxException("DROP is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Truncate truncate, S context)
    {
        throw new JpqlSyntaxException("TRUNCATE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateIndex createIndex, S context)
    {
        throw new JpqlSyntaxException("CREATE INDEX is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateSchema createSchema, S context)
    {
        throw new JpqlSyntaxException("CREATE SCHEMA is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateTable create, S context)
    {
        throw new JpqlSyntaxException("CREATE TABLE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateView createView, S context)
    {
        throw new JpqlSyntaxException("CREATE VIEW is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Alter alter, S context)
    {
        throw new JpqlSyntaxException("ALTER is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Statements statements, S context)
    {
        for (Statement statement : statements) {
            statement.accept(this, context);
        }
        return null;
    }


    @Override
    public <S> Void visit(Execute execute, S context)
    {
        throw new JpqlSyntaxException("EXECUTE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(SetStatement setStatement, S context)
    {
        throw new JpqlSyntaxException("SET is not supported in JPQL");
    }


    @Override
    public <S> Void visit(ResetStatement reset, S context)
    {
        throw new JpqlSyntaxException("RESET is not supported in JPQL");
    }


    @Override
    public <S> Void visit(ShowColumnsStatement showColumnsStatement, S context)
    {
        throw new JpqlSyntaxException("SHOW is not supported in JPQL");
    }

    @Override
    public <S> Void visit(ShowIndexStatement showIndex, S context)
    {
        throw new JpqlSyntaxException("SHOW is not supported in JPQL");
    }

    @Override
    public <S> Void visit(RowConstructor<?> rowConstructor, S context)
    {
        for (Expression expr : rowConstructor) {
            expr.accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(RowGetExpression rowGetExpression, S context)
    {
        rowGetExpression.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(HexValue hexValue, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(Merge merge, S context)
    {
        throw new JpqlSyntaxException("MERGE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(OracleHint hint, S context)
    {
        throw new JpqlSyntaxException("HINTS are not supported in JPQL");
    }

    @Override
    public <S> Void visit(TableFunction tableFunction, S context)
    {
        visit(tableFunction.getFunction(), context);
        return null;
    }


    @Override
    public <S> Void visit(AlterView alterView, S context)
    {
        throw new JpqlSyntaxException("ALTER is not supported in JPQL");
    }

    @Override
    public <S> Void visit(RefreshMaterializedViewStatement materializedView, S context)
    {
        throw new JpqlSyntaxException("REFRESH is not supported in JPQL");
    }


    @Override
    public <S> Void visit(TimeKeyExpression timeKeyExpression, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(DateTimeLiteralExpression literal, S context)
    {
        return null;
    }

    @Override
    public <S> Void visit(Commit commit, S context)
    {
        throw new JpqlSyntaxException("COMMIT is not supported in JPQL");
    }


    @Override
    public <S> Void visit(Upsert upsert, S context)
    {
        throw new JpqlSyntaxException("UPSERT is not supported in JPQL");
    }

    @Override
    public <S> Void visit(UseStatement use, S context)
    {
        throw new JpqlSyntaxException("USE is not supported in JPQL");
    }


    @Override
    public <S> Void visit(ParenthesedFromItem parenthesis, S context)
    {
        parenthesis.getFromItem().accept(this, context);
        visitJoins(parenthesis.getJoins(), context);
        return null;
    }

    private <S> void visitJoins(List<Join> joins, S context)
    {
        if (joins == null) {
            return;
        }
        for (Join join : joins) {
            join.getFromItem().accept(this, context);
            join.getRightItem().accept(this, context);
            for (Expression expression : join.getOnExpressions()) {
                expression.accept(this, context);
            }
        }
    }

    @Override
    public <S> Void visit(Block block, S context)
    {
        throw new JpqlSyntaxException("BEGIN..END is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Comment comment, S context)
    {
        throw new JpqlSyntaxException("COMMENT is not supported in JPQL");
    }

    @Override
    public <S> Void visit(Values values, S context)
    {
        values.getExpressions().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(DescribeStatement describe, S context)
    {
        throw new JpqlSyntaxException("DESCRIBE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(ExplainStatement explainStatement, S context)
    {
        throw new JpqlSyntaxException("EXPLAIN is not supported in JPQL");
    }

    @Override
    public void visit(ExplainStatement explainStatement)
    {
        StatementVisitor.super.visit(explainStatement);
    }

    @Override
    public <S> Void visit(CollateExpression collateExpression, S context)
    {
        collateExpression.getLeftExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(ShowStatement showStatement, S context)
    {
        throw new JpqlSyntaxException("SHOW is not supported in JPQL");
    }


    @Override
    public <S> Void visit(SimilarToExpression expr, S context)
    {
        visitBinaryExpression(expr, context);
        return null;
    }

    @Override
    public <S> Void visit(DeclareStatement declareStatement, S context)
    {
        throw new JpqlSyntaxException("DECLARE is not supported in JPQL");
    }


    @Override
    public <S> Void visit(Grant grant, S context)
    {
        throw new JpqlSyntaxException("GRANT is not supported in JPQL");
    }

    @Override
    public <S> Void visit(ArrayExpression array, S context)
    {
        array.getObjExpression().accept(this, context);
        if (array.getStartIndexExpression() != null) {
            array.getIndexExpression().accept(this, context);
        }
        if (array.getStartIndexExpression() != null) {
            array.getStartIndexExpression().accept(this, context);
        }
        if (array.getStopIndexExpression() != null) {
            array.getStopIndexExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(ArrayConstructor array, S context)
    {
        for (Expression expression : array.getExpressions()) {
            expression.accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(CreateSequence createSequence, S context)
    {
        throw new JpqlSyntaxException("CREATE SEQUENCE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(AlterSequence alterSequence, S context)
    {
        throw new JpqlSyntaxException("ALTER SEQUENCE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateFunctionalStatement createFunctionalStatement, S context)
    {
        throw new JpqlSyntaxException("CREATE FUNCTION is not supported in JPQL");
    }

    @Override
    public <S> Void visit(ShowTablesStatement showTables, S context)
    {
        throw new JpqlSyntaxException("SHOW TABLE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(TSQLLeftJoin tsqlLeftJoin, S context)
    {
        throw new JpqlSyntaxException("TSQL type expressions are not supported in JPQL");
    }

    @Override
    public <S> Void visit(TSQLRightJoin tsqlRightJoin, S context)
    {
        throw new JpqlSyntaxException("TSQL type expressions are not supported in JPQL");
    }

    @Override
    public <S> Void visit(StructType structType, S context)
    {
        if (structType.getArguments() != null) {
            for (SelectItem<?> selectItem : structType.getArguments()) {
                selectItem.getExpression().accept(this, context);
            }
        }
        return null;
    }

    @Override
    public <S> Void visit(LambdaExpression lambdaExpression, S context)
    {
        lambdaExpression.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(VariableAssignment variableAssignment, S context)
    {
        variableAssignment.getVariable().accept(this, context);
        variableAssignment.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(XMLSerializeExpr xmlSerializeExpr, S context)
    {
        throw new JpqlSyntaxException("XML_SERIALIZE is not supported in JPQL");
    }

    @Override
    public <S> Void visit(CreateSynonym createSynonym, S context)
    {
        throw new JpqlSyntaxException("CREATE SYNONYM is not supported in JPQL");
    }

    @Override
    public <S> Void visit(TimezoneExpression timezoneExpression, S context)
    {
        timezoneExpression.getLeftExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(SavepointStatement savepointStatement, S context)
    {
        throw new JpqlSyntaxException("SAVEPOINT is not supported in JPQL");
    }


    @Override
    public <S> Void visit(RollbackStatement rollbackStatement, S context)
    {
        throw new JpqlSyntaxException("ROLLBACK is not supported in JPQL");
    }

    @Override
    public <S> Void visit(AlterSession alterSession, S context)
    {
        throw new JpqlSyntaxException("ALTER SESSION is not supported in JPQL");
    }


    @Override
    public <S> Void visit(JsonAggregateFunction expression, S context)
    {
        Expression expr = expression.getExpression();
        if (expr != null) {
            expr.accept(this, context);
        }

        expr = expression.getFilterExpression();
        if (expr != null) {
            expr.accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(JsonFunction expression, S context)
    {
        for (JsonFunctionExpression expr : expression.getExpressions()) {
            expr.getExpression().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(ConnectByRootOperator connectByRootOperator, S context)
    {
        connectByRootOperator.getColumn().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(IfElseStatement ifElseStatement, S context)
    {
        ifElseStatement.getIfStatement().accept(this, context);
        if (ifElseStatement.getElseStatement() != null) {
            ifElseStatement.getElseStatement().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter, S context)
    {
        oracleNamedFunctionParameter.getExpression().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(RenameTableStatement renameTableStatement, S context)
    {
        for (Map.Entry<Table, Table> e : renameTableStatement.getTableNames()) {
            e.getKey().accept(this, context);
            e.getValue().accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(PurgeStatement purgeStatement, S context)
    {
        if (purgeStatement.getPurgeObjectType() == PurgeObjectType.TABLE) {
            ((Table) purgeStatement.getObject()).accept(this, context);
        }
        return null;
    }

    @Override
    public <S> Void visit(AlterSystemStatement alterSystemStatement, S context)
    {
        // no tables involved in this statement
        return null;
    }


    @Override
    public <S> Void visit(UnsupportedStatement unsupportedStatement, S context)
    {
        // no tables involved in this statement
        return null;
    }


    @Override
    public <S> Void visit(GeometryDistance geometryDistance, S context)
    {
        return visitBinaryExpression(geometryDistance, context);
    }


    @Override
    public <S> Void visit(GroupByElement groupBy, S context)
    {
        groupBy.getGroupByExpressionList().accept(this, context);
        return null;
    }

    @Override
    public <S> Void visit(OrderByElement orderBy, S context)
    {
        orderBy.getExpression().accept(this, context);
        return null;
    }

    @Override
    public void visit(Analyze analyze)
    {
        StatementVisitor.super.visit(analyze);
    }

    @Override
    public void visit(SavepointStatement savepointStatement)
    {
        StatementVisitor.super.visit(savepointStatement);
    }

    @Override
    public void visit(RollbackStatement rollbackStatement)
    {
        StatementVisitor.super.visit(rollbackStatement);
    }

    @Override
    public void visit(Comment comment)
    {
        StatementVisitor.super.visit(comment);
    }

    @Override
    public void visit(Commit commit)
    {
        StatementVisitor.super.visit(commit);
    }

    @Override
    public void visit(Delete delete)
    {
        StatementVisitor.super.visit(delete);
    }

    @Override
    public void visit(Update update)
    {
        StatementVisitor.super.visit(update);
    }

    @Override
    public void visit(Insert insert)
    {
        StatementVisitor.super.visit(insert);
    }

    @Override
    public void visit(Drop drop)
    {
        StatementVisitor.super.visit(drop);
    }

    @Override
    public void visit(Truncate truncate)
    {
        StatementVisitor.super.visit(truncate);
    }

    @Override
    public void visit(CreateIndex createIndex)
    {
        StatementVisitor.super.visit(createIndex);
    }

    @Override
    public void visit(CreateSchema createSchema)
    {
        StatementVisitor.super.visit(createSchema);
    }

    @Override
    public void visit(CreateTable createTable)
    {
        StatementVisitor.super.visit(createTable);
    }

    @Override
    public void visit(CreateView createView)
    {
        StatementVisitor.super.visit(createView);
    }

    @Override
    public void visit(AlterView alterView)
    {
        StatementVisitor.super.visit(alterView);
    }

    @Override
    public void visit(RefreshMaterializedViewStatement materializedView)
    {
        StatementVisitor.super.visit(materializedView);
    }

    @Override
    public void visit(Alter alter)
    {
        StatementVisitor.super.visit(alter);
    }

    @Override
    public void visit(Statements statements)
    {
        StatementVisitor.super.visit(statements);
    }

    @Override
    public void visit(Execute execute)
    {
        StatementVisitor.super.visit(execute);
    }

    @Override
    public void visit(SetStatement set)
    {
        StatementVisitor.super.visit(set);
    }

    @Override
    public void visit(ResetStatement reset)
    {
        StatementVisitor.super.visit(reset);
    }

    @Override
    public void visit(ShowColumnsStatement showColumns)
    {
        StatementVisitor.super.visit(showColumns);
    }

    @Override
    public void visit(ShowIndexStatement showIndex)
    {
        StatementVisitor.super.visit(showIndex);
    }

    @Override
    public void visit(ShowTablesStatement showTables)
    {
        StatementVisitor.super.visit(showTables);
    }

    @Override
    public void visit(Merge merge)
    {
        StatementVisitor.super.visit(merge);
    }

    @Override
    public void visit(Upsert upsert)
    {
        StatementVisitor.super.visit(upsert);
    }

    @Override
    public void visit(UseStatement use)
    {
        StatementVisitor.super.visit(use);
    }

    @Override
    public void visit(Block block)
    {
        StatementVisitor.super.visit(block);
    }

    @Override
    public void visit(DescribeStatement describe)
    {
        StatementVisitor.super.visit(describe);
    }

    @Override
    public void visit(ShowStatement showStatement)
    {
        StatementVisitor.super.visit(showStatement);
    }

    @Override
    public void visit(DeclareStatement declareStatement)
    {
        StatementVisitor.super.visit(declareStatement);
    }

    @Override
    public void visit(Grant grant)
    {
        StatementVisitor.super.visit(grant);
    }

    @Override
    public void visit(CreateSequence createSequence)
    {
        StatementVisitor.super.visit(createSequence);
    }

    @Override
    public void visit(AlterSequence alterSequence)
    {
        StatementVisitor.super.visit(alterSequence);
    }

    @Override
    public void visit(CreateFunctionalStatement createFunctionalStatement)
    {
        StatementVisitor.super.visit(createFunctionalStatement);
    }

    @Override
    public void visit(CreateSynonym createSynonym)
    {
        StatementVisitor.super.visit(createSynonym);
    }

    @Override
    public void visit(AlterSession alterSession)
    {
        StatementVisitor.super.visit(alterSession);
    }

    @Override
    public void visit(IfElseStatement ifElseStatement)
    {
        StatementVisitor.super.visit(ifElseStatement);
    }

    @Override
    public void visit(RenameTableStatement renameTableStatement)
    {
        StatementVisitor.super.visit(renameTableStatement);
    }

    @Override
    public void visit(PurgeStatement purgeStatement)
    {
        StatementVisitor.super.visit(purgeStatement);
    }

    @Override
    public void visit(AlterSystemStatement alterSystemStatement)
    {
        StatementVisitor.super.visit(alterSystemStatement);
    }

    @Override
    public void visit(UnsupportedStatement unsupportedStatement)
    {
        StatementVisitor.super.visit(unsupportedStatement);
    }

    @Override
    public void visit(TableFunction tableFunction)
    {
        FromItemVisitor.super.visit(tableFunction);
    }

    @Override
    public void visit(ParenthesedFromItem parenthesedFromItem)
    {
        FromItemVisitor.super.visit(parenthesedFromItem);
    }

    @Override
    public void visit(GroupByElement groupBy)
    {
        GroupByVisitor.super.visit(groupBy);
    }

    @Override
    public void visit(OrderByElement orderBy)
    {
        OrderByVisitor.super.visit(orderBy);
    }

    @Override
    public void visit(SelectItem<? extends Expression> selectItem)
    {
        SelectItemVisitor.super.visit(selectItem);
    }

    @Override
    public void visit(PlainSelect plainSelect)
    {
        SelectVisitor.super.visit(plainSelect);
    }

    @Override
    public void visit(SetOperationList setOpList)
    {
        SelectVisitor.super.visit(setOpList);
    }

    @Override
    public void visit(Values values)
    {
        SelectVisitor.super.visit(values);
    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect)
    {
        SelectVisitor.super.visit(lateralSubSelect);
    }

    @Override
    public void visit(TableStatement tableStatement)
    {
        SelectVisitor.super.visit(tableStatement);
    }
}
