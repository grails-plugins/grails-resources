package org.grails.plugin.resources.artefacts;

import org.codehaus.groovy.grails.commons.AbstractGrailsClass;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class DefaultResourceMapperClass extends AbstractGrailsClass implements ResourceMapperClass {
    public DefaultResourceMapperClass(Class<?> clazz) {
        super(clazz, ResourceMapperArtefactHandler.SUFFIX);
    }
}
