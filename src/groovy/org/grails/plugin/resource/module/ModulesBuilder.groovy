package org.grails.plugin.resource.module

import org.slf4j.LoggerFactory

/**
 * Implements the resource modules DSL.
 * 
 * The caller provides a list at construction that will be populated during
 * DSL evaluation of maps defining the resource modules.
 */
class ModulesBuilder implements GroovyInterceptable {
    
    private _modules
    private _resources = []
    private _dependencies = []
    private _moduleBuilder = new ModuleBuilder(_resources, _dependencies)
    
    private final log = LoggerFactory.getLogger(this.class.name)
    
    ModulesBuilder(List modules) {
        _modules = modules
    }
    
    def invokeMethod(String name, args) {
        if (args.size() == 1 && args[0] instanceof Closure) {

            // build it
            def moduleDefinition = args[0]
            moduleDefinition.delegate = _moduleBuilder
            moduleDefinition.resolveStrategy = Closure.DELEGATE_FIRST
            moduleDefinition()

            def module = [name: name, resources: _resources.clone(), dependencies: _dependencies.clone()]
            
            if (log.debugEnabled) {
                log.debug("defined module '$module'")
            }
            
            // add it
            _modules << module

            // clear for next
            _resources.clear()
            _dependencies.clear()

        } else {
            throw new IllegalStateException("only 1 closure argument accepted (args were: $args)")
        }
    }

}