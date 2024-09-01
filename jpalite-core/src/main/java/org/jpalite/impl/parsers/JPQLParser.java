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


import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.jpalite.*;
import org.jpalite.impl.queries.QueryParameterImpl;
import org.jpalite.parsers.QueryParser;
import org.jpalite.parsers.QueryStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S1452") //generic wildcard is required
public class JPQLParser extends JPQLAdaptor implements QueryParser
{
    private enum Context
    {
        STATEMENT,
        FROM,
        JOIN,
        SELECT,
        SUB_SELECT,
        WHERE,
        GROUP_BY,
        HAVING,
        ORDER_BY
    }

    /**
     * The parsed query
     */
    private final String query;
    private QueryStatement queryStatement = QueryStatement.OTHER;

    /**
     * Starting number to generate unique tables aliases
     */
    private int tableNr = 1;
    /**
     * List of return types
     */
    private final Map<String, Class<?>> returnTypes;
    /**
     * List of tables used
     */
    private final List<EntityInfo> entityInfoList;
    /**
     * We may use either positional or named parameters, but we cannot mix them within the same query.
     */
    private boolean usingNamedParameters;
    /**
     * Map of parameters used in the query
     */
    private final List<QueryParameterImpl<?>> queryParameters;
    /**
     * Instance of the defined joins in the query
     */
    private List<Join> joins;
    /**
     * State variable used to indicate that in section we are processing
     */
    // private Phase currentPhase = Phase.FROM;
    /**
     * The "from" table in the select statement
     */
    private Table fromTable = null;
    /**
     * If not null the fetchtype settings on the basic fields are ignored and this value is used
     */
    private FetchType overrideBasicFetchType = null;
    /**
     * If not null the fetchtype settings on the ALL fields are ignored and this value is used
     */
    private FetchType overrideAllFetchType = null;
    private boolean selectUsingPrimaryKey = false;
    private String tableAlias = null;

    public class EntityInfo
    {
        private final List<String> aliases;
        private final EntityMetaData<?> metadata;
        private final String tableAlias;

        public EntityInfo(String alias, EntityMetaData<?> metaData)
        {
            aliases = new ArrayList<>();
            aliases.add(alias);
            metadata   = metaData;
            tableAlias = "t" + tableNr;
            tableNr++;
        }

        public EntityInfo(String alias, EntityMetaData<?> metaData, String tableAlias)
        {
            aliases = new ArrayList<>();
            aliases.add(alias);
            metadata        = metaData;
            this.tableAlias = tableAlias;
        }

        @Override
        public String toString()
        {
            return aliases.getFirst() + "->" + metadata + ", " + metadata.getTable() + " " + tableAlias;
        }

        public String getColumnAlias()
        {
            return aliases.getFirst();
        }//getColumnAlias

        public void addColAlias(String alias)
        {
            aliases.add(alias);
        }

        public boolean containsEntityAlias(String alias)
        {
            return aliases.contains(alias);
        }

        public String getTableAlias()
        {
            return tableAlias;
        }

        public EntityMetaData<?> getMetadata()
        {
            return metadata;
        }
    }//EntityInfo

    /**
     * Constructor for the class. The method takes as input a JQPL Statement and convert it to a Native Statement. Note
     * that the original pStatement is modified
     *
     * @param rawQuery   The JQPL query
     * @param queryHints The query hints
     */
    public JPQLParser(String rawQuery, Map<String, Object> queryHints)
    {
        returnTypes          = new LinkedHashMap<>();
        entityInfoList       = new ArrayList<>();
        usingNamedParameters = false;
        queryParameters      = new ArrayList<>();

        if (queryHints.get(JPALiteEntityManager.PERSISTENCE_OVERRIDE_FETCHTYPE) != null) {
            overrideAllFetchType = (FetchType) queryHints.get(JPALiteEntityManager.PERSISTENCE_OVERRIDE_FETCHTYPE);
        }//if

        if (queryHints.get(JPALiteEntityManager.PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE) != null) {
            overrideBasicFetchType = (FetchType) queryHints.get(JPALiteEntityManager.PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE);
        }//if

        try {
            Statement vStatement = CCJSqlParserUtil.parse(rawQuery);
            vStatement.accept(this, Context.STATEMENT);
            query = vStatement.toString().replace(":?", "?");
            entityInfoList.clear();
        }//try
        catch (JSQLParserException ex) {
            throw new PersistenceException("Error parsing query", ex);
        }//catch
    }//JpqlToNative

