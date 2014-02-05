package org.grails.plugin.resources.stash

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for {@link ScriptStashWriter}.
 *
 * @author Patrick Jungermann
 */
@TestMixin(GrailsUnitTestMixin)
class ScriptStashWriterUnitTests {

    ScriptStashWriter writer

    @Before
    void setUp() {
        writer = new ScriptStashWriter()
    }

    @Test(expected = NullPointerException)
    void writeButNoOutputTarget() {
        writer.write(null, ["fragment"])
    }

    @Test(expected = NullPointerException)
    void writeButNoStash() {
        writer.write(new StringWriter(), null)
    }

    @Test
    void writeEmptyStash() {
        StringWriter out = new StringWriter()

        writer.write(out, [])

        assertEquals "", out.toString()
    }

    @Test
    void writeStashWithOneFragment() {
        StringWriter out = new StringWriter()

        writer.write(out, ["fragment;"])

        String expected = "<script type=\"text/javascript\">fragment;</script>"
        assertEquals expected, out.toString()
    }

    @Test
    void writeStashWithMultipleFragments() {
        StringWriter out = new StringWriter()

        writer.write(out, ["fragment1;", "fragment2;"])

        String expected = "<script type=\"text/javascript\">fragment1;</script><script type=\"text/javascript\">fragment2;</script>"
        assertEquals expected, out.toString()
    }

}
