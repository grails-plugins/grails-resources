package org.grails.plugin.resource

import org.apache.commons.io.FileUtils

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class ResourceProcessorTests extends GrailsUnitTestCase {
    def svc
    
    protected void setUp() {
        super.setUp()
        mockLogging(ResourceProcessor, true)
        FileUtils.cleanDirectory(new File('./test-tmp/'));

        svc = new ResourceProcessor()
        
        svc.grailsApplication = [
            config : [grails:[resources:[work:[dir:'./test-tmp']]]],
            mainContext : [servletContext:[
                getResource: { uri -> 
                    assertTrue uri.indexOf('#') < 0
                    new URL('file:./test/test-files'+uri) 
                },
                getMimeType: { uri -> "test/nothing" }
            ]]
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

    void testProcessAdHocResourceIncludesExcludes() {
        
        svc.adHocIncludes = ['/**/*.css', '/**/*.js', '/images/**']
        svc.adHocExcludes = ['/**/*.exe', '/**/*.gz', '/unsafe/**/*.css']

        def testData = [
            [requestURI: '/css/main.css', expected:true],
            [requestURI: '/js/code.js', expected:true],
            [requestURI: '/css/logo.png', expected:false],
            [requestURI: '/images/logo.png', expected:true],
            [requestURI: '/downloads/virus.exe', expected:false],
            [requestURI: '/downloads/archive.tar.gz', expected:false],
            [requestURI: '/unsafe/nested/problematic.css', expected:false]
        ]
        
        testData.each { d ->
            def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]
            
            // We know if it tried to handle it if it 404s, we can't be bothered to creat resourcemeta for all those
            def didHandle = false
            def response = [
                sendError: { code, msg = null -> didHandle = (code == 404) },
                sendRedirect: { uri -> }
            ]
            
            svc.processAdHocResource(request, response)
            
            assertEquals "Failed on ${d.requestURI}", d.expected, didHandle
        }
    }

    void testProcessAdHocResourceIncludesExcludesSpecificFile() {
        
        svc.adHocIncludes = ['/**/*.js']
        svc.adHocExcludes = ['/**/js/something.js']

        def testData = [
            [requestURI: '/js/other.js', expected:true],
            [requestURI: '/js/something.js', expected:false],
            [requestURI: 'js/something.js', expected:false],
            [requestURI: '/xxx/js/something.js', expected:false],
            [requestURI: 'xxx/js/something.js', expected:false]
        ]
        
        testData.each { d ->
            def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]
            
            // We know if it tried to handle it if it 404s, we can't be bothered to create resourcemeta for all those
            def didHandle = false
            def response = [
                sendError: { code, msg = null -> didHandle = true },
                sendRedirect: { uri -> }
            ]
            
            svc.processAdHocResource(request, response)
            
            assertEquals "Failed on ${d.requestURI}", d.expected, didHandle
        }
    }
}
