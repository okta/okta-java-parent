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
                return [ "10.0.0", "9.0.0", "11.0.0", "10.1.0", "10.0.0-beta" ]
            }
        }

        def tempDir = File.createTempDir()
        def project = mock(MavenProject)
        when(project.getVersion()).thenReturn("version-2-SNAPSHOT")
        when(project.getName()).thenReturn("my-name")
        when(project.getUrl()).thenReturn("https://example.com/url")

        mojo.outputDirectory = tempDir
        mojo.legacyVersions = ["legacy-1", "legacy-2"]
        mojo.project = project

        mojo.execute()

        def outputFile = new File(tempDir, "index.html")
        assertThat outputFile, anExistingFile()

        String html = outputFile.text

        assertThat html, allOf(
                containsString("<a id=\"projectLink\" href=\"https://example.com/url\">my-name</a>"),
                containsString("<a href=\"11.0.0/apidocs/index.html\" target=\"javadocs\">11.0.0 [Current]</a>"),
                containsString("<a href=\"10.1.0/apidocs/index.html\" target=\"javadocs\">10.1.0</a>"),
                containsString("<a href=\"10.0.0/apidocs/index.html\" target=\"javadocs\">10.0.0</a>"),
                containsString("<a href=\"10.0.0-beta/apidocs/index.html\" target=\"javadocs\">10.0.0-beta</a>"),
                containsString("<a href=\"9.0.0/apidocs/index.html\" target=\"javadocs\">9.0.0</a>"),
                containsString("<a href=\"development/apidocs/index.html\" target=\"javadocs\">Development</a>"),
                containsString("<a href=\"legacy-1/apidocs/index.html\" target=\"javadocs\">legacy-1 [Legacy]</a>"),
                containsString("<a href=\"legacy-2/apidocs/index.html\" target=\"javadocs\">legacy-2 [Legacy]</a>"),
                containsString("<iframe style=\"width: 100%; height: 100vh\" src=\"11.0.0/apidocs/index.html\" name=\"javadocs\"></iframe>") // currently selected version
        )
    }

    @Test
    void noTagsTest() {
        def mojo = new DocListMojo() {
            @Override
            List<String> getVersions() throws IOException {
                return []
            }
        }

        def tempDir = File.createTempDir()
        def project = mock(MavenProject)
        when(project.getVersion()).thenReturn("version-2-SNAPSHOT")
        when(project.getName()).thenReturn("my-name")
        when(project.getUrl()).thenReturn("https://example.com/url")

        mojo.outputDirectory = tempDir
        mojo.legacyVersions = []
        mojo.project = project

        mojo.execute()

        def outputFile = new File(tempDir, "index.html")
        assertThat outputFile, anExistingFile()

        String html = outputFile.text

        assertThat html, allOf(
                containsString("<a id=\"projectLink\" href=\"https://example.com/url\">my-name</a>"),
                containsString("<a href=\"development/apidocs/index.html\" target=\"javadocs\">Development [Current]</a>"),
                containsString("<iframe style=\"width: 100%; height: 100vh\" src=\"development/apidocs/index.html\" name=\"javadocs\"></iframe>") // currently selected version
        )
    }
}
