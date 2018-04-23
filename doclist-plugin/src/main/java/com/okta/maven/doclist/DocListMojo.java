/*
 * Copyright 2017 Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.maven.doclist;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class DocListMojo extends AbstractMojo {

    /**
     * Output directory for the reports. Note that this parameter is only
     * relevant if the goal is run from the command line or from the default
     * build lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is
     * used instead.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.reporting.outputDirectory}/okta-doclist")
    private File outputDirectory;

    /**
     * Regex to be used to filter git tags for proper versions.
     */
    @Parameter(defaultValue = "${project.artifactId}-(?<version>.*)")
    private String gitTagToVersionRegex;

    /**
     * Legacy versions that are NOT tied to branch naming strategy.
     */
    @Parameter(property = "legacyVersions")
    private List<String> legacyVersions;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {

        try {
            // create the directory if needed
            outputDirectory.mkdirs();

            // copy in image
            String imageName = "okta-dev-logo-48.png";
            File imageDest = new File(outputDirectory, "images/" + imageName);
            imageDest.getParentFile().mkdir();
            FileUtils.copyURLToFile(getClass().getResource("/images/" + imageName), imageDest);

            // figure out the current version
            TreeMap<String, String> versionsMap = new TreeMap<>(getVersions().stream()
                    .collect(Collectors.toMap(v -> v, v -> v)));

            String currentVersion = project.getVersion();
            String currentVersionName;
            String devVersion = "development";
            String devVersionName = "Development";

            if (versionsMap.get(currentVersion) != null) {
                currentVersionName = currentVersion + "[Current]";
                versionsMap.put(currentVersion, currentVersionName);
            } else {
                currentVersion = devVersion;
                devVersionName = devVersionName + " [Current]";
                currentVersionName = devVersionName;
            }

            VelocityEngine velocityEngine = new VelocityEngine();
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            velocityEngine.init();
            Template template = velocityEngine.getTemplate("/templates/index.vm");

            VelocityContext context = new VelocityContext();
            context.put("name", project.getName());
            context.put("url", project.getUrl());
            context.put("current", currentVersion);
            context.put("currentName", currentVersionName);
            context.put("versions", versionsMap.descendingMap());
            context.put("legacy", getLegacyVersions());
            context.put("devVersion", devVersion);
            context.put("devVersionName", devVersionName);

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(outputDirectory, "index.html")), StandardCharsets.UTF_8)) {
                template.merge(context, writer);
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed generate report", e);
        }
    }

    private List<String> getLegacyVersions() {
        return legacyVersions;
    }

    private Repository gitRepository() throws IOException {
        return new FileRepositoryBuilder()
          .readEnvironment() // scan environment GIT_* variables
          .findGitDir() // scan up the file system tree
          .build();
    }

    private List<String> getVersions() throws IOException {

        List<String> versions = new ArrayList<>();

        Repository repository = gitRepository();
        Pattern pattern = Pattern.compile(gitTagToVersionRegex);
        repository.getTags().keySet().forEach(tag -> {
            Matcher matcher = pattern.matcher(tag);
            if (matcher.matches()) {
                String version = matcher.group("version");
                versions.add(version);
            }
        });
        versions.sort(Collections.reverseOrder());
        return versions;
    }
}