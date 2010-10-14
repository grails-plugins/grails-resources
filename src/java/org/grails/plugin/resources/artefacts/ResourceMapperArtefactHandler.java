package org.grails.plugin.resources.artefacts;

import org.codehaus.groovy.grails.commons.*;

public class ResourceMapperArtefactHandler extends AbstractResourcesArtefactHandler {

    static public final String TYPE = "ResourceMapper";
    static public final String SUFFIX = "ResourceMapper";
    
    public ResourceMapperArtefactHandler() {
        super(TYPE, ResourceMapperClass.class, DefaultResourceMapperClass.class, SUFFIX);
    }

}