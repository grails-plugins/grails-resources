package org.grails.plugin.resource

import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

import org.grails.resources.ResourceModulesBuilder

/**
 * @todo Move all this code out into a standard Groovy bean class and declare the bean in plugin setup
 * so that if this is pulled into core, other plugins are not written to depend on this service
 */
class ResourceService {

    def pluginManager
    
    static transactional = false

    static IMPLICIT_MODULE = "__@legacy-files@__"
    
    // @todo make this Config, default to something in /tmp
    def staticFilePath = '/tmp/grails/static'
    File staticFileDir = new File(staticFilePath)
    def staticUrlPrefix
    
    def modulesByName = [:]

    def processedResourcesByURI = new ConcurrentHashMap()

    def moduleNamesByBundle = [:]
    
    List resourceMappers = []
        
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
            redirectToActualUrl(res, request, response)
        } else {
            response.sendError(404)
        }
    }
    
    void redirectToActualUrl(ResourceMeta res, request, response) {
        // Now redirect the client to the processed url
        def u = request.contextPath+staticUrlPrefix+res.actualUrl
        if (log.debugEnabled) {
            log.debug "Redirecting ad-hoc resource ${request.requestURI} to $u which makes it UNCACHEABLE - declare this resource "+
                "and use resourceLink/module tags to avoid redirects and enable client-side caching"
        }
        response.sendRedirect(u)
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
        def uri = request.requestURI[(request.contextPath+staticUrlPrefix).size()..-1]
        def inf = getResourceMetaForURI(uri)
        
        // See if its an ad-hoc resource that has come here via a relative link
        // @todo make this development mode only by default?
        if (inf.actualUrl != uri) {
            redirectToActualUrl(inf, request, response)
            return
        }

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
                        if (log.debugEnabled) {
                            log.debug "Applying request processor on ${request.requestURI}: "+processor.class.name
                        }
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
    
    ResourceMeta getResourceMetaForURI(uri, adHocResource = true, Closure postProcessor = null) {
        def r = processedResourcesByURI[uri]

        // If we don't already have it, its not been declared in the DSL and its
        // not already been retrieved
        if (!r) {
            // We often get multiple simultaneous requests at startup and this causes
            // multiple creates and loss of concurrently processed resources
            def mod
            synchronized (IMPLICIT_MODULE) {
                mod = getModule(IMPLICIT_MODULE)
                if (!mod) {
                    if (log.debugEnabled) {
                        log.debug "Creating implicit module"
                    }
                    defineModule(IMPLICIT_MODULE)
                    mod = getModule(IMPLICIT_MODULE)
                }
            }
            
            // Need to put in cache
            if (log.debugEnabled) {
                log.debug "Creating new implicit resource for ${uri}"
            }
            r = new ResourceMeta(sourceUrl:uri)
        
            // Do the processing
            // @todo we should really sync here on something specific to the resource
            prepareResource(r, adHocResource)
        
            // Only if the URI mapped to a real file, do we add the resource
            // Prevents DoS with zillions of 404s
            if (r.exists()) {
                if (postProcessor) {
                    postProcessor(r)
                }
                synchronized (mod.resources) {
                    // Prevent concurrent requests resulting in multiple additions of same resource
                    if (!mod.resources.find({ x -> x.sourceUrl == r.sourceUrl }) ) {
                        mod.resources << r
                    }
                }
            }
        }
        return r
    }
    
    void prepareResource(ResourceMeta r, boolean adHocResource) {
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
        
        r.contentType = ServletContextHolder.servletContext.getMimeType(uri)
        if (log.debugEnabled) {
            log.debug "Resource [$uri] has content type [${r.contentType}]"
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
            // Delete the existing file - it may be from previous release, we cannot tell.
            if (r.processedFile.exists()) {
                assert r.processedFile.delete()
            }
            // Now copy in the resource from this app deployment
            r.processedFile << origResource
            
            r.actualUrl = r.sourceUrl
            
            // Now iterate over the mappers...
            if (log.debugEnabled) {
                log.debug "Applying mappers to ${r.processedFile}"
            }
            resourceMappers.eachWithIndex { mapperInfo, i ->
                if (log.debugEnabled) {
                    log.debug "Applying static content mapper [${mapperInfo.name}] to ${r.dump()}"
                }

                // Apply mapper if not suppressed for this resource - check attributes
                if (!r.attributes['no'+mapperInfo.name]) {
                    def prevFile = r.processedFile.toString()
                    mapperInfo.mapper(r)
                    
                    // Flag that this mapper has been applied
                    r.attributes['+'+mapperInfo.name] = true
                }

                if (log.debugEnabled) {
                    log.debug "Done applying static content mapper [${mapperInfo.name}] to ${r.dump()}"
                }
            }
            
            if (log.debugEnabled) {
                log.debug "Updating URI to resource cache for ${r.actualUrl} >> ${r.processedFile}"
            }
            
            // Add the actual linking URL to the cache so resourceLink resolves
            processedResourcesByURI[r.actualUrl] = r
            
            // Add the original source url to the cache as well, if it was an ad-hoc resource
            // As the original URL is used, we need this to resolve to the actualUrl for redirect
            if (adHocResource) {
                processedResourcesByURI[r.sourceUrl] = r
            }
        } finally {
            origResource?.close()
        }
    }
    
    /**
     * Resource mappers can mutate URLs any way they like. They are exeecuted in the order
     * registered, so plugins must use dependsOn & loadAfter to set their ordering correctly before
     * they register with us
     * The closure takes 1 arg - the current resource. Any mutations can be performed by 
     * changing actualUrl or processedFile or other propertis of ResourceMeta
     */    
    void addResourceMapper(String name, Closure mapper) {
        resourceMappers << [name:name, mapper:mapper]
    }
    
    void storeModule(ResourceModule m) {
        if (log.debugEnabled) {
            log.debug "Storing resource module definition ${m.dump()}"
        }
        
        m.resources.each { r ->
            prepareResource(r, true)
        }
        modulesByName[m.name] = m
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
    
    /**
     * Resolve a resource to a URL by resource name
     */
    def getModule(name) {
        modulesByName[name]
    }
        
    void forgetResources() {
        modulesByName.clear()
        processedResourcesByURI.clear()
    }
    
    void loadResourcesFromConfig() {
        forgetResources()
        
        // Placeholder code, we might support lists of config closures in future
        def modules = ConfigurationHolder.config.grails.resources.modules
        if (!(modules instanceof Closure)) {
            return
        }
        def moduleClosures = [modules]
        if (log.debugEnabled) {
            log.debug "Loading resource module definitions from Config... "+moduleClosures
        }
        moduleClosures?.each { clo ->
            def builder = new ResourceModulesBuilder()
            clo.delegate = builder
            clo.resolveStrategy = Closure.DELEGATE_FIRST
            clo()
            
            if (log.debugEnabled) {
                log.debug "Resource module definitions for [${builder._modules}] found in Config..."
            }
            builder._modules.each { m ->
                module(m.name, m.resources, m.depends)
            }
        }
    }
    
    def dumpResources(toLog = true) {
        def s1 = new StringBuilder()
        modulesByName.keySet().sort().each { moduleName ->
            def mod = modulesByName[moduleName]
            s1 << "Module: ${moduleName}\n"
            s1 << "   Depends on modules: ${mod.dependsOn}\n"
            def res = []+mod.resources
            res.sort({ a,b -> a.actualUrl <=> b.actualUrl}).each { resource ->
                s1 << "   Resource: ${resource.sourceUrl}\n"
                s1 << "             -- local file: ${resource.processedFile}\n"
                s1 << "             -- mime type: ${resource.contentType}\n"
                s1 << "             -- url for linking: ${resource.actualUrl}\n"
                s1 << "             -- attributes: ${resource.attributes}\n"
                s1 << "             -- tag attributes: ${resource.tagAttributes}\n"
                s1 << "             -- defer: ${resource.defer}\n"
            }
        }
        def s2 = new StringBuilder()
        processedResourcesByURI.keySet().sort().each { uri ->
            def res = processedResourcesByURI[uri]
            s2 << "Request URI: ${uri} => ${res.processedFile}\n"
        }
        if (toLog) {
            log.debug '-'*50
            log.debug "Resource definitions"
            log.debug(s1)
            log.debug '-'*50
            log.debug "Resource URI cache"
            log.debug '-'*50
            log.debug(s2)
            log.debug '-'*50
        }
        return s1.toString() + s2.toString()
    }
}
