package org.grails.plugin.resources.stash

import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest

import javax.servlet.http.HttpServletRequest
import static org.junit.Assert.*

/**
 * Integration tests for {@link StashManager}.
 *
 * @author Patrick Jungermann
 */
class StashManagerIntegrationTests  {

    @Test
    void shouldBeInitializedWithStashWriterForScripts() {
        assertTrue StashManager.STASH_WRITERS["script"] instanceof ScriptStashWriter
    }

    @Test
    void shouldBeInitializedWithStashWriterForStyles() {
        assertTrue StashManager.STASH_WRITERS["style"] instanceof StyleStashWriter
    }

    @Test(expected = NullPointerException)
    void stashFragmentButNoRequest() {
        String type = "fragment-type"
        String disposition = "my-disposition"
        String fragment = "test fragment"

        StashManager.stashPageFragment(null, type, disposition, fragment)
    }

    @Test
    void stashScriptFragment() {
        HttpServletRequest request = new MockHttpServletRequest()
        String type = "script"
        String disposition = "my-disposition"
        String fragment = "test fragment"

        StashManager.stashPageFragment(request, type, disposition, fragment)

        List<String> stash = (List<String>) request["resources.plugin.page.fragments:script:my-disposition"]
        assertTrue stash.contains(fragment)
    }

    @Test
    void stashStyleFragment() {
        HttpServletRequest request = new MockHttpServletRequest()
        String type = "style"
        String disposition = "my-disposition"
        String fragment = "test fragment"

        StashManager.stashPageFragment(request, type, disposition, fragment)

        List<String> stash = (List<String>) request["resources.plugin.page.fragments:style:my-disposition"]
        assertTrue stash.contains(fragment)
    }

    @Test
    void stashCustomTypedFragment() {
        HttpServletRequest request = new MockHttpServletRequest()
        String type = "custom-type"
        String disposition = "my-disposition"
        String fragment = "test fragment"

        StashManager.stashPageFragment(request, type, disposition, fragment)

        List<String> stash = (List<String>) request["resources.plugin.page.fragments:custom-type:my-disposition"]
        assertTrue stash.contains(fragment)
    }

    @Test
    void unstashScriptFragments() {
        StringWriter writer = new StringWriter()
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        request["resources.plugin.page.fragments:script:my-disposition"] = [
                "script-fragment-1;",
                "script-fragment-2;"
        ]

        StashManager.unstashPageFragments(writer, request, disposition)

        String script = "<script type=\"text/javascript\">script-fragment-1;</script><script type=\"text/javascript\">script-fragment-2;</script>"
        assertEquals script, writer.toString()
    }

    @Test
    void unstashStyleFragments() {
        StringWriter writer = new StringWriter()
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        request["resources.plugin.page.fragments:style:my-disposition"] = [
                "style-fragment-1;",
                "style-fragment-2;"
        ]

        StashManager.unstashPageFragments(writer, request, disposition)

        String style = "<style type=\"text/css\">style-fragment-1;style-fragment-2;</style>"
        assertEquals style, writer.toString()
    }

    @Test
    void unstashCustomTypedFragments() {
        StringWriter writer = new StringWriter()
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        StashManager.STASH_WRITERS["custom-type"] = new CustomTypeStashWriter()
        request["resources.plugin.page.fragments:custom-type:my-disposition"] = [
                "script-fragment-1;",
                "script-fragment-2;"
        ]

        StashManager.unstashPageFragments(writer, request, disposition)

        String expected = "<ul><li>script-fragment-1;</li><li>script-fragment-2;</li></ul>"
        assertEquals expected, writer.toString()
    }

    @Test
    void unstashFragmentsOfMultipleTypes() {
        StringWriter writer = new StringWriter()
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        request["resources.plugin.page.fragments:script:my-disposition"] = [
                "script-fragment-1;",
                "script-fragment-2;"
        ]
        request["resources.plugin.page.fragments:style:my-disposition"] = [
                "style-fragment-1;",
                "style-fragment-2;"
        ]

        StashManager.unstashPageFragments(writer, request, disposition)

        String style = "<style type=\"text/css\">style-fragment-1;style-fragment-2;</style>"
        String script = "<script type=\"text/javascript\">script-fragment-1;</script><script type=\"text/javascript\">script-fragment-2;</script>"
        assertEquals style + script, writer.toString()
    }

    @Test
    void ignoreStashesWithoutRegisteredStashWriter() {
        StringWriter writer = new StringWriter()
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        request["resources.plugin.page.fragments:script:my-disposition"] = [
                "script-fragment-1;",
        ]
        request["resources.plugin.page.fragments:ignored:my-disposition"] = [
                "script-fragment-1;",
        ]

        StashManager.unstashPageFragments(writer, request, disposition)

        String script = "<script type=\"text/javascript\">script-fragment-1;</script>"
        assertEquals script, writer.toString()
    }

    @Test(expected = NullPointerException)
    void unstashScriptFragmentsButNoWriter() {
        HttpServletRequest request = new MockHttpServletRequest()
        String disposition = "my-disposition"

        request["resources.plugin.page.fragments:script:my-disposition"] = [
                "script-fragment-1;",
                "script-fragment-2;"
        ]

        StashManager.unstashPageFragments(null, request, disposition)
    }

    @Test(expected = NullPointerException)
    void unstashScriptFragmentsButNoRequest() {
        StringWriter writer = new StringWriter()
        String disposition = "my-disposition"

        StashManager.unstashPageFragments(writer, null, disposition)
    }



}
class CustomTypeStashWriter implements StashWriter {

    @Override
    void write(Writer out, List<String> stash) throws IOException {
        out << "<ul>"
        for (String fragment in stash) {
            out << "<li>"
            out << fragment
            out << "</li>"
        }
        out << "</ul>"
    }

}