package org.grails.plugin.resources.artefacts;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public abstract class AbstractResourcesArtefactHandler extends ArtefactHandlerAdapter {

    public AbstractResourcesArtefactHandler(String type, Class<?> grailsClassType, Class<?> grailsClassImpl, String artefactSuffix) {
        super(type, grailsClassType, grailsClassImpl, artefactSuffix, true);
    }

    @Override
    public String getPluginName() {
        return "resources";
    }
}
