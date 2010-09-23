package org.grails.resources

class ResourceModulesBuilder implements GroovyInterceptable {
    
    def _modules = []
    
    def invokeMethod(String name, args) {
        def c = (Closure)args[0]
        def m = new ModuleBuilder(c)
        m._build()
        _modules << [name:name, depends:m._depends, resources:m._resources]
    }
}