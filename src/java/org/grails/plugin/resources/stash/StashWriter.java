package org.grails.plugin.resources.stash;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes a stash to the output.
 *
 * @author Patrick Jungermann
 */
public interface StashWriter {

    /**
     * Writes the stash's content to the writer.
     *
     * @param out      The output writer.
     * @param stash    The stash.
     * @throws IOException if there was a problem with writing the content to the writer.
     */
    void write(Writer out, List<String> stash) throws IOException;

}
