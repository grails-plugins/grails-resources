package org.grails.plugin.resources.artefacts;


/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class ResourcesArtefactHandler extends AbstractResourcesArtefactHandler {

    static public final String TYPE = "Resources";
    static public final String SUFFIX = "Resources";
    
    public ResourcesArtefactHandler() {
        super(TYPE, ResourcesClass.class, DefaultResourcesClass.class, SUFFIX);
    }

}