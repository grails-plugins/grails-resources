package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

class ResourceService {

    def pluginManager
    
    static transactional = false

    def resourcesByModule = [:]

    def moduleNamesByBundle = [:]
    
    List<Closure> resourceMappers = []
        
    /**
     * Resource mappers can mutate URLs any way they like. They are exeecuted in the order
     * registered, so plugins must use dependsOn & loadAfter to set their ordering correctly before
     * they register with us
     * The closure must take 3 args - the current resource url, the original type of the resource 
     * (js/css/img) and any attributes declared on the resource
     */    
    void addResourceMapper(Closure mapper) {
        resourceMappers << mapper
    }
    
    void storeModule(ResourceModule m) {
        m.applyMappings(resourceMappers)
        resourcesByModule[m.name] = m
    }
    
    def module(String name, String url) {
        storeModule(new ResourceModule(name, [url:url]))
    }

    def module(String name, Map urlInfo) {
        storeModule(new ResourceModule(name, urlInfo))
    }

    def module(String name, List urlsOrInfos) {
        storeModule(new ResourceModule(name, urlsOrInfos))
    }

    def module(String name, List urlsOrInfos, List moduleDeps) {
        def m = new ResourceModule(name, urlsOrInfos)
        storeModule(m)
        moduleDeps?.each { d ->
            m.addModuleDependency(d)
        }
    }
    
    /*
    def bundle(String name, resourceNames) {
        def info = []
        info.addAll(resourceNames)
        resourceNamesByBundle[name] = info
    }
*/

    /**
     * Resolve a resource to a URL by resource name
     */
    def getModule(name) {
        resourcesByModule[name]
    }
    
/*
    def resourcesForBundle(String bundleName) {
        
    }
*/
}
