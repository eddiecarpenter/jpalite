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
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.statement.values.ValuesStatement;

public class JsqlVistorBase implements StatementVisitor, SelectVisitor, FromItemVisitor, SelectItemVisitor,
									   ExtraExpressionVisitor, GroupByVisitor, ItemsListVisitor, OrderByVisitor
{
	@Override
	public void visit(Select select)
	{
		if (select.getWithItemsList() != null) {
			for (WithItem withItem : select.getWithItemsList()) {
				withItem.accept(this);
			}
		}
		select.getSelectBody().accept(this);
	}

	@Override
	public void visit(PlainSelect plainSelect)
	{
		if (plainSelect.getFromItem() != null) {
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				join.getRightItem().accept(this);
				for (Expression vExpressions : join.getOnExpressions()) {
					vExpressions.accept(this);
				}//for
			}//for
		}//if

		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				item.accept(this);
			}//for
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
		}

		if (plainSelect.getHaving() != null) {
			plainSelect.getHaving().accept(this);
		}

		if (plainSelect.getGroupBy() != null) {
			plainSelect.getGroupBy().accept(this);
		}//if

		if (plainSelect.getOrderByElements() != null) {
			for (OrderByElement element : plainSelect.getOrderByElements()) {
				element.accept(this);
			}
		}//if
	}//visit

	@Override
	public void visit(SetOperationList setOpList)
	{
		for (SelectBody select : setOpList.getSelects()) {
			select.accept(this);
		}//for
	}

	@Override
	public void visit(BitwiseRightShift aThis)
	{
		aThis.accept(this);
	}

	@Override
	public void visit(BitwiseLeftShift aThis)
	{
		aThis.accept(this);
	}

	@Override
	public void visit(NullValue nullValue)
	{
		//Not implemented
	}

	@Override
	public void visit(Function function)
	{
		for (Expression item : function.getParameters().getExpressions()) {
			item.accept(this);
		}//for
	}

	@Override
	public void visit(SignedExpression signedExpression)
	{
		if (signedExpression.getExpression() != null) {
			signedExpression.getExpression().accept(this);
		}//if
	}

	@Override
	public void visit(JdbcParameter jdbcParameter)
	{
		//Not implemented
	}

	@Override
	public void visit(JdbcNamedParameter jdbcNamedParameter)
	{
		//Not implemented
	}

	@Override
	public void visit(BooleanValue aThis)
	{
		//Not Implemented
	}

	@Override
	public void visit(DoubleValue doubleValue)
	{
		//Not implemented
	}

	@Override
	public void visit(LongValue longValue)
	{
		//Not implemented
	}

	@Override
	public void visit(HexValue hexValue)
	{
		//Not implemented
	}

	@Override
	public void visit(DateValue dateValue)
	{
		//Not implemented
	}

	@Override
	public void visit(TimeValue timeValue)
	{
		//Not implemented
	}

	@Override
	public void visit(TimestampValue timestampValue)
	{
		//Not implemented
	}

	@Override
	public void visit(Parenthesis parenthesis)
	{
		parenthesis.getExpression().accept(this);
	}

	@Override
	public void visit(StringValue stringValue)
	{
		//Not implemented
	}

	@Override
	public void visit(Addition addition)
	{
		addition.getLeftExpression().accept(this);
		addition.getRightExpression().accept(this);
	}

	@Override
	public void visit(Division division)
	{
		division.getLeftExpression().accept(this);
		division.getRightExpression().accept(this);
	}

	@Override
	public void visit(IntegerDivision division)
	{
		division.getLeftExpression().accept(this);
		division.getRightExpression().accept(this);
	}

	@Override
	public void visit(Multiplication multiplication)
	{
		multiplication.getLeftExpression().accept(this);
		multiplication.getRightExpression().accept(this);
	}

	@Override
	public void visit(Subtraction subtraction)
	{
		subtraction.getLeftExpression().accept(this);
		subtraction.getRightExpression().accept(this);
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
		orExpression.getLeftExpression().accept(this);
		orExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(XorExpression orExpression)
	{
		orExpression.getLeftExpression().accept(this);
		orExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(Between between)
	{
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	@Override
	public void visit(EqualsTo equalsTo)
	{
		equalsTo.getLeftExpression().accept(this);
		equalsTo.getRightExpression().accept(this);
	}

	@Override
	public void visit(GreaterThan greaterThan)
	{
		greaterThan.getLeftExpression().accept(this);
		greaterThan.getRightExpression().accept(this);
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals)
	{
		greaterThanEquals.getLeftExpression().accept(this);
		greaterThanEquals.getRightExpression().accept(this);
	}

	@Override
	public void visit(InExpression inExpression)
	{
		inExpression.getLeftExpression().accept(this);
		if (inExpression.getRightExpression() == null) {
			inExpression.getRightItemsList().accept(this);
		}//if
		else {
			inExpression.getRightExpression().accept(this);
		}
	}

	@Override
	public void visit(FullTextSearch fullTextSearch)
	{
		throw new IllegalArgumentException("MATCH ACCEPT not supported in JQPL");
	}

	@Override
	public void visit(IsNullExpression isNullExpression)
	{
		isNullExpression.getLeftExpression().accept(this);
	}

	@Override
	public void visit(IsBooleanExpression isBooleanExpression)
	{
		isBooleanExpression.getLeftExpression().accept(this);
	}

	@Override
	public void visit(LikeExpression likeExpression)
	{
		likeExpression.getLeftExpression().accept(this);
		likeExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(MinorThan minorThan)
	{
		minorThan.getLeftExpression().accept(this);
		minorThan.getRightExpression().accept(this);
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals)
	{
		minorThanEquals.getLeftExpression().accept(this);
		minorThanEquals.getRightExpression().accept(this);
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo)
	{
		notEqualsTo.getLeftExpression().accept(this);
		notEqualsTo.getRightExpression().accept(this);
	}

	@Override
	public void visit(Column tableColumn)
	{
		//Not implemented
	}

	@Override
	public void visit(AllColumns allColumns)
	{
		//Not implemented
	}

	@Override
	public void visit(AllTableColumns allTableColumns)
	{
		allTableColumns.accept((SelectItemVisitor) this);
	}

	@Override
	public void visit(AllValue pAllValue)
	{
		//Not implemented
	}

	@Override
	public void visit(IsDistinctExpression pIsDistinctExpression)
	{
		//Not implemented
	}

	@Override
	public void visit(GeometryDistance pGeometryDistance)
	{
		//Not implemented
	}

	@Override
	public void visit(SelectExpressionItem selectExpressionItem)
	{
		//Not implemented
	}

	@Override
	public void visit(Table tableName)
	{
		//Not implemented
	}

	@Override
	public void visit(SubSelect subSelect)
	{
		//ignore
	}

	@Override
	public void visit(ExpressionList expressionList)
	{
		for (Expression expression : expressionList.getExpressions()) {
			expression.accept(this);
		}//for
	}

	@Override
	public void visit(NamedExpressionList namedExpressionList)
	{
		//Not implemented
	}

	@Override
	public void visit(MultiExpressionList multiExprList)
	{
		for (ExpressionList exprList : multiExprList.getExpressionLists()) {
			for (Expression expr : exprList.getExpressions()) {
				expr.accept(this);
			}//for
		}//for
	}

	@Override
	public void visit(CaseExpression caseExpression)
	{
		for (WhenClause clause : caseExpression.getWhenClauses()) {
			clause.accept(this);
		}//if

		if (caseExpression.getElseExpression() != null) {
			caseExpression.getElseExpression().accept(this);
		}//if
	}

	@Override
	public void visit(WhenClause whenClause)
	{
		whenClause.getWhenExpression().accept(this);
	}

	@Override
	public void visit(ExistsExpression existsExpression)
	{
		existsExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(AnyComparisonExpression anyComparisonExpression)
	{
		anyComparisonExpression.accept(this);
	}

	@Override
	public void visit(Concat concat)
	{
		concat.getLeftExpression().accept(this);
		concat.getRightExpression().accept(this);
	}

	@Override
	public void visit(Matches matches)
	{
		matches.getLeftExpression().accept(this);
		matches.getRightExpression().accept(this);
	}

	@Override
	public void visit(BitwiseAnd bitwiseAnd)
	{
		bitwiseAnd.getLeftExpression().accept(this);
		bitwiseAnd.getRightExpression().accept(this);
	}

	@Override
	public void visit(BitwiseOr bitwiseOr)
	{
		bitwiseOr.getLeftExpression().accept(this);
		bitwiseOr.getRightExpression().accept(this);
	}

	@Override
	public void visit(BitwiseXor bitwiseXor)
	{
		bitwiseXor.getLeftExpression().accept(this);
		bitwiseXor.getRightExpression().accept(this);
	}

	@Override
	public void visit(CastExpression cast)
	{
		cast.getLeftExpression().accept(this);
	}

	@Override
	public void visit(TryCastExpression pTryCastExpression)
	{
		//Not implemented
	}

	@Override
	public void visit(Modulo modulo)
	{
		//Not implemented
	}

	@Override
	public void visit(AnalyticExpression aexpr)
	{
		if (aexpr.getExpression() != null) {
			aexpr.getExpression().accept(this);
		}//if
	}

	@Override
	public void visit(ExtractExpression eexpr)
	{
		if (eexpr.getExpression() != null) {
			eexpr.getExpression().accept(this);
		}//if
	}

	@Override
	public void visit(IntervalExpression iexpr)
	{
		if (iexpr.getExpression() != null) {
			iexpr.getExpression().accept(this);
		}//if
	}

	@Override
	public void visit(OracleHierarchicalExpression oexpr)
	{
		//Not implemented
	}

	@Override
	public void visit(RegExpMatchOperator rexpr)
	{
		rexpr.getLeftExpression().accept(this);
		rexpr.getRightExpression().accept(this);
	}

	@Override
	public void visit(JsonExpression jsonExpr)
	{
		jsonExpr.getExpression().accept(this);
	}

	@Override
	public void visit(JsonOperator jsonExpr)
	{
		jsonExpr.getLeftExpression().accept(this);
		jsonExpr.getRightExpression().accept(this);
	}

	@Override
	public void visit(RegExpMySQLOperator regExpMySQLOperator)
	{
		//Not implemented
	}

	@Override
	public void visit(UserVariable userVar)
	{
		//Not implemented
	}

	@Override
	public void visit(NumericBind bind)
	{
		//Not implemented
	}

	@Override
	public void visit(KeepExpression aexpr)
	{
		//Not implemented
	}

	@Override
	public void visit(MySQLGroupConcat groupConcat)
	{
		//Not implemented
	}

	@Override
	public void visit(ValueListExpression valueList)
	{
		valueList.getExpressionList().accept(this);
	}

	@Override
	public void visit(RowConstructor rowConstructor)
	{
		rowConstructor.getExprList().accept(this);
	}

	@Override
	public void visit(RowGetExpression rowGetExpression)
	{
		//Not implemented
	}

	@Override
	public void visit(OracleHint hint)
	{
		//Not implemented
	}

	@Override
	public void visit(TimeKeyExpression timeKeyExpression)
	{
		//Not implemented
	}

	@Override
	public void visit(DateTimeLiteralExpression literal)
	{
		//Not implemented
	}

	@Override
	public void visit(NotExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(NextValExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(CollateExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(SimilarToExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(ArrayExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(ArrayConstructor aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(VariableAssignment aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(XMLSerializeExpr aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(TimezoneExpression aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(JsonAggregateFunction aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(JsonFunction aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(ConnectByRootOperator aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(OracleNamedFunctionParameter aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(SubJoin subjoin)
	{
		subjoin.accept(this);
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect)
	{
		lateralSubSelect.accept(this);
	}

	@Override
	public void visit(ValuesList valuesList)
	{
		valuesList.accept(this);
	}

	@Override
	public void visit(TableFunction tableFunction)
	{
		//Not implemented
	}

	@Override
	public void visit(ParenthesisFromItem aThis)
	{
		aThis.accept(this);
	}

	@Override
	public void visit(Analyze pAnalyze)
	{
		//Not implemented
	}

	@Override
	public void visit(SavepointStatement savepointStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(RollbackStatement rollbackStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(Comment comment)
	{
		//Not implemented
	}

	@Override
	public void visit(Commit commit)
	{
		//Not implemented
	}

	@Override
	public void visit(Delete delete)
	{
		//Not implemented
	}

	@Override
	public void visit(Update update)
	{
		//Not implemented
	}

	@Override
	public void visit(Insert insert)
	{
		//Not implemented
	}

	@Override
	public void visit(Replace replace)
	{
		//Not implemented
	}

	@Override
	public void visit(Drop drop)
	{
		//Not implemented
	}

	@Override
	public void visit(Truncate truncate)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateIndex createIndex)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateSchema aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateTable createTable)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateView createView)
	{
		//Not implemented
	}

	@Override
	public void visit(AlterView alterView)
	{
		//Not implemented
	}

	@Override
	public void visit(Alter alter)
	{
		//Not implemented
	}

	@Override
	public void visit(Statements stmts)
	{
		//Not implemented
	}

	@Override
	public void visit(Execute execute)
	{
		//Not implemented
	}

	@Override
	public void visit(SetStatement set)
	{
		//Not implemented
	}

	@Override
	public void visit(ResetStatement reset)
	{
		//Not implemented
	}

	@Override
	public void visit(ShowColumnsStatement set)
	{
		//Not implemented
	}

	@Override
	public void visit(ShowTablesStatement showTables)
	{
		//Not implemented
	}

	@Override
	public void visit(Merge merge)
	{
		//Not implemented
	}

	@Override
	public void visit(Upsert upsert)
	{
		//Not implemented
	}

	@Override
	public void visit(UseStatement use)
	{
		//Not implemented
	}

	@Override
	public void visit(Block block)
	{
		block.accept(this);
	}

	@Override
	public void visit(WithItem withItem)
	{
		//Not implemented
	}

	@Override
	public void visit(ValuesStatement values)
	{
		//Not implemented
	}

	@Override
	public void visit(DescribeStatement describe)
	{
		//Not implemented
	}

	@Override
	public void visit(ExplainStatement aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(ShowStatement aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(DeclareStatement aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(Grant grant)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateSequence createSequence)
	{
		//Not implemented
	}

	@Override
	public void visit(AlterSequence alterSequence)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateFunctionalStatement createFunctionalStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(CreateSynonym createSynonym)
	{
		//Not implemented
	}

	@Override
	public void visit(AlterSession alterSession)
	{
		//Not implemented
	}

	@Override
	public void visit(IfElseStatement aThis)
	{
		//Not implemented
	}

	@Override
	public void visit(RenameTableStatement renameTableStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(PurgeStatement purgeStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(AlterSystemStatement alterSystemStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(UnsupportedStatement pUnsupportedStatement)
	{
		//Not implemented
	}

	@Override
	public void visit(GroupByElement groupBy)
	{
		groupBy.getGroupByExpressionList().accept(this);
	}

	@Override
	public void visit(OrderByElement orderBy)
	{
		orderBy.getExpression().accept(this);
	}
}
