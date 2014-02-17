package org.grails.plugin.resource

import grails.test.mixin.integration.IntegrationTestMixin
import grails.test.mixin.TestMixin

@TestMixin(IntegrationTestMixin)
class ResourceProcessorIntegTests {
    
    def grailsResourceProcessor
    
    protected makeMockResource(uri) {
        [
            uri:uri, 
            disposition:'head', 
            exists: { -> true }
        ]
    }

    void testGettingModulesInDependencyOrder() {
        def testModules = [
            a: [name:'a', resources: [ makeMockResource('a.css') ] ],
            b: [name:'b', dependsOn:['a'], resources: [ makeMockResource('b.css') ] ],
            c: [name:'c', dependsOn:['a', 'b'], resources: [ makeMockResource('a.css') ] ],
            d: [name:'d', dependsOn:['b'], resources: [ makeMockResource('a.css') ] ],
            e: [name:'e', dependsOn:['d'], resources: [ makeMockResource('a.css') ] ]
        ]

        def modsNeeded = [
            e: true,
            c: true
        ]

        grailsResourceProcessor.modulesByName.putAll(testModules)
        grailsResourceProcessor.updateDependencyOrder()
        
        def moduleNames = grailsResourceProcessor.getAllModuleNamesRequired(modsNeeded)
        println "Module names: ${moduleNames}"
        def moduleNameResults = grailsResourceProcessor.getModulesInDependencyOrder(moduleNames)
        println "Modules: ${moduleNameResults}"

        assert moduleNameResults.indexOf('a') < moduleNameResults.indexOf('b')
        assert moduleNameResults.indexOf('b') < moduleNameResults.indexOf('c')
        assert moduleNameResults.indexOf('b') < moduleNameResults.indexOf('d')
        assert moduleNameResults.indexOf('b') < moduleNameResults.indexOf('e')
        assert moduleNameResults.indexOf('d') < moduleNameResults.indexOf('e')
    }
}
