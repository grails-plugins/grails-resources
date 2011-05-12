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
            config : [grails:[resources:[work:[dir:'./test-tmp']]]]
        ]
        svc.servletContext = [
            getResource: { uri -> 
                assertTrue uri.indexOf('#') < 0
                new URL('file:./test/test-files'+uri) 
            },
            getMimeType: { uri -> "test/nothing" }
        ]
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testPrepareURIWithHashFragment() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

    void testBuildResourceURIForGrails1_4() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

    void testBuildResourceURIForGrails1_3AndLower() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

}
