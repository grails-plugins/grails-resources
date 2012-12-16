package org.grails.plugin.resources.artefacts;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class ResourceMapperArtefactHandler extends AbstractResourcesArtefactHandler {

    static public final String TYPE = "ResourceMapper";
    static public final String SUFFIX = "ResourceMapper";
    
    public ResourceMapperArtefactHandler() {
        super(TYPE, ResourceMapperClass.class, DefaultResourceMapperClass.class, SUFFIX);
    }

}