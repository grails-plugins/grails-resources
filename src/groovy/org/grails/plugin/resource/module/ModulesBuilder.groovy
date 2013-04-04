package org.grails.plugin.resource.module

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implements the resource modules DSL.
 *
 * The caller provides a list at construction that will be populated during
 * DSL evaluation of maps defining the resource modules.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ModulesBuilder implements GroovyInterceptable {

    private List _modules
    private List _moduleOverrides
    private Map _collatedData
    private ModuleBuilder _moduleBuilder
    private boolean _strict

    static METHODNAME_OVERRIDES = 'overrides'

    private final Logger log = LoggerFactory.getLogger(this.class.name)

    ModulesBuilder(List modules, boolean strict = false) {
        _modules = modules
        _strict = strict
        _collatedData = [resources:[], dependencies:[]]
        _moduleBuilder = new ModuleBuilder(_collatedData)
    }

    def invokeMethod(String name, args) {
        if (args.size() != 1 || !(args[0] instanceof Closure)) {
            throw new IllegalStateException("Only 1 closure argument is accepted (args were: $args)")
        }

        if (name == METHODNAME_OVERRIDES) {
            if (log.debugEnabled) {
                log.debug("Processing module overrides")
            }
            ModulesBuilder nestedBuilder = new ModulesBuilder(_moduleOverrides == null ? [] : _moduleOverrides, false)
            Closure moduleDefinition = args[0]
            moduleDefinition.delegate = nestedBuilder
            moduleDefinition.resolveStrategy = Closure.DELEGATE_FIRST
            moduleDefinition()
            // Copy these nested decls into separate data for post-processing
            _moduleOverrides = nestedBuilder._modules
            return
        }

        if (_strict && _modules.find { m -> m.name == name}) {
            throw new IllegalArgumentException("A module called [$name] has already been defined")
        }

        // build it
        Closure moduleDefinition = args[0]
        moduleDefinition.delegate = _moduleBuilder
        moduleDefinition.resolveStrategy = Closure.DELEGATE_FIRST
        moduleDefinition()

        def module = [
            name: name,
            resources: _collatedData.resources.clone(),
            defaultBundle: _collatedData.defaultBundle,
            dependencies: _collatedData.dependencies.clone()]
        if (log.debugEnabled) {
            log.debug("Defined module '$module'")
        }

        // add it
        _modules << module

        // clear for next
        _collatedData.clear()
        _collatedData.resources = []
        _collatedData.dependencies = []
    }
}
