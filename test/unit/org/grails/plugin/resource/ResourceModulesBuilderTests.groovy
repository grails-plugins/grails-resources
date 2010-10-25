package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.module.*
import org.grails.plugin.resource.*

class ResourceModulesBuilderTests extends GrailsUnitTestCase {
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
        def modules = []
        def bld = new ModulesBuilder(modules)
        
        bld.testModule {
            defaultBundle false
            resource url:'simile/simile.css'
            resource url:'simile/simile.js'
        }
        
        assertEquals 1, modules.size()
        assertEquals 'testModule', modules[0].name
        assertEquals false, modules[0].defaultBundle
    }

    void testDefaultBundling() {
        def modules = []
        def bld = new ModulesBuilder(modules)
        
        bld.testModule {
            resource url:'simile/simile.css'
            resource url:'simile/simile.js'
        }
        
        assertEquals 1, modules.size()
        assertEquals 'testModule', modules[0].name
        assertNull modules[0].defaultBundle
    }

    void testDefaultBundleWithName() {
        def modules = []
        def bld = new ModulesBuilder(modules)
        
        bld.testModule {
            defaultBundle "frank-and-beans"
            resource url:'simile/simile.css'
            resource url:'simile/simile.js'
        }
        
        assertEquals 1, modules.size()
        assertEquals 'testModule', modules[0].name
        assertEquals 'frank-and-beans', modules[0].defaultBundle
    }
}