package org.grails.plugin.resources.stash;

import org.grails.plugin.resource.util.DispositionsUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Manages the stashing and unstashing of page fragments.
 *
 * @author Patrick Jungermann
 */
public class StashManager {

    /**
     * Prefix used for storing page fragment stashes.
     */
    public static final String REQ_ATTR_PREFIX_PAGE_FRAGMENTS = "resources.plugin.page.fragments";

    /**
     * Registered, usable stash writers.
     */
    public static final Map<String, StashWriter> STASH_WRITERS = new HashMap<String, StashWriter>();
    static {
        // register the basic stash writers
        STASH_WRITERS.put("script", new ScriptStashWriter());
        STASH_WRITERS.put("style", new StyleStashWriter());
    }

    /**
     * Stashes a page fragment.
     *
     * @param request        The current request, at which the page fragment has to be stashed.
     * @param type           The stash's (writer) type.
     * @param disposition    The disposition, at which the page fragment has to be unstashed.
     * @param fragment       The fragment, which has to be stashed.
     */
    @SuppressWarnings("unchecked")
    public static void stashPageFragment(final HttpServletRequest request, final String type, final String disposition, final String fragment) {
        final String resourceTrackerName = makePageFragmentKey(type, disposition);
        DispositionsUtils.addDispositionToRequest(request, disposition, "-page-fragments-");

        List<String> resourceTracker = (List<String>) request.getAttribute(resourceTrackerName);
        if (resourceTracker == null) {
            resourceTracker = new ArrayList<String>();
            request.setAttribute(resourceTrackerName, resourceTracker);
        }
        resourceTracker.add(fragment);
    }

    /**
     * Unstashes the disposition's page fragments.
     *
     * @param out            The target, to which all fragments have to be written to.
     * @param request        The request, at which the fragments have been stashed.
     * @param disposition    The disposition, for which all fragments have to be rendered.
     * @throws IOException if there was any problem with writing the fragments.
     */
    public static void unstashPageFragments(final Writer out, final HttpServletRequest request, final String disposition) throws IOException {
        for (final String type : STASH_WRITERS.keySet()) {
            List<String> stash = consumePageFragments(request, type, disposition);
            if (!stash.isEmpty()) {
                STASH_WRITERS.get(type).write(out, stash);
            }
        }
    }

    /**
     * Returns the stash (all page fragments) of the requested type and disposition.
     *
     * @param request        The request, at which the fragments have been stashed.
     * @param type           The fragments' type.
     * @param disposition    The disposition, for which the fragments have to be returned.
     * @return All fragments of the requested type and disposition.
     */
    @SuppressWarnings("unchecked")
    private static List<String> consumePageFragments(final HttpServletRequest request, final String type, final String disposition) {
        List<String> stash = (List<String>) request.getAttribute(makePageFragmentKey(type, disposition));
        return stash != null ? stash : Collections.EMPTY_LIST;
    }

    /**
     * Returns the page fragment key, used to store the page fragment stashes.
     *
     * @param type           The fragments' type.
     * @param disposition    The fragments' disposition.
     * @return The page fragment key.
     */
    private static String makePageFragmentKey(final String type, final String disposition) {
        return REQ_ATTR_PREFIX_PAGE_FRAGMENTS + ":" + type + ":" + disposition;
    }

}
