package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class ResourceServiceTests extends GrailsUnitTestCase {
    def svc
    
    protected void setUp() {
        super.setUp()
        mockLogging(ResourceService, true)
        svc = new ResourceService()
        
        svc.grailsApplication = [
            config : new ConfigObject()
        ]
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testDeclareModule() {
        
    }

}
