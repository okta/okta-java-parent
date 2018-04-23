/*
 * Copyright 2018 Okta, Inc.
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
package com.okta.maven.doclist

import org.apache.maven.project.MavenProject
import org.testng.annotations.Test

import static org.hamcrest.Matchers.allOf
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.io.FileMatchers.anExistingFile
import static org.hamcrest.Matchers.containsString

class DocListMojoTest {

    @Test
    void happyPath() {
        def mojo = new DocListMojo() {
            @Override
            List<String> getVersions() throws IOException {
                return ["version-1", "version-2"]
            }
        }

        def tempDir = File.createTempDir()
        def project = mock(MavenProject)
        when(project.getVersion()).thenReturn("version-2")
        when(project.getName()).thenReturn("my-name")
        when(project.getUrl()).thenReturn("http://example.com/url")

        mojo.outputDirectory = tempDir
        mojo.legacyVersions = ["legacy-1", "legacy-2"]
        mojo.project = project

        mojo.execute()

        def outputFile = new File(tempDir, "index.html")
        assertThat outputFile, anExistingFile()

        String html = outputFile.text

        assertThat html, allOf(
                containsString("<a id=\"projectLink\" href=\"http://example.com/url\">my-name</a>"),
                containsString("<a href=\"version-2/apidocs/index.html\" target=\"javadocs\">version-2 [Current]</a>"),
                containsString("<a href=\"version-1/apidocs/index.html\" target=\"javadocs\">version-1</a>"),
                containsString("<a href=\"development/apidocs/index.html\" target=\"javadocs\">Development</a>"),
                containsString("<a href=\"legacy-1/apidocs/index.html\" target=\"javadocs\">legacy-1 [Legacy]</a>"),
                containsString("<a href=\"legacy-2/apidocs/index.html\" target=\"javadocs\">legacy-2 [Legacy]</a>"),
                containsString("<iframe style=\"width: 100%; height: 100vh\" src=\"version-2/apidocs/index.html\" name=\"javadocs\"></iframe>") // currently selected version
        )
    }
}
