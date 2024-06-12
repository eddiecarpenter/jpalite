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

import com.google.auto.service.AutoService;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("io.jpalite.repository.Repository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
@SuppressWarnings("java:S1192") //We are generating code here. Defining statics will impair the readability
public class JPALiteRepositoryProcessor extends AbstractProcessor
{
	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

	//---------------------------------------------------------------[ note ]---
	private void note(String msg)
	{
		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
	}//note

	//------------------------------------------------------------[ process ]---
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
	{

		if (roundEnv.processingOver()) {
			return false;
		}//if

		note("Creating JPALite Repositories");
		try {
			Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(Repository.class);

			for (Element element : set) {
				Repository annotation = element.getAnnotation(Repository.class);
				if (annotation != null) {
					generateRepo((TypeElement) element, annotation);
				}//if
			}//for
			return true;
		}//try
		catch (Exception ex) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
		}//catch

		return false;
	}//process

	private void addJpaRepository(PrintWriter out, DeclaredType jpaRepository)
	{

		String argType = jpaRepository.getTypeArguments().get(0).toString();
		String idType = jpaRepository.getTypeArguments().get(1).toString();

		out.println("@Transactional");
		out.println("public void persist(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  if (!em.contains(entity)) {");
		out.println("    em.persist(entity);");
		out.println("  }");
		out.println("}");
		out.println("");

		out.println("@Transactional");
		out.println("public void save(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  if (!em.contains(entity)) {");
		out.println("    if (em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity) != null) {");
		out.println("      em.merge(entity);");
		out.println("    } else {");
		out.println("      em.persist(entity);");
		out.println("    }");
		out.println("  }");
		out.println("}");
		out.println("");

		out.println("@Transactional");
		out.println("public " + argType + " merge(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  if (!em.contains(entity)) {");
		out.println("    return (" + argType + ") em.merge(entity);");
		out.println("  }");
		out.println("  return (" + argType + ")entity;");
		out.println("}");
		out.println("");

		out.println("public void refresh(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  em.refresh(entity);");
		out.println("}");
		out.println("");

		out.println("public void lock(" + argType + " entity, LockModeType mode) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  em.lock(entity, mode);");
		out.println("}");
		out.println("");

		out.println("public " + argType + " findById(" + idType + " id) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  return em.find(" + argType + ".class,id);");
		out.println("}");
		out.println("");

		out.println("public " + argType + " findById(" + idType + " id, LockModeType mode) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  return em.find(" + argType + ".class,id, mode);");
		out.println("}");
		out.println("");

		out.println("public " + argType + " getReference(" + idType + " id) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  return em.getReference(" + argType + ".class,id);");
		out.println("}");
		out.println("");

		out.println("public " + argType + " clone(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  return ((JPALiteEntityManager)em).clone(entity);");
		out.println("}");
		out.println("");

		out.println("public void delete(" + argType + " entity) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  em.remove(entity);");
		out.println("}");
		out.println("");
	}//addJpaRepository

	private void addPagingRepository(PrintWriter out, DeclaredType pagingRepository)
	{
		String vArgType = pagingRepository.getTypeArguments().get(0).toString();
		String vEntityType = vArgType.substring(vArgType.lastIndexOf(".") + 1);

		out.println("public long count(Filter filter, Map<String, Object> hints) {");
		out.println("  EntityManager em = getEntityManager();");
		out.print("  String q=\"select count(");
		out.print(vEntityType);
		out.print(") from ");
		out.print(vEntityType);
		out.println("\";");
		out.println("  Map<String, Object> parameters = new HashMap<>();");
		out.println("  if (!filter.isUnfiltered()) q = q+\" where \"+filter.getExpression(parameters); ");
		out.println("  TypedQuery<Long> query = em.createQuery(q,Long.class);");
		out.println("  if (hints != null && !hints.isEmpty()) {");
		out.println("    hints.entrySet().forEach(entry -> query.setHint(entry.getKey(), entry.getValue()));");
		out.println("  }");
		out.println("  if (!parameters.isEmpty()) parameters.entrySet().stream().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));");
		out.println("  return query.getSingleResult();");
		out.println("}");
		out.println("");

		out.print("public List<");
		out.print(vArgType);
		out.println("> findAll(Pageable pageable, Filter filter, Map<String, Object> hints) {");
		out.println("  EntityManager em = getEntityManager();");
		out.println("  if (hints != null && !hints.isEmpty()) {");
		out.println("    hints.entrySet().stream().forEach(entry -> em.setProperty(entry.getKey(), entry.getValue()));");
		out.println("  }");

		out.print("  String q=\"select ");
		out.print(vEntityType);
		out.print(" from ");
		out.print(vEntityType);
		out.println("\";");
		out.println("  Map<String, Object> parameters = new HashMap<>();");
		out.println("  if (!filter.isUnfiltered()) q = q+\" where \"+filter.getExpression(parameters); ");
		out.println("  if (!pageable.getSort().isUnsorted()) q = q+\" order by \"+pageable.getSort().getExpression();");
		out.print("  TypedQuery<");
		out.print(vArgType);
		out.print("> query = em.createQuery(q,");
		out.print(vArgType);
		out.println(".class);");
		out.println("  if (hints != null && !hints.isEmpty()) {");
		out.println("    hints.entrySet().forEach(entry -> query.setHint(entry.getKey(), entry.getValue()));");
		out.println("  }");
		out.println("  if (!parameters.isEmpty()) parameters.entrySet().stream().forEach(entry -> query.setParameter(entry.getKey(), entry.getValue()));");
		out.println("  if (!pageable.isUnpaged()) query.setFirstResult(pageable.getPageIndex())");
		out.println("                                    .setMaxResults(pageable.getPageSize());");
		out.println("  return query.getResultList();");
		out.println("}");
		out.println("");
	}//addPagingRepository

	@SuppressWarnings({"java:S3776", "java:S6541"}) //Complexity cannot be reduced further
	private void createMethod(PrintWriter out, ExecutableElement method)
	{
		Query[] queries = method.getAnnotationsByType(Query.class);
		if (queries.length > 0) {
			method.getAnnotationMirrors()
					.forEach(p ->
							 {
								 if (!p.getAnnotationType().toString().equals(Query.class.getName())) {
									 out.println(p);
								 }//if
							 });

			out.print("public final ");

			//The return type can either be a List or an object

			out.print(method.getReturnType());
			out.print(" ");
			out.print(method.getSimpleName().toString());
			out.print("(");

			out.print(method.getParameters()
							  .stream()
							  .map(p -> p.asType() + " " + p.getSimpleName())
							  .collect(Collectors.joining(",")));
			out.println(") {");
			out.println("  EntityManager em = getEntityManager();");
			boolean returnCollection = false;
			String returnType = method.getReturnType().toString();
			if (method.getReturnType() instanceof DeclaredType declaredType && !declaredType.getTypeArguments().isEmpty()) {
				returnCollection = true;
				returnType = declaredType.getTypeArguments().get(0).toString();
			}//if

			StringBuilder pageAndSort = new StringBuilder();
			Query query = queries[0];
			if (query.namedQuery()) {
				out.print("TypedQuery<");
				out.print(returnType);
				out.print("> query = em.createNamedQuery(\"");
				out.print(query.value());
				out.print("\",");
				out.print(returnType);
				out.println(".class);");

				method.getParameters()
						.stream()
						.filter(p -> p.asType().toString().equals(Pageable.class.getName()))
						.forEach(p -> pageAndSort.append("query.setFirstResult(")
								.append(p.getSimpleName())
								.append(".getPageIndex())\n")
								.append(".setMaxResults(")
								.append(p.getSimpleName())
								.append(".getPageSize());\n"));
			}//if
			else {
				out.println("String queryStr = \"" + query.value() + "\";");

				//Pageable and Sorting is only support for JPQL queries
				if (!query.nativeQuery() && !query.updateQuery()) {
					//Check for Pageable parameter
					method.getParameters()
							.stream()
							.filter(p -> p.asType().toString().equals(Pageable.class.getName()))
							.forEach(p ->
									 {
										 pageAndSort.append("query.setFirstResult(")
												 .append(p.getSimpleName())
												 .append(".getPageIndex())\n")
												 .append(".setMaxResults(")
												 .append(p.getSimpleName())
												 .append(".getPageSize());\n");
										 out.println("queryStr += " + p.getSimpleName() + ".getSort().getOrderBy();");
									 });
					method.getParameters()
							.stream()
							.filter(p -> p.asType().toString().equals(Sort.class.getName()))
							.forEach(p -> out.println("queryStr += " + p.getSimpleName() + ".getOrderBy();"));
				}//if

				if (query.nativeQuery()) {
					out.println("jakarta.persistence.Query query = em.createNativeQuery(queryStr);");
				}//if
				else if (query.updateQuery()) {
					out.println("jakarta.persistence.Query query = em.createQuery(queryStr);");
				}//if
				else {
					out.print("TypedQuery<");
					out.print(returnType);
					out.print("> query = em.createQuery(queryStr,");

					out.print(returnType);
					out.println(".class);");
				}//else
			}//else

			if (!query.updateQuery() && query.lockMode() != LockModeType.NONE) {
				out.print("  query.setLockMode(");
				out.print("LockModeType." + query.lockMode().name());
				out.println(");");
			}//if

			if (method.getParameters()
					.stream()
					.anyMatch(p -> p.getAnnotation(QueryParam.class) != null)) {

				method.getParameters()
						.stream()
						.filter(p -> p.getAnnotation(QueryParam.class) != null)
						.map(p -> "\"" + p.getAnnotation(QueryParam.class).value() + "\"," + p.getSimpleName())
						.forEach(p -> out.println("query.setParameter(" + p + ");"));
			}//if
			else {
				AtomicInteger paramNr = new AtomicInteger(0);
				method.getParameters()
						.stream()
						.filter(p -> !p.asType().toString().equals(Pageable.class.getName()))
						.map(p -> paramNr.incrementAndGet() + "," + p.getSimpleName())
						.forEach(p -> out.println("query.setParameter(" + p + ");"));
			}//else

			for (QueryHint hint : query.hints()) {
				out.print("query.setHint(\"");
				out.print(hint.name());
				out.print("\",\"");
				out.print(hint.value());
				out.println("\");");
			}//for

			if (query.updateQuery()) {
				out.println("return query.executeUpdate();");
			}//if
			else {
				out.println(pageAndSort);

				if (!returnCollection && query.catchNoResult()) {
					out.println("try {");
				}//if
				out.print("  return ");
				if (query.nativeQuery()) {
					out.print("(");
					out.print(returnType);
					out.print(") ");
				}//if
				out.print("  query.");
				if (returnCollection) {
					out.println("getResultList();");
				}//if
				else {
					out.println("getSingleResult();");
				}//else

				if (!returnCollection && query.catchNoResult()) {
					out.println("}");
					out.println("catch (NoResultException ex) {");
					out.println("  return null;");
					out.println("}");
				}//if
			}//else

			out.println("}");
		}//if
	}//createMethod

	private void generateRepo(TypeElement repoElement, Repository annotation) throws IOException
	{

		String packageName = annotation.name();
		if (packageName.isBlank()) {
			packageName = repoElement.getQualifiedName() + "Impl";
		}//if

		int lastDot = packageName.lastIndexOf('.');
		if (lastDot == 0) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Repository.name is invalid, expected qualified name");
			return;
		}//if

		FileObject repoFile = processingEnv.getFiler().createSourceFile(packageName);
		String repoClassName = packageName.substring(lastDot + 1);
		packageName = packageName.substring(0, lastDot);

		try (PrintWriter out = new PrintWriter(repoFile.openWriter())) {
			out.print("package ");
			out.print(packageName);
			out.println(";");
			out.println();
			out.println("import io.quarkus.arc.Arc;");
			out.println("import jakarta.enterprise.context.RequestScoped;");
			out.println("import jakarta.inject.Inject;");
			out.println("import jakarta.transaction.Transactional;");
			out.println("import jakarta.annotation.Generated;");
			out.println("import jakarta.persistence.*;");
			out.println("import java.util.Collections;");
			out.println("import java.util.List;");
			out.println("import java.util.HashMap;");
			out.println("import java.util.Map;");
			out.println("import io.jpalite.extension.PersistenceProducer;");
			out.println("import io.jpalite.JPALiteEntityManager;");
			out.println("import io.jpalite.PersistenceUnit;");
			out.println("import io.jpalite.repository.*;");
			out.println("import io.jpalite.EntityState;");
			out.println();

			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			out.println("@Generated(value={\"Generated by " + getClass().getName() + "\"}," +
						"date=\"" + dateFormat.format(new Date()) + "\"," +
						"comments=\"JPALite Repository Generation\")");
			out.println("@RequestScoped");
			out.print("public class ");
			out.print(repoClassName);
			if (!annotation.abstractClass().isBlank()) {
				out.print(" extends ");
				out.print(annotation.abstractClass());
				out.print(" ");
			}//if
			out.print(" implements ");
			out.print(repoElement.getQualifiedName());

			out.println(" {");
			out.println("  @Inject");
			out.print("  @PersistenceUnit(\"");
			out.print(annotation.persistenceUnit());
			out.println("\")");
			out.println("  EntityManager em;");

			out.println("public EntityManager getEntityManager() {");
//			out.println("   PersistenceProducer producer = Arc.container().instance(PersistenceProducer.class).get();");
//			out.print("   return producer.getEntityManager(\"");
//			out.print(annotation.persistenceUnit());
//			out.println("\");");
			out.println("   return em;");
			out.println("}");

			repoElement.getInterfaces()
					.forEach(t ->
							 {
								 switch (((DeclaredType) t).asElement().toString()) {
									 case "io.jpalite.repository.JpaRepository" ->
											 addJpaRepository(out, (DeclaredType) t);
									 case "io.jpalite.repository.PagingRepository" ->
											 addPagingRepository(out, (DeclaredType) t);
									 default -> {
										 //Ignore the rest
									 }
								 }
							 });

			for (Element vMethod : repoElement.getEnclosedElements()) {
				createMethod(out, (ExecutableElement) vMethod);
			}//for

			out.println("}");
			out.flush();
		}//try
		catch (Exception ex) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Process Error:" + ex.getMessage());
		}//catch
	}//generateRepo
}//JPALiteRepositoryProcessor
