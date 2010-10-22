package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.module.*

class ResourceModulesBuilderTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }
    
    void testDefaultBundleFalse() {
        def modules = []
        def bld = new ModulesBuilder(modules)
        
        bld.testModule {
            defaultBundle false
            resource uri:'simile/simile.css'
            resource uri:'simile/simile.js'
        }
        
        assertEquals 1, modules.size()
        assertEquals 'testModule', modules[0].name
        assertEquals false, modules[0].defaultBundle
        assertEquals 2, modules[0].resources.size()
        assertTrue modules[0].resources.every { it.bundle == null }
    }
}