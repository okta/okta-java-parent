package com.okta.maven.changelog

import org.testng.annotations.Test

import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat

class CommitMessageTest {

    @Test
    void nullParse() {
        assertThat CommitMessage.parse(null), nullValue()
    }

    @Test
    void emptyStringParse() {
        assertThat CommitMessage.parse(""), nullValue()
    }

    @Test
    void justWhitespaceParse() {
        assertThat CommitMessage.parse("  "), nullValue()
    }

    @Test
    void singleLineWithPrefix() {
        def result = CommitMessage.parse("myPrefix: some subject text")

        //def result = CommitMessage.parse("""myPrefix: some subject text""".stripIndent())
        assertThat result.type, is("myPrefix")
        assertThat result.subject, is("some subject text")
    }

    @Test
    void multiLineWithPrefix() {
        def result = CommitMessage.parse("""myPrefix: some subject text

        More text here
        and more
        """.stripIndent())
        assertThat result.type, is("myPrefix")
        assertThat result.subject, is("some subject text")
    }

    @Test
    void multiLineWithTrailer() {
        def result = CommitMessage.parse("""some subject text

More text here
changelog: myPrefix
and more
        """)
        assertThat result.type, is("myPrefix")
        assertThat result.subject, is("some subject text")
    }

    @Test
    void multiLineNoPrefixOrTrailer() {
        def result = CommitMessage.parse("""some subject text

More text here
and more
        """)
        assertThat result.type, is(null)
        assertThat result.subject, is("some subject text")
    }

    @Test
    void multiLineNoPrefixAndTrailer() {
        def result = CommitMessage.parse("""myPrefix: some subject text

More text here
changelog: wrong
and more
        """)
        assertThat result.type, is("myPrefix")
        assertThat result.subject, is("some subject text")
    }
}