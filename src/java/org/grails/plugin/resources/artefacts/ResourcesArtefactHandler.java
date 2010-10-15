package org.grails.plugin.resources.artefacts;

import org.codehaus.groovy.grails.commons.*;

public class ResourcesArtefactHandler extends AbstractResourcesArtefactHandler {

    static public final String TYPE = "Resources";
    static public final String SUFFIX = "Resources";
    
    public ResourcesArtefactHandler() {
        super(TYPE, ResourcesClass.class, DefaultResourcesClass.class, SUFFIX);
    }

}