package org.grails.plugin.resources.stash;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes stashed styles to the output. All stashed fragments will be written into one style tag, in order.
 *
 * @author Patrick Jungermann
 */
public class StyleStashWriter implements StashWriter {

    @Override
    public void write(final Writer out, final List<String> stash) throws IOException {
        if (stash.isEmpty()) {
            return;
        }

        out.write("<style type=\"text/css\">");
        for (final String fragment : stash) {
            out.write(fragment);
        }
        out.write("</style>");
    }

}
