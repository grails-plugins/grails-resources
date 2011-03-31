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
        def output = tagLib.resourceLink(uri:'test.less', rel:'stylesheet/less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
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
        def output = tagLib.resourceLink(uri:'test.less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
    }
}
