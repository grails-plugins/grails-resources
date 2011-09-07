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

    def testExternalWithAdhocResourceURIThatIsExcluded() {
        def result = applyTemplate('<r:external uri="js/core.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/js/core.js') != -1
    }

    def testExternalWithAdhocResourceDirAndFileThatIsExcluded() {
        def result = applyTemplate('<r:external dir="js" file="core.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/js/core.js') != -1
    }

    def testExternalWithAdhocResourceURI() {
        def result = applyTemplate('<r:external uri="js/adhoc.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }

    def testExternalWithAdhocResourceURIWithSlash() {
        def result = applyTemplate('<r:external uri="/js/adhoc.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }

    def testExternalWithAdhocResourceDirAndFile() {
        def result = applyTemplate('<r:external dir="js" file="adhoc.js"/>', [:])
        println "Result: ${result}"
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }
}