    @Override
    public boolean isSelectUsingPrimaryKey()
    {
        return selectUsingPrimaryKey;
    }//isSelectUsingPrimaryKey

    EntityInfo findEntityInfoWithTableAlias(String tableAlias)
    {
        for (EntityInfo vInfo : entityInfoList) {
            if (vInfo.getTableAlias().equals(tableAlias)) {
                return vInfo;
            }//if
        }//for
        return null;
    }//findEntityInfoWithTableAlias

    EntityInfo findEntityInfoWithColAlias(String colAlias)
    {
        for (EntityInfo vInfo : entityInfoList) {
            if (vInfo.containsEntityAlias(colAlias)) {
                return vInfo;
            }//if
        }//for
        return null;
    }//findEntityInfoWithColAlias

    EntityInfo findEntityInfoWithEntity(Class<?> entityClass)
    {
        for (EntityInfo info : entityInfoList) {
            if (info.getMetadata().getEntityClass().equals(entityClass)) {
                return info;
            }//if
        }//for
        return null;
    }//findEntityInfoWithEntity

    @Override
    public QueryStatement getStatement()
    {
        return queryStatement;
    }

    /**
     * Return the Native query
     *
     * @return the SQL query
     */
    @Override
    public String getQuery()
    {
        return query;
    }//getNativeStatement

    /**
     * Return the type of parameter that is used.
     *
     * @return True if using named parameters
     */
    @Override
    public boolean isUsingNamedParameters()
    {
        return usingNamedParameters;
    }//isUsingNamedParameters

    @Override
    public int getNumberOfParameters()
    {
        return queryParameters.size();
    }//getNumberOfParameters

    /**
     * Return a map of the query parameters defined.
     *
     * @return The query parameters
     */
    @Override
    public List<QueryParameterImpl<?>> getQueryParameters()
    {
        return queryParameters;
    }

    /**
     * Return a list of all the return type in the select
     *
     * @return the list
     */
    @Override
    public List<Class<?>> getReturnTypes()
    {
        return new ArrayList<>(returnTypes.values());
    }//getReturnTypes

    /**
     * Check if the given return type is provided by the JQPL guery. If not an IllegalArgumentException exception is
     * generated
     *
     * @param typeToCheck The class to check
     * @throws IllegalArgumentException if the type is not provided
     */
    @Override
    public void checkType(Class<?> typeToCheck)
    {
        if (queryStatement == QueryStatement.SELECT) {
            if (!typeToCheck.isArray()) {
                if (returnTypes.size() > 1) {
                    throw new IllegalArgumentException("Type specified for Query [" + typeToCheck.getName() + "] does not support multiple result set.");
                }//if
                if (!returnTypes.get("c1").isAssignableFrom(typeToCheck)) {
                    throw new IllegalArgumentException("Type specified for Query [" + typeToCheck.getName() + "] is incompatible with query return type [" + returnTypes.get("c1").getName() + "]");
                }//if
            }//if
            else {
                if (typeToCheck != byte[].class && typeToCheck != Object[].class) {
                    throw new IllegalArgumentException("Cannot create TypedQuery for query with more than one return using requested result type " + typeToCheck.arrayType().getName() + "[]");
                }//if
            }//else
        }//if
    }//checkType

