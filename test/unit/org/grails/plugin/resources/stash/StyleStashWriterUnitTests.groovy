package org.grails.plugin.resources.stash

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for {@link StyleStashWriter}.
 *
 * @author Patrick Jungermann
 */
@TestMixin(GrailsUnitTestMixin)
class StyleStashWriterUnitTests {

    StyleStashWriter writer

    @Before
    void setUp() {
        writer = new StyleStashWriter()
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

        String expected = "<style type=\"text/css\">fragment;</style>"
        assertEquals expected, out.toString()
    }

    @Test
    void writeStashWithMultipleFragments() {
        StringWriter out = new StringWriter()

        writer.write(out, ["fragment1;", "fragment2;"])

        String expected = "<style type=\"text/css\">fragment1;fragment2;</style>"
        assertEquals expected, out.toString()
    }

}
