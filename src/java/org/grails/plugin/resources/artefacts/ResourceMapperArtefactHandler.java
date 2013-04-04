package org.grails.plugin.resources.artefacts;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class ResourceMapperArtefactHandler extends AbstractResourcesArtefactHandler {

    public static final String TYPE = "ResourceMapper";
    public static final String SUFFIX = "ResourceMapper";

    public ResourceMapperArtefactHandler() {
        super(TYPE, ResourceMapperClass.class, DefaultResourceMapperClass.class, SUFFIX);
    }
}
