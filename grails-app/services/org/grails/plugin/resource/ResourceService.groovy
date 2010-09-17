package org.grails.plugin.resource

import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.context.ServletContextHolder

class ResourceService {

    def pluginManager
    
    static transactional = false

    static IMPLICIT_MODULE = "__@legacy-files@__"
    
    // @todo make this Config, default to something in /tmp
    def staticFilePath = '/tmp/grails/static'
    File staticFileDir = new File(staticFilePath)
    def staticUrlPrefix
    
    def resourcesByModule = [:]

    def processedResourcesByURI = new ConcurrentHashMap()

    def moduleNamesByBundle = [:]
    
    List<Closure> resourceMappers = []
        
    /**
     * Process a legacy URI that points to a normal resource, not produced with our
     * own tags, and likely not referencing a declared resource.
     * Therefore the URI may not be build-unique and cannot reliably be cached so
     * we have to redirect "Moved Temporarily" to it in case another plugin causes eternal caching etc.
     *
     * To do this, we simply search the cache based on sourceUrl instead of actualUrl
     *
     * This is not recommended, its just a quick out of the box fix for legacy (or pre-"resources plugin" plugin) code.
     *
     * So a request for <ctx>/css/main.css comes in. This needs to redirect to e.g. <ctx>/static/css/342342353345343534.css
     * This involves looking it up by source uri. Therefore the same resource may have multiple mappings in the 
     * processedResourcesByURI map but they should not be conflicting.
     */
    void processAdHocResource(request, response) {
        if (log.debugEnabled) {
            log.debug "Handling ad-hoc resource ${request.requestURI}"
        }
        def res = getResourceMetaForURI(request.requestURI[request.contextPath.size()..-1], false)
        if (res?.exists()) {
            // Now redirect the client to the processed url
            def u = request.contextPath+staticUrlPrefix+res.actualUrl
            if (log.debugEnabled) {
                log.debug "Redirecting ad-hoc resource ${request.requestURI} to $u - declare this resource "+
                    "and use resourceLink/module tags to avoid redirects"
            }
            response.sendRedirect(u)
        } else {
            response.sendError(404)
        }
    }
    
    /**
     * Process a URI where the input URI matches a cached and declared resource URI,
     * without any redirects. This is the real deal
     */
    void processDeclaredResource(request, response) {
        if (log.debugEnabled) {
            log.debug "Handling resource ${request.requestURI}"
        }
        // Find the ResourceMeta for the request, or create it
        def inf = getResourceMetaForURI(request.requestURI[(request.contextPath+staticUrlPrefix).size()..-1])
        
        // If we have a file, go for it
        if (inf?.exists()) {
            if (log.debugEnabled) {
                log.debug "Returning processed resource ${request.requestURI}"
            }
            def data = inf.processedFile.newInputStream()
            try {
                // Now set up the response
                response.contentType = inf.contentType
                response.setContentLength(inf.processedFile.size().toInteger())

                // Here we need to let the mapper add headers etc
                if (inf.requestProcessors) {
                    if (log.debugEnabled) {
                        log.debug "Running request processors on ${request.requestURI}"
                    }
                    inf.requestProcessors.each { processor ->
                        def p = processor.clone()
                        p.delegate = inf
                        p(request, response)
                    }
                }
                
                // Could we do something faster here?
                response.outputStream << data
            } finally {
                data?.close()
            }
        } else {
            response.sendError(404)
        }
    }
    
    ResourceMeta getResourceMetaForURI(uri, byProcessedUrl = true) {
        def r = processedResourcesByURI[uri]
        if (!r) {
            def mod = getModule(IMPLICIT_MODULE)
            if (!mod) {
                if (log.debugEnabled) {
                    log.debug "Creating implicit module"
                }
                defineModule(IMPLICIT_MODULE)
                mod = getModule(IMPLICIT_MODULE)
            }
            // Need to put in cache
            if (log.debugEnabled) {
                log.debug "Creating new implicit resource for ${uri}"
            }
            r = new ResourceMeta(sourceUrl:uri)
            
            // Do the processing
            prepareResource(r, byProcessedUrl)
            
            // Only if the URI mapped to a real file, do we add the resource
            // Prevents DoS with zillions of 404s
            if (r.exists()) {
                mod.resources << r
            }
        }
        return r
    }
    
    void prepareResource(ResourceMeta r, boolean byProcessedUrl) {
        if (log.debugEnabled) {
            log.debug "Preparing resource ${r.sourceUrl}"
        }
        def uri = r.sourceUrl
        def origResource = ServletContextHolder.servletContext.getResourceAsStream(uri)
        if (!origResource) {
            if (log.errorEnabled) {
                log.error "Resource not found: ${uri}"
            }
            return // do nothing, 404s
        }
        
        try {
            def fileSystemDir = uri[0..uri.lastIndexOf('/')-1].replaceAll('/', File.separator)
            def fileSystemFile = uri[uri.lastIndexOf('/')+1..-1].replaceAll('/', File.separator)
            def staticDir = new File(staticFileDir, fileSystemDir)
            
            // force the structure
            if (!staticDir.exists()) {
                assert staticDir.mkdirs()
            }
            
            // copy the file ready for mutation
            r.processedFile = new File(staticDir, fileSystemFile)
            if (!r.processedFile.exists()) {
                r.processedFile << origResource
            }
            
            r.actualUrl = r.sourceUrl
            
            // Now iterate over the mappers...
            if (log.debugEnabled) {
                log.debug "Applying mappers to ${r.processedFile}"
            }
            resourceMappers.eachWithIndex { mapper, i ->
                if (log.debugEnabled) {
                    log.debug "Applying static content mapper $i to ${r.dump()}"
                }
                // Apply mapper
                def prevFile = r.processedFile.toString()
                mapper(r)

                // Update actualUrl if mutated
                if (prevFile != r.processedFile.toString()) {
                    r.actualUrl = (r.processedFile.toString()-staticFilePath.toString()).replace('\\', '/')
                }
                
                if (log.debugEnabled) {
                    log.debug "Done applying static content mapper $i to ${r.dump()}"
                }
            }
            
            if (log.debugEnabled) {
                log.debug "Updating URI to resource cache for ${r.actualUrl} >> ${r.processedFile}"
            }
            processedResourcesByURI[byProcessedUrl ? r.actualUrl : r.sourceUrl] = r
        } finally {
            origResource?.close()
        }
    }
    
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
        if (log.debugEnabled) {
            log.debug "Storing resource module definition ${m.dump()}"
        }
        
        m.resources.each { r ->
            prepareResource(r, true)
        }
        resourcesByModule[m.name] = m
    }
    
    def defineModule(String name) {
        storeModule(new ResourceModule(name))
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