    private <S> void joinAccept(Join join, S context)
    {
        if (!join.getOnExpressions().isEmpty()) {
            throw new IllegalArgumentException("JOIN ON is not supported in JQPL - " + join);
        }//if

        //JOIN <schema>.<table> <alias> eg e.department d
        Table joinTable = (Table) join.getRightItem();
        String joinField = joinTable.getName(); //<table>=department
        String fromEntity = joinTable.getSchemaName(); //<schema>=e

        String joinAlias;
        if (joinTable.getAlias() != null) {
            joinAlias = joinTable.getAlias().getName(); // <alias>=d
        }//if
        else {
            //No Alias was set. Make it the same as the schema.table value
            joinAlias = joinTable.getFullyQualifiedName(); //<schema>.<table>=e.department
            joinTable.setAlias(new Alias(joinAlias, false));
        }//else

        joinTable.accept(this, context);
        EntityInfo joinEntityInfo = findEntityInfoWithColAlias(joinAlias); // <alias>=d or <schema>.<table>=e.department

        /*
         * If the schema name is not present we are busy with a new Cartesian style join
         * eg select d from Employee e, Department d where ...
         * This case we just process the table
         */
        if (fromEntity != null) {
            EntityInfo fromEntityInfo = findEntityInfoWithColAlias(fromEntity);
            EntityField fromEntityField = fromEntityInfo.getMetadata().getEntityField(joinField); //<table>=department
            EntityField joinEntityField;
            if (fromEntityField.getMappedBy() != null) {
                joinEntityField = joinEntityInfo.getMetadata().getEntityField(fromEntityField.getMappedBy());
                if (fromEntityInfo.getMetadata().hasMultipleIdFields()) {
                    throw new IllegalArgumentException("Cannot JOIN on multiple id fields");
                }//if
                fromEntityField = fromEntityInfo.getMetadata().getIdField();
            }//if
            else {
                if (joinEntityInfo.getMetadata().hasMultipleIdFields()) {
                    throw new IllegalArgumentException("Cannot JOIN on multiple id fields");
                }//if
                joinEntityField = joinEntityInfo.getMetadata().getIdField();
            }//else

            BinaryExpression expression = new EqualsTo();
            expression.setLeftExpression(new Column(new Table(fromEntityInfo.getTableAlias()), fromEntityField.getColumn()));
            expression.setRightExpression(new Column(new Table(joinEntityInfo.getTableAlias()), joinEntityField.getColumn()));
            join.getOnExpressions().add(expression);

            if (fromEntityField.getMappingType() == MappingType.MANY_TO_ONE || fromEntityField.getMappingType() == MappingType.ONE_TO_ONE) {
                join.withInner(!fromEntityField.isNullable())
                    .withLeft(fromEntityField.isNullable())
                    .withRight(false)
                    .withOuter(false)
                    .withCross(false)
                    .withFull(false)
                    .withStraight(false)
                    .withNatural(false);
            }//if
        }//if
    }//joinAccept

    private <S> void addJoin(Table table, S context)
    {
        Join join = new Join();
        join.setInner(true);
        join.setRightItem(table);

        joins.add(join);
        joinAccept(join, context);
    }//addJoin

    private EntityInfo findMappedBy(String fieldName)
    {
        for (EntityInfo info : entityInfoList) {
            for (EntityField vField : info.getMetadata().getEntityFields()) {
                if (fieldName.equals(vField.getMappedBy())) {
                    //Yes, we have winner :-)
                    return info;
                }//if
            }//for
        }//for
        return null;
    }//findMappedBy

