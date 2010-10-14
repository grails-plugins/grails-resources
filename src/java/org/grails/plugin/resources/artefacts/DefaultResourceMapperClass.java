package org.grails.plugin.resources.artefacts;

import org.codehaus.groovy.grails.commons.*;
import groovy.lang.Closure;

public class DefaultResourceMapperClass extends AbstractGrailsClass implements ResourceMapperClass {
    public DefaultResourceMapperClass(Class clazz) {
        super(clazz, ResourceMapperArtefactHandler.SUFFIX);
    }
}