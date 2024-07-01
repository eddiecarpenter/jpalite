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

package io.jpalite.jqpl;

import io.jpalite.EntityMetaData;
import io.jpalite.EntityMetaDataManager;
import io.jpalite.JPALiteEntityManager;
import io.jpalite.impl.parsers.JPQLParser;
import io.jpalite.test.*;
import jakarta.persistence.FetchType;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JPQLParserTest
{
	@BeforeAll
	static void beforeAll()
	{
		TestEntityMetaDataManager.init();
	}

	@Test
	void testUsingSelectIn()
	{
		JPQLParser vParser = new JPQLParser("select RatePlan from RatePlan where (uid, resourceVersion) in (select e.uid, max(e.resourceVersion) from RatePlan e group by e.uid)", new HashMap<>());
		Assertions.assertEquals("SELECT t1.ID \"c1-1\", t1.UID \"c1-2\", t1.RESOURCE_VERSION \"c1-3\", t1.OPERATOR_ID \"c1-4\", t1.PLAN_NAME \"c1-5\", t1.CREATED_BY \"c1-6\", t1.APPROVED_BY \"c1-7\", t1.EFFECTIVE_DATE \"c1-8\", " +
										"t1.RATE_PLAN_CONFIG \"c1-9\", t1.MODIFIED_ON \"c1-10\", t1.CREATED_DATE \"c1-11\" " +
										"FROM RATE_PLAN t1 " +
										"WHERE (t1.UID, t1.RESOURCE_VERSION) IN (SELECT t2.UID \"c1\", max(t2.RESOURCE_VERSION) \"c2\" " +
										"FROM RATE_PLAN t2 GROUP BY t2.UID)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(RatePlan.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void testUsingBitAndOperators() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("select E from Employee E where bitand(E.department.id, :flag) = :flag", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE bitand(t2.IRN, ?) = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("select Employee from Employee where bitand(department.id, :flag) = :flag", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE bitand(t2.IRN, ?) = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("select Employee1 from Employee1 where bitand(department.id, :flag) = :flag", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2\", t1.AGE \"c1-3\", t1.DEPT \"c1-5\" " +
										"FROM EMPLOYEE t1 " +
										"LEFT JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"WHERE bitand(t2.IRN, ?) = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee1.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenUsingBracketsInWhereClauses() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("select E from Employee E where (E.department.id = :val)", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE (t2.IRN = ?)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("select Employee from Employee where (department.id = :val)", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE (t2.IRN = ?)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenPathExpressionIsUsedForSingleValuedAssociation_thenCreatesImplicitInnerWithLazyJoin() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee1 e", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2\", t1.AGE \"c1-3\", t1.DEPT \"c1-5\" " +
										"FROM EMPLOYEE t1",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee1.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("SELECT e FROM Employee1 e JOIN e.department", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", t2.COMP \"c1-5-3\" " +
										"FROM EMPLOYEE t1 " +
										"LEFT JOIN DEPT t2 ON t1.DEPT = t2.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee1.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenOverridingFetchTypes_thenGenerateQueryCorrect() throws JSQLParserException
	{
		Map<String, Object> vHints = new HashMap<>();
		vHints.put(JPALiteEntityManager.PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE, FetchType.EAGER);
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e", vHints);
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", t1.SALARY \"c1-4\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vHints = new HashMap<>();
		vHints.put(JPALiteEntityManager.PERSISTENCE_OVERRIDE_FETCHTYPE, FetchType.LAZY);
		vParser = new JPQLParser("SELECT e FROM Employee e", vHints);
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", t1.DEPT \"c1-5\" " +
										"FROM EMPLOYEE t1",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vHints = new HashMap<>();
		vHints.put(JPALiteEntityManager.PERSISTENCE_OVERRIDE_FETCHTYPE, FetchType.LAZY);
		vHints.put(JPALiteEntityManager.PERSISTENCE_OVERRIDE_BASIC_FETCHTYPE, FetchType.EAGER);
		vParser = new JPQLParser("SELECT e FROM Employee e", vHints);
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t1.SALARY \"c1-4\", t1.DEPT \"c1-5\" " +
										"FROM EMPLOYEE t1",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenPathExpressionIsUsedForSingleValuedAssociation_thenCreatesImplicitInnerJoin() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("SELECT Employee FROM Employee", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("SELECT e FROM Employee e JOIN e.department", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("select e.department.id, e.department.company from Employee e", new HashMap<>());
		Assertions.assertEquals("SELECT t2.IRN \"c1\", " +
										"t3.IRN \"c2-1\", t3.NAME \"c2-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(2, vParser.getReturnTypes().size());
		Assertions.assertEquals(Integer.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(Company.class, vParser.getReturnTypes().get(1));

		vParser = new JPQLParser("select e.fullName.name, e.department.name, e.department.id from Employee e", new HashMap<>());
		Assertions.assertEquals("SELECT t1.NAME \"c1\", t2.NAME \"c2\", t2.IRN \"c3\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN",
								vParser.getQuery());
		Assertions.assertEquals(3, vParser.getReturnTypes().size());
		Assertions.assertEquals(String.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(String.class, vParser.getReturnTypes().get(1));
		Assertions.assertEquals(Integer.class, vParser.getReturnTypes().get(2));

		vParser = new JPQLParser("select e.fullName, e.department from Employee e", new HashMap<>());
		Assertions.assertEquals("SELECT t1.NAME \"c1-1\", t1.SURNAME \"c1-2\", t2.IRN \"c2-1\", t2.NAME \"c2-2\", " +
										"t3.IRN \"c2-3-1\", t3.NAME \"c2-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(2, vParser.getReturnTypes().size());
		Assertions.assertEquals(FullName.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(Department.class, vParser.getReturnTypes().get(1));
	}

	@Test
	void whenJoinKeywordIsUsed_thenCreatesExplicitInnerJoin() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("select case when exists(select 1 from Employee where id=:id) then 1 else 0 end", new HashMap<>());

		Assertions.assertEquals("SELECT CASE WHEN EXISTS (SELECT 1 \"c1\" FROM EMPLOYEE t1 WHERE t1.IRN = ?) THEN 1 ELSE 0 END \"c1\"",
								vParser.getQuery());

		vParser = new JPQLParser("select count(Employee) from Employee where department.company=:id", new HashMap<>());

		Assertions.assertEquals("SELECT count(t1.IRN) \"c1\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t3.IRN = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Object.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(1, vParser.getQueryParameters().size());
		Assertions.assertEquals(Object.class, vParser.getQueryParameters().get(0).getParameterType());
		Assertions.assertFalse(vParser.isSelectUsingPrimaryKey());
	}

	@Test
	void whenWhereINIsUsed() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e where e.age in (11,22,33)", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.AGE IN (11, 22, 33)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("SELECT e FROM Employee e where e.age in (select e1.age from Employee1 e1)", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.AGE IN (SELECT t4.AGE \"c1\" FROM EMPLOYEE t4)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

	}

	@Test
	void whenIsNullIsUsed() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e where e.fullName.name is null", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.NAME IS NULL",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));

		vParser = new JPQLParser("SELECT e FROM Employee e where e.fullName.name is not null", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.NAME IS NOT NULL",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenExistIsUsed_thenCreatesExplicitInnerJoin() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT d FROM Employee e JOIN e.department d", new HashMap<>());

		Assertions.assertEquals("SELECT t2.IRN \"c1-1\", t2.NAME \"c1-2\", " +
										"t3.IRN \"c1-3-1\", t3.NAME \"c1-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Department.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenLeftKeywordIsSpecified_thenCreatesOuterJoinAndIncludesNonMatched() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT DISTINCT d FROM Department d LEFT JOIN d.employees e", new HashMap<>());

		Assertions.assertEquals("SELECT DISTINCT t1.IRN \"c1-1\", t1.NAME \"c1-2\", " +
										"t3.IRN \"c1-3-1\", t3.NAME \"c1-3-2\" " +
										"FROM DEPT t1 " +
										"LEFT JOIN EMPLOYEE t2 ON t1.IRN = t2.DEPT " +
										"INNER JOIN COMPANY t3 ON t1.COMP = t3.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Department.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenEntitiesAreListedInFrom_ThenCreatesCartesianProduct() throws JSQLParserException
	{
		//Mark the company field as lazy for this test
		EntityMetaData<Department> vMetaData = EntityMetaDataManager.getMetaData(Department.class);
		vMetaData.getEntityField("company").setFetchType(FetchType.LAZY);

		JPQLParser vParser = new JPQLParser("SELECT e, d FROM Employee e, Department d", new HashMap<>());
		vMetaData.getEntityField("company").setFetchType(FetchType.EAGER);

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t2.COMP \"c1-5-3\", " +
										"t2.IRN \"c2-1\", t2.NAME \"c2-2\", t2.COMP \"c2-3\" " +
										"FROM EMPLOYEE t1, DEPT t2",
								vParser.getQuery());
		Assertions.assertEquals(2, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(Department.class, vParser.getReturnTypes().get(1));
	}

	@Test
	void whenEntitiesAreListedInFromAndMatchedInWhere_ThenCreatesJoin() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT d FROM Employee e, Department d WHERE e.department = d", new HashMap<>());

		Assertions.assertEquals("SELECT t2.IRN \"c1-1\", t2.NAME \"c1-2\", " +
										"t3.IRN \"c1-3-1\", t3.NAME \"c1-3-2\" " +
										"FROM EMPLOYEE t1, DEPT t2 " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.DEPT = t2.IRN",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Department.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenEntitiesAreOrderedByFK() throws JSQLParserException
	{

		JPQLParser vParser = new JPQLParser("SELECT Employee FROM Employee Order by department.name asc", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"ORDER BY t2.NAME ASC", vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenCollectionValuedAssociationIsJoined_ThenCanSelect() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e JOIN e.phones ph WHERE ph LIKE '1%'", new HashMap<>());

		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t3.IRN \"c1-5-1\", t3.NAME \"c1-5-2\", " +
										"t4.IRN \"c1-5-3-1\", t4.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"JOIN PHONE t2 ON t1.IRN = t2.EMPL " +
										"INNER JOIN DEPT t3 ON t1.DEPT = t3.IRN " +
										"INNER JOIN COMPANY t4 ON t3.COMP = t4.IRN " +
										"WHERE t2.IRN LIKE '1%'",
								vParser.getQuery());

		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenMultipleEntitiesAreListedWithJoin_ThenCreatesMultipleJoins() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT ph FROM Employee e " +
													"JOIN e.department d " +
													"JOIN e.phones ph " +
													"WHERE d.name IS NOT NULL", new HashMap<>());
		Assertions.assertEquals("SELECT t3.IRN \"c1-1\", t3.NUM \"c1-2\", " +
										"t1.IRN \"c1-3-1\", t1.NAME \"c1-3-2-1\", t1.SURNAME \"c1-3-2-2\", t1.AGE \"c1-3-3\", " +
										"t2.IRN \"c1-3-5-1\", t2.NAME \"c1-3-5-2\", " +
										"t4.IRN \"c1-3-5-3-1\", t4.NAME \"c1-3-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"JOIN PHONE t3 ON t1.IRN = t3.EMPL " +
										"INNER JOIN COMPANY t4 ON t2.COMP = t4.IRN " +
										"WHERE t2.NAME IS NOT NULL",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Phone.class, vParser.getReturnTypes().get(0));
	}

	@Test
	void whenGroupByOrderBy_ThenCreatesJoins() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e.fullName.name, count(ph.id) FROM Employee e " +
													"JOIN e.phones ph " +
													"group by e.fullName.name " +
													"order by e.fullName.name, ph", new HashMap<>());
		Assertions.assertEquals("SELECT t1.NAME \"c1\", count(t2.IRN) \"c2\" " +
										"FROM EMPLOYEE t1 " +
										"JOIN PHONE t2 ON t1.IRN = t2.EMPL " +
										"GROUP BY t1.NAME " +
										"ORDER BY t1.NAME, t2.IRN",
								vParser.getQuery());
		Assertions.assertEquals(2, vParser.getReturnTypes().size());
		Assertions.assertEquals(String.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(Object.class, vParser.getReturnTypes().get(1));
	}

	@Test
	void whenGroupByHaving_ThenCreatesJoins() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e.fullName.name, count(ph.id),:p1 FROM Employee e " +
													"JOIN e.phones ph " +
													"group by e.fullName.name " +
													"having count(ph.id) > 1", new HashMap<>());
		Assertions.assertEquals("SELECT t1.NAME \"c1\", count(t2.IRN) \"c2\", ? \"c3\" " +
										"FROM EMPLOYEE t1 " +
										"JOIN PHONE t2 ON t1.IRN = t2.EMPL " +
										"GROUP BY t1.NAME " +
										"HAVING count(t2.IRN) > 1",
								vParser.getQuery());
		assertDoesNotThrow(() -> vParser.checkType(Object[].class));
		assertThrows(IllegalArgumentException.class, () -> vParser.checkType(Employee.class));
	}

	@Test
	void whenWhereByEntity_ThenCreateWhereOnPK() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("SELECT e FROM Employee e " +
													"where e.fullName=:FullName", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE (t1.NAME, t1.SURNAME) = (?, ?)",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(1, vParser.getQueryParameters().size());
		Assertions.assertEquals(FullName.class, vParser.getQueryParameters().get(0).getParameterType());

		vParser = new JPQLParser("SELECT Employee FROM Employee " +
										 "where fullName=:FullName and age>:age", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE (t1.NAME, t1.SURNAME) = (?, ?) " +
										"AND t1.AGE > ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(2, vParser.getQueryParameters().size());
		Assertions.assertEquals(FullName.class, vParser.getQueryParameters().get(0).getParameterType());

		vParser = new JPQLParser("SELECT e FROM Employee e where e=?1", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t1.IRN = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(1, vParser.getQueryParameters().size());
		Assertions.assertEquals(Object.class, vParser.getQueryParameters().get(0).getParameterType());
		Assertions.assertTrue(vParser.isSelectUsingPrimaryKey());

		vParser = new JPQLParser("SELECT e FROM Employee e where e.department.company=?1", new HashMap<>());
		Assertions.assertEquals("SELECT t1.IRN \"c1-1\", t1.NAME \"c1-2-1\", t1.SURNAME \"c1-2-2\", t1.AGE \"c1-3\", " +
										"t2.IRN \"c1-5-1\", t2.NAME \"c1-5-2\", " +
										"t3.IRN \"c1-5-3-1\", t3.NAME \"c1-5-3-2\" " +
										"FROM EMPLOYEE t1 " +
										"INNER JOIN DEPT t2 ON t1.DEPT = t2.IRN " +
										"INNER JOIN COMPANY t3 ON t2.COMP = t3.IRN " +
										"WHERE t3.IRN = ?",
								vParser.getQuery());
		Assertions.assertEquals(1, vParser.getReturnTypes().size());
		Assertions.assertEquals(Employee.class, vParser.getReturnTypes().get(0));
		Assertions.assertEquals(1, vParser.getQueryParameters().size());
		Assertions.assertEquals(Object.class, vParser.getQueryParameters().get(0).getParameterType());
		Assertions.assertFalse(vParser.isSelectUsingPrimaryKey());
	}

	@Test
	void deleteEntityUsingPK() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("DELETE Employee e where e.id=:id", new HashMap<>());
		Assertions.assertEquals("DELETE EMPLOYEE t1 WHERE t1.IRN = ?",
								vParser.getQuery());

		vParser = new JPQLParser("DELETE Employee where age>?", new HashMap<>());
		Assertions.assertEquals("DELETE EMPLOYEE t1 WHERE t1.AGE > ?",
								vParser.getQuery());
	}

	@Test
	void updateEntityUsingPK() throws JSQLParserException
	{
		JPQLParser vParser = new JPQLParser("UPDATE Employee e set e.salary=e.salary*1.05 " +
													"where e.id=:id", new HashMap<>());
		Assertions.assertEquals("UPDATE EMPLOYEE t1 SET t1.SALARY = t1.SALARY * 1.05 WHERE t1.IRN = ?",
								vParser.getQuery());

		vParser = new JPQLParser("UPDATE Employee set salary=salary*1.05 " +
										 "where id=:id", new HashMap<>());
		Assertions.assertEquals("UPDATE EMPLOYEE t1 SET t1.SALARY = t1.SALARY * 1.05 WHERE t1.IRN = ?",
								vParser.getQuery());

		vParser = new JPQLParser("UPDATE Employee e set (e.salary,e.age)=(e.salary*1.05,e.age+1) " +
										 "where e.id=:id", new HashMap<>());
		Assertions.assertEquals("UPDATE EMPLOYEE t1 SET (t1.SALARY, t1.AGE) = (t1.SALARY * 1.05, t1.AGE + 1) WHERE t1.IRN = ?",
								vParser.getQuery());

		vParser = new JPQLParser("UPDATE Employee set (salary,age)=(salary*1.05,age+1) " +
										 "where id=:id", new HashMap<>());
		Assertions.assertEquals("UPDATE EMPLOYEE t1 SET (t1.SALARY, t1.AGE) = (t1.SALARY * 1.05, t1.AGE + 1) WHERE t1.IRN = ?",
								vParser.getQuery());
	}

	@Test
	void whenUsingNamedParameters_thenCheckIfNamesReused()
	{
		JPQLParser vParser = new JPQLParser("select Employee from Employee " +
													"where id = :num " +
													"and age= :num",
											new HashMap<>());
		assertEquals(2, vParser.getNumberOfParameters());
	}
}
