package org.grails.plugin.resource

import grails.test.*

class ResourceTagLibTests extends TagLibUnitTestCase {
    protected void setUp() {
        super.setUp()
        
        Object.metaClass.encodeAsHTML = { -> delegate.toString() }
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testResourceLinkWithRelOverride() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/css/test.less'
        testMeta.actualUrl = '/css/test.less'
        testMeta.disposition = 'head'
        
        tagLib.resourceService = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.resourceLink(uri:'/css/test.less', rel:'stylesheet/less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
        assertTrue output.contains('href="/static/css/test.less"')
    }

    void testResourceLinkWithRelOverrideFromResourceDecl() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/css/test.less'
        testMeta.actualUrl = '/css/test.less'
        testMeta.contentType = "stylesheet/less"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [rel:'stylesheet/less']
        
        tagLib.resourceService = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.resourceLink(uri:'/css/test.less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
        assertTrue output.contains('href="/static/css/test.less"')
    }

    void testImgTagWithAttributes() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/images/test.png'
        testMeta.actualUrl = '/images/test.png'
        testMeta.contentType = "image/png"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [width:'100', height:'50', alt:'mugshot']
        
        tagLib.resourceService = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.img(uri:'/images/test.png').toString()
        println "Output was: $output"
        assertTrue output.contains('width="100"')
        assertTrue output.contains('height="50"')
        assertTrue output.contains('alt="mugshot"')
        assertTrue output.contains('src="/static/images/test.png"')
        assertFalse output.contains('uri=')
    }
    
    def testDebugModeResourceLinkWithAbsoluteCDNURL() {

        def url = 'https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js'
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = url
        testMeta.actualUrl = url
        testMeta.disposition = 'head'
        
        tagLib.request.contextPath = "/resourcestests"
        
        tagLib.resourceService = [
            isDebugMode: { r -> true },
            getResourceMetaForURI: { uri, adhoc, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.resourceLink(uri:url, type:"js").toString()
        println "Output was: $output"
        assertTrue output.contains('src="https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js?_debugResources')
    }
}
