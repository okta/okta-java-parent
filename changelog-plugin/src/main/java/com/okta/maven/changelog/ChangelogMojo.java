/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.maven.changelog;

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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, aggregator = true)
public class ChangelogMojo extends AbstractMojo {

    /**
     * Output directory for the reports. Note that this parameter is only
     * relevant if the goal is run from the command line or from the default
     * build lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is
     * used instead.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.reporting.outputDirectory}/")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * Regex to be used to filter git tags for proper versions.
     */
//    @Parameter(defaultValue = "${project.artifactId}-(?<version>.*)")
    @Parameter(defaultValue = "okta-parent-(?<version>.*)")
    private String gitTagToVersionRegex;

    @Override
    public void execute() throws MojoExecutionException {

        try {
            getCommitMessages();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed generate report", e);
        }
    }

    private Repository gitRepository() throws IOException {
        return new FileRepositoryBuilder()
          .readEnvironment() // scan environment GIT_* variables
          .findGitDir() // scan up the file system tree
          .build();
    }

    List<CommitMessage> getCommitMessages() throws IOException, GitAPIException {

        Repository repository = gitRepository();
        Map<String, String> tags = getTags(repository);

        Git git = new Git(repository);
        Iterator<RevCommit> logEntries = git.log()
                    .setRevFilter(RevFilter.MERGE_BASE)
                    .call()
                    .iterator();

        String describe = git.describe().setLong(false).call();
//        Pattern.compile(gitTagToVersionRegex).matcher(describe)

        // it is easier just to regex the describe to see if the HEAD commit is a tag
        Pattern describeParts = Pattern.compile(gitTagToVersionRegex + "-(?<num>\\d)+-(?<sha>\\p{Alnum}+)";

        boolean firstCommitIsTag = describeParts.matcher(describe).matches();

        int index = 0;
        while(logEntries.hasNext()) {
            RevCommit commit = logEntries.next();
            String commitHash = commit.getName();


            System.out.println(commit.getFullMessage());

        }

        return null;
    }

    Map<String, String> getTags(Repository repository) {
        Map<String, String> tags = new TreeMap<>(Collections.reverseOrder());

        Pattern pattern = Pattern.compile(gitTagToVersionRegex);
        repository.getTags().entrySet().forEach(entry -> {
            Matcher matcher = pattern.matcher(entry.getKey());
            if (matcher.matches()) {
                String version = matcher.group("version");
                tags.put(entry.getValue().getObjectId().getName(), version);
            }
        });
        return tags;
    }


    ChangelogMojo setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    ChangelogMojo setProject(MavenProject project) {
        this.project = project;
        return this;
    }
}