package org.grails.plugin.resource

import grails.test.GroovyPagesTestCase

class ResourceTagLibIntegTests extends GroovyPagesTestCase {
    
    def resourceService
    
    protected makeMockResource(uri) {
        [
            uri:uri, 
            disposition:'head', 
            exists: { -> true }
        ]
    }

    def testExternalWithAdhocResourceURI() {
        def result = applyTemplate('<r:external uri="/js/core.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_core.js') != -1

        result = applyTemplate('<r:external uri="js/core.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_core.js') != -1
    }

    def testExternalWithAdhocResourceDirAndFile() {
        def result = applyTemplate('<r:external dir="js" file="core.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_core.js') != -1
    }
}
