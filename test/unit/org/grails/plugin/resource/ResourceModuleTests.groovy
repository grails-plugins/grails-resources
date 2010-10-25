package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.module.*
import org.grails.plugin.resource.*

class ResourceModuleTests extends GrailsUnitTestCase {
    def svc
    
    protected void setUp() {
        super.setUp()
        
        svc = new Expando()
        svc.getDefaultSettingsForURI = { uri, type ->
            [:]
        }
    }

    protected void tearDown() {
        super.tearDown()
    }
    
    void testDefaultBundleFalse() {
        def resources = [
            [url:'simile/simile.css'],
            [url:'simile/simile.js']
        ]
        
        def m = new ResourceModule('testModule', resources, false, svc)
        
        assertEquals 2, m.resources.size()
        assertTrue m.resources.every { it.bundle == null }
    }

    void testDefaultBundling() {
        def resources = [
            [url:'simile/simile.css'],
            [url:'simile/simile.js']
        ]
        
        def m = new ResourceModule('testModule', resources, null, svc)
        
        assertEquals 2, m.resources.size()
        m.resources.each { r -> 
            assertEquals 'testModule', r.bundle
        }
    }

    void testDefaultBundleWithName() {
        def resources = [
            [url:'simile/simile.css'],
            [url:'simile/simile.js']
        ]
        
        def m = new ResourceModule('testModule', resources, "frank-and-beans", svc)

        assertEquals 2, m.resources.size()
        m.resources.each { r -> 
            assertEquals 'frank-and-beans', r.bundle
        }
    }
}