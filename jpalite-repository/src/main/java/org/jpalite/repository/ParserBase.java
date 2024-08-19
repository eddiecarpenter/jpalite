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

class ParserBase implements StatementVisitor, SelectVisitor, FromItemVisitor, ItemsListVisitor, ExpressionVisitor
{
	@Override
	public void visit(Analyze analyze)
	{
		//ignore
	}

	@Override
	public void visit(SavepointStatement savepointStatement)
	{
		//ignore
	}

	@Override
	public void visit(RollbackStatement rollbackStatement)
	{
		//ignore
	}

	@Override
	public void visit(Comment comment)
	{
		//ignore
	}

	@Override
	public void visit(Commit commit)
	{
		//ignore
	}

	@Override
	public void visit(Delete delete)
	{
		//ignore
	}

	@Override
	public void visit(Update update)
	{
		//ignore
	}

	@Override
	public void visit(Insert insert)
	{
		//ignore
	}

	@Override
	public void visit(Replace replace)
	{
		//ignore
	}

	@Override
	public void visit(Drop drop)
	{
		//ignore
	}

	@Override
	public void visit(Truncate truncate)
	{
		//ignore
	}

	@Override
	public void visit(CreateIndex createIndex)
	{
		//ignore
	}

	@Override
	public void visit(CreateSchema aThis)
	{
		//ignore
	}

	@Override
	public void visit(CreateTable createTable)
	{
		//ignore
	}

	@Override
	public void visit(CreateView createView)
	{
		//ignore
	}

	@Override
	public void visit(AlterView alterView)
	{
		//ignore
	}

	@Override
	public void visit(Alter alter)
	{
		//ignore
	}

	@Override
	public void visit(Statements stmts)
	{
		//ignore
	}

	@Override
	public void visit(Execute execute)
	{
		//ignore
	}

	@Override
	public void visit(SetStatement set)
	{
		//ignore
	}

	@Override
	public void visit(ResetStatement reset)
	{
		//ignore
	}

	@Override
	public void visit(ShowColumnsStatement set)
	{
		//ignore
	}

	@Override
	public void visit(ShowTablesStatement showTables)
	{
		//ignore
	}

	@Override
	public void visit(Merge merge)
	{
		//ignore
	}

	@Override
	public void visit(Select select)
	{
		select.getSelectBody().accept(this);
	}

	@Override
	public void visit(Upsert upsert)
	{
		//ignore
	}

	@Override
	public void visit(UseStatement use)
	{
		//ignore
	}

	@Override
	public void visit(Block block)
	{
		//ignore
	}

	@Override
	public void visit(DescribeStatement describe)
	{
		//ignore
	}

	@Override
	public void visit(ExplainStatement aThis)
	{
		//ignore
	}

	@Override
	public void visit(ShowStatement aThis)
	{
		//ignore
	}

	@Override
	public void visit(DeclareStatement aThis)
	{
		//ignore
	}

	@Override
	public void visit(Grant grant)
	{
		//ignore
	}

	@Override
	public void visit(CreateSequence createSequence)
	{
		//ignore
	}

	@Override
	public void visit(AlterSequence alterSequence)
	{
		//ignore
	}

	@Override
	public void visit(CreateFunctionalStatement createFunctionalStatement)
	{
		//ignore
	}

	@Override
	public void visit(CreateSynonym createSynonym)
	{
		//ignore
	}

	@Override
	public void visit(AlterSession alterSession)
	{
		//ignore
	}

	@Override
	public void visit(IfElseStatement aThis)
	{
		//ignore
	}

	@Override
	public void visit(RenameTableStatement renameTableStatement)
	{
		//ignore
	}

	@Override
	public void visit(PurgeStatement purgeStatement)
	{
		//ignore
	}

	@Override
	public void visit(AlterSystemStatement alterSystemStatement)
	{
		//ignore
	}

	@Override
	public void visit(UnsupportedStatement unsupportedStatement)
	{
		//ignore
	}

