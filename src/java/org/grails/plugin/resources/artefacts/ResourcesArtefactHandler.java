package org.grails.plugin.resources.artefacts;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class ResourcesArtefactHandler extends AbstractResourcesArtefactHandler {

    public static final String TYPE = "Resources";
    public static final String SUFFIX = "Resources";

    public ResourcesArtefactHandler() {
        super(TYPE, ResourcesClass.class, DefaultResourcesClass.class, SUFFIX);
    }
}
