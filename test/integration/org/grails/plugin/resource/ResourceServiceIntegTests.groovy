package org.grails.plugin.resource

class ResourceServiceIntegTests extends GroovyTestCase {
    
    def resourceService
    
    protected makeMockResource(uri) {
        [
            uri:uri, 
            disposition:'head', 
            exists: { -> true }
        ]
    }

    def testGettingModulesInDependencyOrder() {
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

        resourceService.modulesByName.putAll(testModules)
        resourceService.updateDependencyOrder()
        
        def moduleNames = resourceService.getAllModuleNamesRequired(modsNeeded)
        println "Module names: ${moduleNames}"
        def moduleNameResults = resourceService.getModulesInDependencyOrder(moduleNames)
        println "Modules: ${moduleNameResults}"

        assertTrue moduleNameResults.indexOf('a') < moduleNameResults.indexOf('b')
        assertTrue moduleNameResults.indexOf('b') < moduleNameResults.indexOf('c')
        assertTrue moduleNameResults.indexOf('b') < moduleNameResults.indexOf('d')
        assertTrue moduleNameResults.indexOf('b') < moduleNameResults.indexOf('e')
        assertTrue moduleNameResults.indexOf('d') < moduleNameResults.indexOf('e')
    }
}