	@Override
	public void visit(PlainSelect plainSelect)
	{
		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
		}
	}

	@Override
	public void visit(SetOperationList setOpList)
	{
		//ignore
	}

	@Override
	public void visit(WithItem withItem)
	{
		//ignore
	}

	@Override
	public void visit(ValuesStatement aThis)
	{
		//ignore
	}

	@Override
	public void visit(BitwiseRightShift aThis)
	{
		//ignore
	}

	@Override
	public void visit(BitwiseLeftShift aThis)
	{
		//ignore
	}

	@Override
	public void visit(NullValue nullValue)
	{
		//ignore
	}

	@Override
	public void visit(Function function)
	{
		//ignore
	}

	@Override
	public void visit(SignedExpression signedExpression)
	{
		//ignore
	}

	@Override
	public void visit(JdbcParameter jdbcParameter)
	{
		//ignore
	}

	@Override
	public void visit(JdbcNamedParameter jdbcNamedParameter)
	{
		//ignore
	}

	@Override
	public void visit(DoubleValue doubleValue)
	{
		//ignore
	}

	@Override
	public void visit(LongValue longValue)
	{
		//ignore
	}

	@Override
	public void visit(HexValue hexValue)
	{
		//ignore
	}

	@Override
	public void visit(DateValue dateValue)
	{
		//ignore
	}

	@Override
	public void visit(TimeValue timeValue)
	{
		//ignore
	}

	@Override
	public void visit(TimestampValue timestampValue)
	{
		//ignore
	}

	@Override
	public void visit(Parenthesis parenthesis)
	{
		//ignore
	}

	@Override
	public void visit(StringValue stringValue)
	{
		//ignore
	}

	@Override
	public void visit(Addition addition)
	{
		addition.getLeftExpression().accept(this);
		addition.getRightExpression().accept(this);
		//ignore
	}

	@Override
	public void visit(Division division)
	{
		//ignore
	}

	@Override
	public void visit(IntegerDivision division)
	{
		//ignore
	}

	@Override
	public void visit(Multiplication multiplication)
	{
		//ignore
	}

	@Override
	public void visit(Subtraction subtraction)
	{
		//ignore
	}

	@Override
	public void visit(AndExpression andExpression)
	{
		//ignore
	}

	@Override
	public void visit(OrExpression orExpression)
	{
		//ignore
	}

	@Override
	public void visit(XorExpression orExpression)
	{
		//ignore
	}

	@Override
	public void visit(Between between)
	{
		//ignore
	}

	@Override
	public void visit(EqualsTo equalsTo)
	{
		//ignore
	}

	@Override
	public void visit(GreaterThan greaterThan)
	{
		//ignore
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals)
	{
		//ignore
	}

	@Override
	public void visit(InExpression inExpression)
	{
		//ignore
	}

	@Override
	public void visit(FullTextSearch fullTextSearch)
	{
		//ignore
	}

	@Override
	public void visit(IsNullExpression isNullExpression)
	{
		//ignore
	}

	@Override
	public void visit(IsBooleanExpression isBooleanExpression)
	{
		//ignore
	}

	@Override
	public void visit(LikeExpression likeExpression)
	{
		//ignore
	}

	@Override
	public void visit(MinorThan minorThan)
	{
		//ignore
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals)
	{
		//ignore
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo)
	{
		//ignore
	}

	@Override
	public void visit(Column tableColumn)
	{
		//ignore
	}

	@Override
	public void visit(CaseExpression caseExpression)
	{
		//ignore
	}

	@Override
	public void visit(WhenClause whenClause)
	{
		//ignore
	}

	@Override
	public void visit(ExistsExpression existsExpression)
	{
		//ignore
	}

	@Override
	public void visit(AnyComparisonExpression anyComparisonExpression)
	{
		//ignore
	}

	@Override
	public void visit(Concat concat)
	{
		//ignore
	}

	@Override
	public void visit(Matches matches)
	{
		//ignore
	}

	@Override
	public void visit(BitwiseAnd bitwiseAnd)
	{
		//ignore
	}

	@Override
	public void visit(BitwiseOr bitwiseOr)
	{
		//ignore
	}

	@Override
	public void visit(BitwiseXor bitwiseXor)
	{
		//ignore
	}

	@Override
	public void visit(CastExpression cast)
	{
		//ignore
	}

	@Override
	public void visit(TryCastExpression cast)
	{
		//ignore
	}

	@Override
	public void visit(Modulo modulo)
	{
		//ignore
	}

	@Override
	public void visit(AnalyticExpression aexpr)
	{
		//ignore
	}

	@Override
	public void visit(ExtractExpression eexpr)
	{
		//ignore
	}

	@Override
	public void visit(IntervalExpression iexpr)
	{
		//ignore
	}

	@Override
	public void visit(OracleHierarchicalExpression oexpr)
	{
		//ignore
	}

	@Override
	public void visit(RegExpMatchOperator rexpr)
	{
		//ignore
	}

	@Override
	public void visit(JsonExpression jsonExpr)
	{
		//ignore
	}

	@Override
	public void visit(JsonOperator jsonExpr)
	{
		//ignore
	}

	@Override
	public void visit(RegExpMySQLOperator regExpMySQLOperator)
	{
		//ignore
	}

	@Override
	public void visit(UserVariable uservar)
	{
		//ignore
	}

	@Override
	public void visit(NumericBind bind)
	{
		//ignore
	}

	@Override
	public void visit(KeepExpression aexpr)
	{
		//ignore
	}

	@Override
	public void visit(MySQLGroupConcat groupConcat)
	{
		//ignore
	}

	@Override
	public void visit(ValueListExpression valueList)
	{
		//ignore
	}

	@Override
	public void visit(RowConstructor rowConstructor)
	{
		//ignore
	}

	@Override
	public void visit(RowGetExpression rowGetExpression)
	{
		//ignore
	}

	@Override
	public void visit(OracleHint hint)
	{
		//ignore
	}

	@Override
	public void visit(TimeKeyExpression timeKeyExpression)
	{
		//ignore
	}

	@Override
	public void visit(DateTimeLiteralExpression literal)
	{
		//ignore
	}

	@Override
	public void visit(NotExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(NextValExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(CollateExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(SimilarToExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(ArrayExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(ArrayConstructor aThis)
	{
		//ignore
	}

	@Override
	public void visit(VariableAssignment aThis)
	{
		//ignore
	}

	@Override
	public void visit(XMLSerializeExpr aThis)
	{
		//ignore
	}

	@Override
	public void visit(TimezoneExpression aThis)
	{
		//ignore
	}

	@Override
	public void visit(JsonAggregateFunction aThis)
	{
		//ignore
	}

	@Override
	public void visit(JsonFunction aThis)
	{
		//ignore
	}

	@Override
	public void visit(ConnectByRootOperator aThis)
	{
		//ignore
	}

	@Override
	public void visit(OracleNamedFunctionParameter aThis)
	{
		//ignore
	}

	@Override
	public void visit(AllColumns allColumns)
	{
		//ignore
	}

	@Override
	public void visit(AllTableColumns allTableColumns)
	{
		//ignore
	}

	@Override
	public void visit(AllValue allValue)
	{
		//ignore
	}

	@Override
	public void visit(IsDistinctExpression isDistinctExpression)
	{
		//ignore
	}

	@Override
	public void visit(GeometryDistance geometryDistance)
	{
		//ignore
	}

	@Override
	public void visit(ExpressionList expressionList)
	{
		expressionList.getExpressions().stream()
					  .forEach(e -> e.accept(this));
	}

	@Override
	public void visit(NamedExpressionList namedExpressionList)
	{
		//ignore
	}

	@Override
	public void visit(MultiExpressionList multiExprList)
	{
		//ignore
	}

	@Override
	public void visit(Table tableName)
	{
		//ignore
	}

	@Override
	public void visit(SubSelect subSelect)
	{
		//ignore
	}

	@Override
	public void visit(SubJoin subjoin)
	{
		//ignore
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect)
	{
		//ignore
	}

	@Override
	public void visit(ValuesList valuesList)
	{
		//ignore
	}

	@Override
	public void visit(TableFunction tableFunction)
	{
		//ignore
	}

	@Override
	public void visit(ParenthesisFromItem aThis)
	{
		//ignore
	}
}
