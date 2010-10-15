package org.grails.plugin.resource.module

import org.slf4j.LoggerFactory

/**
 * Implements the DSL for a resource as part of a module.
 * 
 * Is designed to be reused for multiple invocations. The caller is
 * responsible for clearing the given resources and dependencies between invocations.
 */
class ModuleBuilder {
    
    private final _resources
    private final _dependencies

    private final log = LoggerFactory.getLogger(this.class.name)
    
    ModuleBuilder(List resources, List dependencies) {
        _resources = resources
        _dependencies = dependencies
    }
        
    void dependsOn(String[] dependencies) {
        _dependencies.addAll(dependencies.toList())
    } 
    
    void resource(args) {
        _resources << args
    }
    
    def missingMethod(String name, args) {
        throw new RuntimeException("Sorry - flavours are not yet supported by the builder!")
    }
}