    private <S> void expandEntity(boolean root, EntityMetaData<?> entity, String selectNr, String colAlias, EntityField entityField, String tableAlias, List<SelectItem<? extends Expression>> newList, S context)
    {
        String newTableAlias = tableAlias + "." + entityField.getName();
        if (!root) {
            colAlias += "-" + entityField.getFieldNr();
        }//if

        //only XXXX_TO_ONE type mappings can be expanded
        if (entityField.getMappingType() == MappingType.ONE_TO_ONE || entityField.getMappingType() == MappingType.MANY_TO_ONE) {
            //Check if we already have a JOIN for the entity
            EntityInfo entityInfo = findEntityInfoWithEntity(entityField.getType());
            //We will expand if FetchType is EAGER or if we have an existing JOIN on the Entity
            if (entityInfo != null || (overrideAllFetchType != null && overrideAllFetchType == FetchType.EAGER) || (overrideAllFetchType == null && entityField.getFetchType() == FetchType.EAGER)) {
                if (entityInfo == null) {
                    //if where have many-to-one mapping on the field, check if one of the other tables ( FROM and JOIN) have an ONE_TO_MANY link
                    //back to this entity
                    if (entityField.getMappingType() == MappingType.MANY_TO_ONE) {
                        EntityInfo info = findMappedBy(entityField.getName());
                        if (info != null) {
                            getAllColumns(selectNr, colAlias, info.getMetadata(), info.getColumnAlias(), newList, context);
                            return;
                        }//if
                    }//if

                    Table table = new Table(tableAlias, entityField.getName());
                    table.setAlias(new Alias(tableAlias + "." + entityField.getName(), false));
                    addJoin(table, context);
                    entityInfo = findEntityInfoWithEntity(entityField.getType());
                }//if
                else {
                    if (!entityInfo.containsEntityAlias(newTableAlias)) {
                        entityInfo.addColAlias(newTableAlias);
                    }//if
                }//else

                getAllColumns(selectNr, colAlias, entityInfo.getMetadata(), newTableAlias, newList, context);
            }//if
            else {
                newList.add(createSelectColumn(entityField.getName(), selectNr + colAlias, tableAlias));
            }//else
        }//if
        else if (entityField.getMappingType() == MappingType.EMBEDDED) {
            EntityInfo entityInfo = findEntityInfoWithTableAlias(newTableAlias);
            if (entityInfo == null) {
                EntityInfo parentEntityInfo = findEntityInfoWithEntity(entity.getEntityClass());
                EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityField.getType());
                entityInfo = new EntityInfo(tableAlias + "." + entityField.getName(), metaData, parentEntityInfo.getTableAlias());
                entityInfoList.add(entityInfo);
            }//if

            getAllColumns(selectNr, colAlias, entityInfo.getMetadata(), newTableAlias, newList, context);
        }//else
    }//expandEntity

    private SelectItem<Column> createSelectColumn(String field, String colField, String tableAlias)
    {
        Column newColumn = createColumn(field, tableAlias);
        SelectItem<Column> newItem = new SelectItem<>(newColumn);
        if (colField != null) {
            newItem.setAlias(new Alias("\"" + colField + "\"", false));
        }//if
        return newItem;
    }//createSelectColumn

    private Column createColumn(String field, String tableAlias)
    {
        Table table = new Table();
        String[] parts = tableAlias.split("\\.");
        if (parts.length > 1) {
            table.setSchemaName(parts[0]);
            table.setName(tableAlias.substring(parts[1].length() + 2));
        }//if
        else {
            table.setName(parts[0]);
        }//else

        table.setAlias(new Alias(tableAlias, false));
        return new Column(table, field);
    }//createColumn

    private <S> void getAllColumns(String selectNr, String colAlias, EntityMetaData<?> entity, String tableAlias, List<SelectItem<? extends Expression>> newList, S context)
    {
        for (EntityField field : entity.getEntityFields()) {
            if (field.getMappingType() == MappingType.BASIC) {
                if (field.isIdField() || (overrideBasicFetchType != null && overrideBasicFetchType == FetchType.EAGER) || (overrideBasicFetchType == null && field.getFetchType() == FetchType.EAGER)) {
                    newList.add(createSelectColumn(field.getName(), selectNr + colAlias + "-" + field.getFieldNr(), tableAlias));
                }//if
            }//if
            else {
                expandEntity(false, entity, selectNr, colAlias, field, tableAlias, newList, context);
            }//else
        }//for
    }//getAllColumns

    private <S> EntityInfo findEntity(String selectPath, S context)
    {
        EntityInfo entityInfo = findEntityInfoWithColAlias(selectPath);
        if (entityInfo != null) {
            return entityInfo;
        }//if

        int vDot = selectPath.lastIndexOf(".");
        if (vDot == -1) {
            throw new IllegalStateException("Invalid fields specified");
        }//if

        String path = selectPath.substring(0, vDot);
        String field = selectPath.substring(vDot + 1);
        entityInfo = findEntityInfoWithColAlias(path);
        if (entityInfo == null) {
            entityInfo = findEntity(path, context);
        }//if

        EntityField entityField = entityInfo.getMetadata().getEntityField(field);
        if (entityField.getMappingType() == MappingType.EMBEDDED) {
            EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityField.getType());
            entityInfo = new EntityInfo(path + "." + entityField.getName(), metaData, entityInfo.getTableAlias());
            entityInfoList.add(entityInfo);
        }//if
        else {
            Table table = new Table(path, field);
            table.setAlias(new Alias(path + "." + entityField.getName(), false));
            addJoin(table, context);
        }//else

        return findEntityInfoWithColAlias(selectPath);
    }//findEntity

    private <S> void processSelectItem(String colLabel, SelectItem<? extends Expression> selectItem, List<SelectItem<? extends Expression>> newList, S context)
    {
        /*
         * case 1. select e from Employee e
         * Only one select item, selecting specifically the entity
         * <p>
         * case 2. select e.name, e.department from Employee e
         * Only one or more items from the entity
         * The fields can either be entity, embedded class or basic field.
         * <p>
         * processSelectItem() will be called for each item found
         */
        selectItem.setAlias(new Alias(colLabel, false));
        if (selectItem.getExpression() instanceof Column column) {
            if ("NEW".equalsIgnoreCase(column.getColumnName())) {
                throw new IllegalArgumentException("JPQL Constructor Expressions are not supported - " + column);
            }//if

            //Check if we are working with a full entity or a field in an entity
            if (column.getTable() == null) {
                /*
                 * We will get here for any field being specified. eg select e.name | select e.department | select e.department.name
                 */

                EntityInfo entityInfo = findEntityInfoWithColAlias(column.getColumnName());
                if (entityInfo == null) {
                    throw new IllegalArgumentException("Unknown column - " + column);
                }//if


                addResultType(colLabel, entityInfo.getMetadata().getEntityClass(), context);
                getAllColumns(colLabel, "", entityInfo.getMetadata(), column.getColumnName(), newList, context);
            }//if
            else {
                /*
                 * We will get here for selectItem where a field was specified. Eg select e.department from Employee e
                 */
                String fieldName = column.getColumnName();
                String fullPath = column.getTable().getFullyQualifiedName();

                //Find the Entity from the path
                EntityInfo entityInfo = findEntity(fullPath, context);
                EntityField entityField = entityInfo.getMetadata().getEntityField(fieldName);
                addResultType(colLabel, entityField.getType(), context);

                if (entityField.getMappingType() == MappingType.BASIC) {
                    newList.add(createSelectColumn(entityField.getName(), colLabel, fullPath));
                }//if
                else {
                    expandEntity(true, entityInfo.getMetadata(), colLabel, "", entityField, fullPath, newList, context);
                }//else
            }//else
        }//if
        else {
            selectItem.setAlias(new Alias("\"" + colLabel + "\"", false));
            newList.add(selectItem);
        }//else
    }//processSelectItem

    @SuppressWarnings("unchecked")
    private <S> void processSelectItems(List<SelectItem<? extends Expression>> selectItems, List<SelectItem<? extends Expression>> newList, S context)
    {
        for (int nr = 0; nr < selectItems.size(); nr++) {
            String colLabel = "c" + (nr + 1);
            SelectItem<?> item = selectItems.get(nr);
            processSelectItem(colLabel, item, newList, context);
        }//for
    }//processSelectItems

    private <S> void processUpdateSet(List<UpdateSet> updateSets, S context)
    {
        for (UpdateSet item : updateSets) {
            ExpressionList<Column> newColList;
            if (item.getColumns() instanceof ParenthesedExpressionList) {
                newColList = new ParenthesedExpressionList<>();
            }
            else {
                newColList = new ExpressionList<>();
            }
            for (Column column : item.getColumns()) {
                if (column.getTable() == null) {
                    column.setTable(new Table("X"));
                }//if
                String fieldName = column.getColumnName();
                String fullPath = column.getTable().getFullyQualifiedName();
                EntityInfo entityInfo = findEntity(fullPath, context);
                EntityField entityField = entityInfo.getMetadata().getEntityField(fieldName);
                if (entityField.getMappingType() == MappingType.EMBEDDED) {
                    throw new PersistenceException("Embedded field are not supported in update sets");
                }//if
                else {
                    Column newCol = createColumn(fieldName, fullPath);
                    newColList.add(newCol);
                }//if
            }//for
            item.setColumns(newColList);
        }//for
    }//processUpdateSet

    @Override
    public <S> Void visit(Update update, S context)
    {
        queryStatement = QueryStatement.UPDATE;
        if (update.getTable().getAlias() == null) {
            update.getTable().setAlias(new Alias("X", false));
            fromTable = new Table(update.getTable().getName());
            fromTable.setAlias(new Alias("X", false));
        }//if

        update.getTable().accept(this, context);

        processUpdateSet(update.getUpdateSets(), context);
        for (UpdateSet updateSet : update.getUpdateSets()) {
            for (Column column : updateSet.getColumns()) {
                column.accept(this, Context.SELECT);
            }//for
            for (Expression expression : updateSet.getValues()) {
                expression.accept(this, Context.SELECT);
            }//for
        }//for

        if (update.getWhere() != null) {
            update.getWhere().accept(this, Context.WHERE);
        }//if

        return null;
    }//visit

    @Override
    public <S> Void visit(Delete delete, S context)
    {
        queryStatement = QueryStatement.DELETE;
        if (delete.getTable().getAlias() == null) {
            tableAlias = delete.getTable().getName();
            delete.getTable().setAlias(new Alias(tableAlias, false));
            fromTable = new Table(delete.getTable().getName());
            fromTable.setAlias(new Alias(delete.getTable().getName(), false));
        }//if

        delete.getTable().accept(this, context);

        if (delete.getWhere() != null) {
            delete.getWhere().accept(this, Context.WHERE);
        }//if

        return null;
    }

    @Override
    public <S> Void visit(Insert insert, S context)
    {
        queryStatement = QueryStatement.INSERT;
        throw new PersistenceException("INSERT queries are not valid in JPQL");
    }

    private <S> void addResultType(String column, Class<?> classType, S context)
    {
        if (context == Context.SELECT) {
            returnTypes.put(column, classType);
        }//if
    }

    @Override
    public <S> Void visit(PlainSelect plainSelect, S context)
    {
        queryStatement = QueryStatement.SELECT;
        if (plainSelect.getFromItem() instanceof Table table) {
            if (table.getAlias() == null) {
                tableAlias = table.getName();
                table.setAlias(new Alias(table.getName(), false));
            }//if
            fromTable = new Table(table.getName());
            fromTable.setAlias(new Alias(table.getAlias().getName(), false));

            plainSelect.getFromItem().accept(this, Context.FROM);
        }

        if (plainSelect.getJoins() == null) {
            joins = new ArrayList<>();
            plainSelect.setJoins(joins);
        }//if
        else {
            joins = plainSelect.getJoins();
        }//else

        for (Join join : joins) {
            joinAccept(join, Context.JOIN);
        }//for

        if (plainSelect.getSelectItems() != null) {
            List<SelectItem<?>> selectItemList = plainSelect.getSelectItems();
            List<SelectItem<?>> newList = new ArrayList<>();
            plainSelect.setSelectItems(newList);
            processSelectItems(selectItemList, newList, context == Context.STATEMENT ? Context.SELECT : Context.SUB_SELECT);
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                item.accept(this, context == Context.STATEMENT ? Context.SELECT : Context.SUB_SELECT);
            }//for
        }

        selectUsingPrimaryKey = false; //Catch the case where there are no WHERE clause
        if (plainSelect.getWhere() != null) {
            //Set to true, if a tableColumn referencing a non-ID field is found it will be changed to false
            selectUsingPrimaryKey = true;
            plainSelect.getWhere().accept(this, Context.WHERE);
        }

        if (plainSelect.getHaving() != null) {
            plainSelect.getHaving().accept(this, Context.HAVING);
        }

        if (plainSelect.getGroupBy() != null) {
            plainSelect.getGroupBy().accept(this, Context.GROUP_BY);
        }//if

        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement vElement : plainSelect.getOrderByElements()) {
                vElement.accept(this, Context.ORDER_BY);
            }
        }//if

        return null;
    }//visitPlainSelect

    private <X> void addQueryParameter(Expression expression, Class<X> parameterType)
    {
        if (expression instanceof JdbcParameter parameter) {
            if (queryParameters.isEmpty()) {
                usingNamedParameters = false;
            }//if
            else if (usingNamedParameters) {
                throw new IllegalArgumentException("Mixing positional and named parameters are not allowed");
            }//else if

            queryParameters.add(new QueryParameterImpl<>(parameter.getIndex(), parameterType));
        }//if
        else if (expression instanceof JdbcNamedParameter parameter) {
            if (queryParameters.isEmpty()) {
                usingNamedParameters = true;
            }//if
            else if (!usingNamedParameters) {
                throw new IllegalArgumentException("Mixing positional and named parameters are not allowed");
            }//else if

            queryParameters.add(new QueryParameterImpl<>(parameter.getName(), queryParameters.size() + 1, parameterType));
        }//else
    }//addQueryParameter

    private <S> boolean processWhereColumn(BinaryExpression expression, Expression parameter, Column tableColumn, S context)
    {
        EntityInfo entityInfo = findEntityInfoWithColAlias(tableColumn.getFullyQualifiedName());

        if (entityInfo == null && tableColumn.getTable() == null) {
            tableColumn.setTable(fromTable);
            entityInfo = findEntityInfoWithColAlias(tableColumn.getFullyQualifiedName());
        }//if

        if (entityInfo == null) {
            entityInfo = findEntityInfoWithColAlias(tableColumn.getTable().getName());
            if (entityInfo != null) {
                EntityField field = entityInfo.getMetadata().getEntityField(tableColumn.getColumnName());
                tableColumn.setColumnName(field.getName());
                entityInfo = findEntityInfoWithColAlias(tableColumn.getFullyQualifiedName());
            }//if
            else {
                entityInfo = findEntityInfoWithColAlias(fromTable.getAlias().getName());
                if (entityInfo != null) {
                    if (tableColumn.getTable().getAlias() == null && !tableColumn.getFullyQualifiedName().startsWith(entityInfo.getColumnAlias())) {
                        String schema = entityInfo.getColumnAlias();
                        if (tableColumn.getTable().getSchemaName() != null) {
                            schema += "." + tableColumn.getTable().getSchemaName();
                        }//if
                        tableColumn.getTable().setSchemaName(schema);
                    }//if
                    String path = tableColumn.getFullyQualifiedName();
                    int dot = path.lastIndexOf('.');
                    String field = path.substring(dot + 1);
                    path = path.substring(0, dot);

                    EntityInfo foundInfo = entityInfo;
                    entityInfo = findJoins(path, entityInfo, context);
                    EntityField entityField = entityInfo.getMetadata().getEntityField(field);
                    if (entityField.isEntityField()) {
                        entityInfo = findJoins(tableColumn.getFullyQualifiedName(), foundInfo, context);
                        tableColumn.setColumnName(entityInfo.getMetadata().getIdField().getName());
                    }
                    tableColumn.getTable().setAlias(new Alias(entityInfo.getColumnAlias(), false));
                }//if
            }//else
        }//if

        if (entityInfo != null && (entityInfo.getMetadata().getEntityType() == EntityType.EMBEDDABLE || entityInfo.getMetadata().getEntityType() == EntityType.ID_CLASS)) {
            addQueryParameter(parameter, entityInfo.getMetadata().getEntityClass());

            ExpressionList<Expression> colList = new ParenthesedExpressionList<>();
            ExpressionList<Expression> paramList = new ParenthesedExpressionList<>();
            for (EntityField entityField : entityInfo.getMetadata().getEntityFields()) {
                Table table = new Table();
                table.setName(tableColumn.getTable().getFullyQualifiedName() + "." + tableColumn.getColumnName());
                colList.add(new Column(table, entityField.getName()));
                paramList.add(new JdbcParameter());
            }//for

            expression.setLeftExpression(colList);
            expression.setRightExpression(paramList);

            //Only visit the left tableColumn expression, we have already processed the parameters
            expression.getLeftExpression().accept(this, context);

            return false;
        }//if

        return true;
    }

    @SuppressWarnings("java:S6201") //instanceof check variable cannot be used here
    private <S> void visitEntity(BinaryExpression expression, S context)
    {
        Column tableColumn = null;
        Expression parameter = null;

        if (expression.getLeftExpression() instanceof Column && (expression.getRightExpression() instanceof JdbcParameter || expression.getRightExpression() instanceof JdbcNamedParameter)) {
            tableColumn = (Column) expression.getLeftExpression();
            parameter   = expression.getRightExpression();
        }//if
        else if (expression.getRightExpression() instanceof Column && (expression.getLeftExpression() instanceof JdbcParameter || expression.getLeftExpression() instanceof JdbcNamedParameter)) {
            tableColumn = (Column) expression.getRightExpression();
            parameter   = expression.getLeftExpression();
        }//else

        if (tableColumn != null && !processWhereColumn(expression, parameter, tableColumn, context)) {
            return;
        }//if

        expression.getLeftExpression().accept(this, context);
        expression.getRightExpression().accept(this, context);
    }

    @Override
    public <S> Void visit(EqualsTo pExpression, S context)
    {
        visitEntity(pExpression, context);
        return null;
    }

    @Override
    public <S> Void visit(NotEqualsTo pExpression, S context)
    {
        visitEntity(pExpression, context);
        return null;
    }

    @Override
    public <S> Void visit(Table tableName, S context)
    {
        if (tableName.getAlias() == null) {
            throw new IllegalArgumentException("Missing alias for " + tableName.getName());
        }//if

        EntityInfo entityInfo;
        EntityMetaData<?> metaData;
        if (tableName.getSchemaName() != null) {
            entityInfo = findEntityInfoWithColAlias(tableName.getSchemaName());
            if (entityInfo == null) {
                throw new IllegalArgumentException("Invalid schema - " + tableName);
            }//if

            EntityField field = entityInfo.getMetadata().getEntityField(tableName.getName());
            metaData = EntityMetaDataManager.getMetaData(field.getType());
        }//if
        else {
            metaData = EntityMetaDataManager.getMetaData(tableName.getName());
            tableName.setName(tableName.getAlias().getName());
        }//else

        entityInfo = new EntityInfo(tableName.getAlias().getName(), metaData);
        entityInfoList.add(entityInfo);

        tableName.setAlias(new Alias(entityInfo.getTableAlias(), false));
        tableName.setName(metaData.getTable());
        tableName.setSchemaName(null);

        return null;
    }//visitTable

    @SuppressWarnings("java:S1643") //StringBuilder cannot be used here
    private <S> EntityInfo findJoins(String path, EntityInfo entityInfo, S context)
    {
        String[] pathElements = path.split("\\.");
        String pathElement = pathElements[0];
        for (int i = 1; i < pathElements.length; i++) {
            EntityField field = entityInfo.getMetadata().getEntityField(pathElements[i]);
            if (!field.isEntityField()) {
                break;
            }//if

            pathElement = pathElement + "." + field.getName();
            entityInfo  = findEntity(pathElement, context);
        }//for

        return entityInfo;
    }//findJoins

    @Override
    public <S> Void visit(Column tableColumn, S context)
    {
        /*
         * A Column can either be point to an entity in which case we need to use the primary key field or to the actual field
         */
        EntityInfo entityInfo = findEntityInfoWithColAlias(tableColumn.getFullyQualifiedName());
        if (entityInfo == null) {
            if (tableColumn.getTable() == null) {
                tableColumn.setTable(fromTable);
            }//if

            if (tableColumn.getTable().getAlias() == null) {
                entityInfo = findEntityInfoWithColAlias(tableColumn.getTable().getFullyQualifiedName());
            }
            else {
                entityInfo = findEntityInfoWithColAlias(tableColumn.getTable().getAlias().getName());
            }
            if (entityInfo == null && this.tableAlias != null) {
                Table table = tableColumn.getTable();
                table.setSchemaName(tableAlias);
                entityInfo = findEntity(table.getFullyQualifiedName(), context);
            }//if

            if (entityInfo == null) {
                throw new IllegalArgumentException("Missing entity alias prefix on column '" + tableColumn + "'");
            }//if

            EntityField field = entityInfo.getMetadata().getEntityField(tableColumn.getColumnName());
            tableColumn.setColumnName(field.getColumn());
            tableColumn.setTable(new Table(entityInfo.getTableAlias()));
            if (context == Context.WHERE && (!entityInfo.getTableAlias().equals("t1") || !field.isIdField())) {
                selectUsingPrimaryKey = false;
            }
        }//if
        else {
            if (entityInfo.getMetadata().hasMultipleIdFields()) {
                throw new IllegalArgumentException("WHERE on Entity columns with multiple ID fields are not supported - " + tableColumn);
            }//if

            tableColumn.setTable(new Table(entityInfo.getTableAlias()));
            tableColumn.setColumnName(entityInfo.getMetadata().getIdField().getColumn());
            if (context == Context.WHERE && !entityInfo.getTableAlias().equals("t1")) {
                selectUsingPrimaryKey = false;
            }//if
        }//else

        return null;
    }//visitColumn

    @Override
    public <S> Void visit(Function function, S context)
    {
        if (function.getParameters() != null) {
            for (Expression item : function.getParameters()) {
                /*
                 * Only add a return type if the function was used in the select
                 */
                if (context == Context.SELECT) {
                    addResultType("c" + (returnTypes.size() + 1), Object.class, context);
                }//if

                item.accept(this, context);
            }//for
        }//if

        return null;
    }

    @Override
    public <S> Void visit(JdbcParameter jdbcParameter, S context)
    {
        addQueryParameter(jdbcParameter, Object.class);

        jdbcParameter.setUseFixedIndex(false);
        jdbcParameter.setIndex(queryParameters.size());

        /*
         * Only add a return type if the parameter was used in the select
         */
        if (context == Context.SELECT) {
            addResultType("c" + (returnTypes.size() + 1), Object.class, context);
        }//if

        return null;
    }//visitJdbcParameter

    @Override
    public <S> Void visit(JdbcNamedParameter jdbcNamedParameter, S context)
    {
        addQueryParameter(jdbcNamedParameter, Object.class);
        jdbcNamedParameter.setName("?");

        /*
         * Only add a return type if the parameter was used in the select
         */
        if (context == Context.SELECT) {
            addResultType("c" + (returnTypes.size() + 1), Object.class, context);
        }//if

        return null;
    }//visitJdbcNamedParameter
}
