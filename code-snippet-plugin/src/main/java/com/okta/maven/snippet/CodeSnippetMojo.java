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
package com.okta.maven.snippet;

import com.github.javaparser.JavaParser;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This mojo replaces code examples found in a markdown file with snippets from an existing source file. Snippets are added/updated in between the tags <code>[//]: # (method: aMethodName)</code> and <code>[//]: # (end: aMethodName)</code>
 * <br>
 * Assuming the contents of the {@code sourceFile} was:
 * <pre><code>
 * public class Example {
 *   public void doFoo() {
 *      System.out.println("doing foo");
 *      int result = 1 + 1;
 *   }
 * }
 * </code></pre>
 *
 * And the {@code markdownFile} contained:
 * <pre><code>
 * Test
 [//]: # (method: doFoo)
 *     something here
 [//]: # (end: doFoo)
 * </code></pre>
 *
 * The result would be:
 * <pre><code>
  * Test
 [//]: # (method: doFoo)
 * ```java
 * System.out.println("doing foo");
 * int result = 1 + 1;
 * ```
 [//]: # (end: doFoo)
 * </code></pre>
 */
@Mojo(name = "snip", defaultPhase = LifecyclePhase.PROCESS_SOURCES, aggregator = true)
public class CodeSnippetMojo extends AbstractMojo {

    /**
     * Markdown file to be updated based on the source from {@code sourceFile}.
     */
    @Parameter(property = "markdownFile", defaultValue = "${project.basedir}/README.md")
    private File markdownFile = null;

    /**
     * Java source file to be parsed for snippets.
     */
    @Parameter(property = "sourceFile", defaultValue = "${sourceDirectory}/Examples.java")
    private File sourceFile = null;

    @Override
    public void execute() throws MojoExecutionException {

        try {

            if (!markdownFile.exists()) {
                throw new MojoExecutionException("The 'markdownFile' is required and must exist.");
            }

            if (!sourceFile.exists()) {
                throw new MojoExecutionException("The 'sourceFile' is required and must exist.");
            }

            Map<String, String> methodBodyMap = listMethodBodies(sourceFile);
            Path markdownFilePath = markdownFile.toPath();
            String langTag = FileUtils.extension(sourceFile.getName());
            String contents = IOUtil.toString(Files.readAllBytes(markdownFilePath), StandardCharsets.UTF_8.name());
            String result = replaceSnippets(contents, langTag, methodBodyMap);
            FileUtils.fileWrite(markdownFile, result);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed generate report", e);
        }
    }

    private static Map<String, String> listMethodBodies(File sourceFile) throws FileNotFoundException {

        final Map<String, String> methodBodyMap = new HashMap<>();

        new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(final MethodDeclaration method, final Object arg) {
                super.visit(method, arg);

                String body = method.getBody().get().getStatements().stream()
                        .map(statement -> {
                            TokenRange tokenRange = statement.getTokenRange().get();
                            Optional<Comment> comment = statement.getComment();

                            StringBuilder blockAsString = new StringBuilder();
                            if (comment.isPresent()) {
                                blockAsString.append("\n")
                                            .append(comment.get());
                            }
                            blockAsString.append(tokenRange.toString().replaceAll("\n {8}", "\n"));

                            return blockAsString;
                        })
                        .collect(Collectors.joining("\n"));

                // if the body starts or ends with new lines, strip them
                body = StringUtils.strip(body, "\n");

                methodBodyMap.put(method.getNameAsString(), body);
            }
        }.visit(JavaParser.parse(sourceFile), null);

        return methodBodyMap;
    }

    private String replaceSnippets(String fileContents, String langTag, Map<String, String> methodBodyMap) {

        for( Map.Entry<String, String> entry : methodBodyMap.entrySet()) {
            String startText = "[//]: # (method: " + entry.getKey() + ")";
            String endText = "[//]: # (end: " + entry.getKey() + ")";

            Pattern startPattern = Pattern.compile(
                    Pattern.quote(startText) +
                    "(.*)" +
                    Pattern.quote(endText), Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = startPattern.matcher(fileContents);
            StringBuffer stringBuffer = new StringBuffer();
            if (matcher.find()) {
                String text = startText + "\n```" + langTag + "\n" + entry.getValue() + "\n```\n" + endText;
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(text));
            }
            matcher.appendTail(stringBuffer);
            fileContents = stringBuffer.toString();
        }
        return fileContents;
    }

    CodeSnippetMojo setMarkdownFile(File markdownFile) {
        this.markdownFile = markdownFile;
        return this;
    }

    CodeSnippetMojo setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
        return this;
    }
}