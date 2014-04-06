package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.module.*

@TestMixin(GrailsUnitTestMixin)
class ResourceModuleTests {
    def svc
    
    @org.junit.Before
    void setupTest() {
        svc = new Expando()
        svc.getDefaultSettingsForURI = { uri, type ->
            [:]
        }
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
            [url:'simile/simile.css', disposition:'head'],
            [url:'simile/simile.js', disposition:'head']
        ]
        
        def m = new ResourceModule('testModule', resources, null, svc)
        
        assertEquals 2, m.resources.size()
        m.resources.each { r -> 
            assertEquals 'bundle_testModule_head', r.bundle
        }
    }

    void testDefaultBundleWithName() {
        def resources = [
            [url:'simile/simile.css', disposition:'defer'],
            [url:'simile/simile.js', disposition:'defer']
        ]
        
        def m = new ResourceModule('testModule', resources, "frank-and-beans", svc)

        assertEquals 2, m.resources.size()
        m.resources.each { r -> 
            assertEquals 'frank-and-beans_defer', r.bundle
        }
    }

    void testExcludedMapperString() {
        def resources = [
            [url:'simile/simile.js', disposition:'head', exclude:'minify']
        ]
        
        def m = new ResourceModule('testModule', resources, null, svc)
        
        assertEquals 1, m.resources.size()
        assertTrue m.resources[0].excludedMappers.contains('minify')
    }

    void testExcludedMapperSet() {
        def resources = [
            [url:'simile/simile.js', disposition:'head', exclude:['minify']]
        ]
        
        def m = new ResourceModule('testModule', resources, null, svc)
        
        assertEquals 1, m.resources.size()
        assertTrue m.resources[0].excludedMappers.contains('minify')
    }

    void testStringOnlyResource() {
        def resources = [
            'js/test.js'
        ]
        
        def m = new ResourceModule('testModule', resources, null, svc)
        
        assertEquals 1, m.resources.size()
        assertEquals "/js/test.js", m.resources[0].sourceUrl
    }

}