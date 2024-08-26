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

package org.jpalite.impl;

import jakarta.persistence.*;
import javassist.*;
import org.jpalite.JPALiteTooling;
import org.jpalite.JPALiteToolingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.reflect.Modifier.*;

public class JPALiteToolingImpl implements JPALiteTooling
{
    private static final Logger LOG = LoggerFactory.getLogger(JPALiteToolingImpl.class);
    private static final String CHECK_FIELD_NAME = "JPALITE_TOOLING";
    public static final String ERROR_PROCESSING_FILE = "Error processing file";

    private final Map<String, String> entityClasses = new TreeMap<>();
    private final List<CtClass> converterClasses = new ArrayList<>();
    private String outputDir;
    private final ClassPool classPool;

    public JPALiteToolingImpl()
    {
        outputDir = ".";
        classPool = ClassPool.getDefault();
    }//JPATooling

    private void applyLazyFetch(CtClass entityClass, CtField field) throws CannotCompileException, NotFoundException
    {
        //Id fields cannot be fetched lazily
        if (!field.hasAnnotation(Id.class) && (field.hasAnnotation(Column.class) || field.hasAnnotation(OneToMany.class) || field.hasAnnotation(JoinColumn.class))) {
            String fieldName = field.getName();
            String type = field.getType().getName();
            String vGetterMethod = ((type.equals(Boolean.class.getName()) || type.equals(boolean.class.getName()) ? "is" : "get") + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            try {
                CtMethod getter = entityClass.getDeclaredMethod(vGetterMethod);
                getter.insertBefore("{_lazyFetch(\"" + field.getName() + "\");}");
            }//try
            catch (NotFoundException ex) {
                LOG.debug("Field {}::{} have no getting method - Lazy fetching tooling not applied.", entityClass.getName(), field.getName());
            }//catch
        }//if
    }//applyLazyFetch

    private void applyChangeTracker(CtClass entityClass, CtField field) throws CannotCompileException, NotFoundException
    {
        String fieldName = field.getName();
        String vSetterMethod = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        try {
            CtMethod setter = entityClass.getDeclaredMethod(vSetterMethod, new CtClass[]{field.getType()});
            setter.insertBefore("{_markField(\"" + field.getName() + "\");}");
        }//try
        catch (NotFoundException ex) {
            LOG.warn("Field {}::{} have no setter method - Change tracking not applied.", entityClass.getName(), field.getName());
        }//catch
    }//applyChangeTracker

    private void applyToString(CtClass entityClass) throws CannotCompileException
    {
        try {
            CtMethod setter = entityClass.getDeclaredMethod("toString", new CtClass[]{});
            setter.insertBefore("{return super.toString();}");
        }//try
        catch (NotFoundException ex) {
            CtMethod toString = CtMethod.make("public String toString(){return super.toString();}", entityClass);
            entityClass.addMethod(toString);
        }//catch
    }//applyToString

    private void applyEquals(CtClass pEntityClass) throws CannotCompileException
    {
        try {
            CtMethod method = pEntityClass.getDeclaredMethod("equals");
            pEntityClass.removeMethod(method);
        }//try
        catch (NotFoundException ex) {
            //Ignore
        }//catch

        CtMethod equals = CtMethod.make("public boolean equals(Object o){" +
                                        "return super.equals(o);" +
                                        "}", pEntityClass);
        pEntityClass.addMethod(equals);
    }//applyEquals

    private void applyTooling(CtClass entityClass) throws JPALiteToolingException
    {
        try {
            LOG.debug("Applying JPA Tooling to {}", entityClass.getName());
            Class<?> jpaEntityClass = Thread.currentThread().getContextClassLoader().loadClass("org.jpalite.impl.JPAEntityImpl");
            CtClass jpaEntityImpl = classPool.get(jpaEntityClass.getName());

            entityClass.setSuperclass(jpaEntityImpl);
            for (CtField field : entityClass.getDeclaredFields()) {
                if (!isStatic(field.getModifiers()) && !isFinal(field.getModifiers()) && !isTransient(field.getModifiers()) && field.getAnnotation(Transient.class) == null) {
                    applyChangeTracker(entityClass, field);
                    applyLazyFetch(entityClass, field);
                    applyToString(entityClass);
                    applyEquals(entityClass);
                }//if
            }//for
        }//try
        catch (CannotCompileException ex) {
            throw new JPALiteToolingException("Compiler error " + entityClass.getName());
        }
        catch (ClassNotFoundException | NotFoundException ex) {
            LOG.error("Error transforming {}", entityClass.getName(), ex);
            throw new JPALiteToolingException("Error check annotation " + entityClass.getName());
        }
    }//applyTooling

    private void toolClass(CtClass ctClass) throws JPALiteToolingException, CannotCompileException, IOException
    {
        try {
            ctClass.getDeclaredField(CHECK_FIELD_NAME);
            LOG.debug("JPA Tooling already applied to {} - skipping class", ctClass.getSimpleName());
        }//try
        catch (NotFoundException ex) {
            applyTooling(ctClass);

            CtField checkField = CtField.make("private static final boolean " + CHECK_FIELD_NAME + " = true;", ctClass);
            ctClass.addField(checkField);

            ctClass.writeFile(outputDir);
        }//else
    }//toolClass

    private void processConvertClass(CtClass ctClass) throws JPALiteToolingException
    {
        try {
            Converter converter = (Converter) ctClass.getAnnotation(Converter.class);
            if (converter != null) {
                converterClasses.add(ctClass);
            }//if
        }//try
        catch (ClassNotFoundException ex) {
            throw new JPALiteToolingException(ERROR_PROCESSING_FILE, ex);
        }//catch
    }//processClass

    private void processClass(CtClass ctClass) throws JPALiteToolingException
    {
        try {
            Entity entity = (Entity) ctClass.getAnnotation(Entity.class);
            if (entity != null || ctClass.getAnnotation(Table.class) != null || ctClass.getAnnotation(Embeddable.class) != null) {

                String entityName = ctClass.getSimpleName();
                if (entity != null && !entity.name().isEmpty()) {
                    entityName = entity.name();
                }//if

                String entityClass = entityClasses.get(entityName);
                if (entityClass != null) {
                    throw new JPALiteToolingException("Entity [" + entityName + "] name found in [" + ctClass.getName() + "] is already defined in [" + entityClass + "]");
                }//if
                entityClasses.put(entityName, ctClass.getName());

                toolClass(ctClass);
            }//if

        }//try
        catch (ClassNotFoundException | IOException | CannotCompileException ex) {
            throw new JPALiteToolingException(ERROR_PROCESSING_FILE, ex);
        }//catch
    }//processClass

    @SuppressWarnings("java:S6126") //The IDE insert tabs and spaces into a text block resulting in a compile error
    @Override
    public void process(String outputDir, List<String> classList) throws JPALiteToolingException
    {
        String vAttrs = "\",\n" +
                        "\"allDeclaredConstructors\": true,\n" +
                        "\"allPublicConstructors\": true,\n" +
                        "\"allDeclaredMethods\": true,\n" +
                        "\"allPublicMethods\": true,\n" +
                        "\"allDeclaredFields\": true,\n" +
                        "\"allPublicFields\": true }\n";

        this.outputDir = outputDir;
        try {
            entityClasses.clear();
            converterClasses.clear();

            classPool.insertClassPath(outputDir);
            classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            classPool.appendSystemPath();

            Files.createDirectories(Path.of(outputDir + "/META-INF/services"));
            Files.createDirectories(Path.of(outputDir + "/META-INF/native-image/org.jpalite.persistent"));

            //Build a list of all the converter classes first
            for (String className : classList) {
                CtClass ctClass = classPool.get(className);
                processConvertClass(ctClass);
            }//for

            //Process the entity classes
            for (String className : classList) {
                CtClass ctClass = classPool.get(className);
                processClass(ctClass);
            }//for

            boolean first = true;
            if (!entityClasses.isEmpty() || !converterClasses.isEmpty()) {
                try (FileOutputStream outputStream = new FileOutputStream(outputDir + "/META-INF/persistenceUnits.properties");
                     FileOutputStream nativeImageStream = new FileOutputStream(outputDir + "/META-INF/native-image/org.jpalite.persistent/reflect-config.json")) {
                    nativeImageStream.write("[\n".getBytes());

                    for (Map.Entry<String, String> entry : entityClasses.entrySet()) {
                        outputStream.write(entry.getKey().getBytes());
                        outputStream.write('=');
                        outputStream.write(entry.getValue().getBytes());
                        outputStream.write('\n');

                        if (first) {
                            first = false;
                        } else {
                            nativeImageStream.write(',');
                        }//else

                        nativeImageStream.write("{\n\"name\": \"".getBytes());
                        nativeImageStream.write(entry.getValue().getBytes());
                        nativeImageStream.write(vAttrs.getBytes());

                    }

                    if (!converterClasses.isEmpty()) {
                        try (FileOutputStream converterStream = new FileOutputStream(outputDir + "/META-INF/services/org.jpalite.FieldConvertType")) {
                            for (CtClass convertClass : converterClasses) {

                                try {
                                    converterStream.write(convertClass.getName().getBytes());
                                    converterStream.write('\n');

                                    if (first) {
                                        first = false;
                                    } else {
                                        nativeImageStream.write(',');
                                    }//else

                                    nativeImageStream.write("{\n\"name\": \"".getBytes());
                                    nativeImageStream.write(convertClass.getName().getBytes());
                                    nativeImageStream.write(vAttrs.getBytes());

                                    CtMethod convertToEntityAttribute = convertClass.getDeclaredMethod("convertToEntityAttribute");
                                    if (convertToEntityAttribute != null) {
                                        nativeImageStream.write(",{\n\"name\": \"".getBytes());
                                        nativeImageStream.write(convertToEntityAttribute.getReturnType().getName().getBytes());
                                        nativeImageStream.write(vAttrs.getBytes());
                                    }
                                }
                                catch (Exception ex) {
                                    LOG.error("Converter {} not correctly implemented", convertClass.getName());
                                }
                            }//for
                        }//try

                        nativeImageStream.write(']');
                    }
                }//try
            }

        }//try
        catch (NotFoundException ex) {
            throw new JPALiteToolingException(ERROR_PROCESSING_FILE, ex);
        }
        catch (IOException ex) {
            throw new JPALiteToolingException("IO Error creating persistenceUnits file", ex);
        }//catch
    }//process

}//JPALiteToolingImpl
