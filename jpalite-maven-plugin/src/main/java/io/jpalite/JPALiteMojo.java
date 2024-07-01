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

package io.jpalite;

import io.jpalite.impl.JPALiteToolingImpl;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "jpalite", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class JPALiteMojo extends AbstractMojo
{
	private static final String LINE_SEPARATOR = "------------------------------------------------------------------------";
	private static final Logger LOG = LoggerFactory.getLogger(JPALiteMojo.class);

	@Parameter(defaultValue = "${project}", property = "tsjpa.project", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "false", property = "tsjpa.skip", required = false)
	private boolean skip;

	private List<String> getClassList(String pDir) throws IOException
	{
		List<String> vClassList = new ArrayList<>();
		Path vInput = Paths.get(pDir);
		if (vInput.toFile().exists()) {
			Files.walkFileTree(vInput, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					if (Files.isRegularFile(file)) {
						String vFilename = file.getFileName().toString();
						if (vFilename.endsWith(".class")) {
							String vPath = file.getParent().toString();
							if (vPath.equals(pDir)) {
								vPath = "";
							}//if
							else {
								vPath = vPath.substring(pDir.length() + 1).replace("/", ".") + ".";
							}//else

							String vNameSpace = vPath + vFilename.substring(0, vFilename.length() - 6);
							vClassList.add(vNameSpace);
						}//if
					}//if
					return FileVisitResult.CONTINUE;
				}
			});
		}//if

		return vClassList;
	}

	@SuppressWarnings({"java:S2112", "UrlHashCode"}) //We need URLs here
	public void execute() throws MojoExecutionException
	{
		if (skip) {
			getLog().info("Skipping executing.");
			return;
		}//if

		URLClassLoader contextClassLoader = null;
		ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
		try {
			//Setup the classpath for the plugin
			Set<URL> urls = new HashSet<>();
			for (String resource : project.getRuntimeClasspathElements()) {
				urls.add(new File(resource).toURI().toURL());
			}//for

			urls.add(new File(project.getBuild().getTestOutputDirectory()).toURI().toURL());

			contextClassLoader = URLClassLoader.newInstance(
					urls.toArray(new URL[0]),
					Thread.currentThread().getContextClassLoader());

			Thread.currentThread().setContextClassLoader(contextClassLoader);

			JPALiteTooling tooling = new JPALiteToolingImpl();

			//Process runtime classes
			LOG.info(LINE_SEPARATOR);
			List<String> list = getClassList(project.getBuild().getOutputDirectory());
			LOG.info("JPA Tooling found {} runtime classes", list.size());
			LOG.info(LINE_SEPARATOR);
			tooling.process(project.getBuild().getOutputDirectory(), list);

			//Process test classes
			LOG.info(LINE_SEPARATOR);
			list = getClassList(project.getBuild().getTestOutputDirectory());
			LOG.info("JPA Tooling found {} test classes", list.size());
			LOG.info(LINE_SEPARATOR);
			tooling.process(project.getBuild().getTestOutputDirectory(), list);
		}//try
		catch (IOException ex) {
			throw new MojoExecutionException("Error build class list ", ex);
		}//catch
		catch (JPALiteToolingException ex) {
			LOG.error("Error executing JPALite Tooling class", ex);
			throw new MojoExecutionException("Error executing JPALite Tooling class");
		}
		catch (DependencyResolutionRequiredException ex) {
			LOG.error("Error loading JPALite Tooling class", ex);
			throw new MojoExecutionException("Error loading JPALite Tooling class");
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentLoader);
			if (contextClassLoader != null) {
				try {
					contextClassLoader.close();
				}//try
				catch (IOException ex) {
					//ignore
				}//catch
			}//if
		}//finally
	}//execute
}//JPALiteMojo
