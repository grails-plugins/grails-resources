package org.grails.plugin.resources.stash;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes stashed scripts to the output. Each fragment will be written to separate script tags as an error
 * inside of one of them would stop the execution of the following ones. This might be harmful, e.g.,
 * in case of (externally provided and/or dependent) tracking scripts.
 *
 * @author Patrick Jungermann
 */
public class ScriptStashWriter implements StashWriter {

    @Override
    public void write(final Writer out, final List<String> stash) throws IOException {
        for (final String fragment : stash) {
            out.write("<script type=\"text/javascript\">");
            out.write(fragment);
            out.write("</script>");
        }
    }

}
