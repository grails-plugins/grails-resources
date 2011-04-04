package org.grails.plugin.resource

import java.util.concurrent.ConcurrentHashMap

import grails.util.Environment
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.util.WebUtils
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.io.FilenameUtils
import javax.servlet.ServletRequest
import grails.util.Environment

import grails.spring.BeanBuilder
import org.grails.plugin.resource.mapper.ResourceMappersFactory
import org.grails.plugin.resource.module.*
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.plugins.PluginManagerHolder

import org.grails.plugin.resource.util.ResourceMetaStore

/**
 * This is where it all happens.
 *
 * This class loads resource declarations (see reload()) and receives requests from the servlet filter to
 * serve resources. It will process the resource if necessary the first time, and then return the file.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceService implements InitializingBean {
    
    static transactional = false

    static IMPLICIT_MODULE = "__@adhoc-files@__"
    static SYNTHETIC_MODULE = "__@synthetic-files@__"
    static REQ_ATTR_DEBUGGING = 'resources.debug'
    
    static DEFAULT_MODULE_SETTINGS = [
        css:[disposition: 'head'],
        rss:[disposition: 'head'],
        gif:[disposition: 'head'],
        jpg:[disposition: 'head'],
        png:[disposition: 'head'],
        ico:[disposition: 'head'],
        js:[disposition: 'defer']
    ]

    def staticUrlPrefix
    
    private File workDir
    
    def modulesByName = new ConcurrentHashMap()

    def processedResourcesByURI = new ResourceMetaStore()
    def syntheticResourcesByURI = new ConcurrentHashMap()

    def modulesInDependencyOrder = []
    
    def resourceMappers
    
    def grailsApplication
    def servletContext
    
    void updateDependencyOrder() {
        def modules = (modulesByName.collect { it.value }).findAll { !(it.name in [IMPLICIT_MODULE, SYNTHETIC_MODULE]) }
        def ordered = modules.collect { it.name }

        modules.each { m ->
            def currentIdx = ordered.indexOf(m.name)
            m.dependsOn?.each { dm ->
                def idx = ordered.indexOf(dm)
                if (idx > currentIdx) {
                    ordered.remove(m.name)
                    ordered.add(idx, m.name)
                    currentIdx = idx
                }
            }
        }
        
        ordered << IMPLICIT_MODULE
        ordered << SYNTHETIC_MODULE 
        
        modulesInDependencyOrder = ordered
    }
    
    void afterPropertiesSet() {
        if (!servletContext) {
            servletContext = grailsApplication.mainContext.servletContext
        }
    }
    
    File getWorkDir() {
        // @todo this isn't threadsafe at startup if its lazy. We should change it.
        if (!this.@workDir) {
            def d = getConfigParamOrDefault('work.dir', null)
            this.@workDir = d ? new File(d) : new File(WebUtils.getTempDir(servletContext), "grails-resources")
        }
        assert this.@workDir
        return this.@workDir
    }
    
    def getPluginManager() {
        // The plugin manager bean configured in integration testing is not the real thing and causes errors.
        // Using the pluginManager from the holder means that we always get a legit instance.
        // http://jira.codehaus.org/browse/GRAILSPLUGINS-2712
        PluginManagerHolder.pluginManager
    }
    
    def extractURI(request, adhoc) {
        def uriStart = (adhoc ? request.contextPath : request.contextPath+staticUrlPrefix).size()
        return uriStart < request.requestURI.size() ? request.requestURI[uriStart..-1] : ''
    }

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
        def uri = ResourceService.removeQueryParams(extractURI(request, true))
        // @todo query params are lost at this point for ad hoc resources, this needs fixing
        def res
        try {
            res = getResourceMetaForURI(uri, true)
        } catch (FileNotFoundException fnfe) {
            response.sendError(404, fnfe.message)
            return
        }
        
        if (Environment.current == Environment.DEVELOPMENT) {
            if (res) {
                response.setHeader('X-Grails-Resources-Original-Src', res?.sourceUrl)
            }
        }
        if (res?.exists()) {
            redirectToActualUrl(res, request, response)
        } else {
            response.sendError(404)
        }
    }
    
    /**
     * Redirect the client to the actual processed Url, used for when an ad-hoc resource is accessed
     */
    void redirectToActualUrl(ResourceMeta res, request, response) {
        // Now redirect the client to the processed url
        // NOTE: only works for local resources
        def u = request.contextPath+staticUrlPrefix+res.linkUrl
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
        def uri = ResourceService.removeQueryParams(extractURI(request, false))
        def inf
        try {
            inf = getResourceMetaForURI(uri)
        } catch (FileNotFoundException fnfe) {
            response.sendError(404, fnfe.message)
            return
        }
        
        if (Environment.current == Environment.DEVELOPMENT) {
            if (inf) {
                response.setHeader('X-Grails-Resources-Original-Src', inf.sourceUrl)
            }
        }

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
                response.setDateHeader('Last-Modified', inf.originalLastMod)

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
                
                // @todo Could we do something faster here? Feels wrong, buffer size is tiny in Groovy
                response.outputStream << data
            } finally {
                data?.close()
            }
        } else {
            response.sendError(404)
        }
    }
    
    /**
     * See if we have a ResourceMeta for this URI.
     * @return null if not processed/created yet, the instance if it exists
     */
    ResourceMeta findSyntheticResourceForURI(String uri) {
        syntheticResourcesByURI[uri]
    }
    
    /**
     * See if we have a ResourceMeta for this URI.
     * @return null if not processed/created yet, the instance if it exists
     */
    ResourceMeta findResourceForURI(String uri) {
        processedResourcesByURI[uri]
    }
    
    ResourceMeta newSyntheticResource(String uri, Class<ResourceMeta> type) {
        if (log.debugEnabled) {
            log.debug "Creating synthetic resource of type ${type} for URI [${uri}]"
        }
        def synthModule = getOrCreateSyntheticOrImplicitModule(true)
        def agg = synthModule.addNewSyntheticResource(type, uri, this)
        agg.processedFile = makeFileForURI(uri)
        
        if (log.debugEnabled) {
            log.debug "synthetic module resources: ${synthModule.resources}"
        }
        
        // Need to store this somewhere so GET requests can look up bundle as it is a synthetic resource
        syntheticResourcesByURI[uri] = agg
        
        return agg
    }
    
    ResourceModule getOrCreateSyntheticOrImplicitModule(boolean synthetic) {
        def mod
        def moduleName = synthetic ? SYNTHETIC_MODULE : IMPLICIT_MODULE
        // We often get multiple simultaneous requests at startup and this causes
        // multiple creates and loss of concurrently processed resources
        synchronized (moduleName) {
            mod = getModule(moduleName)
            if (!mod) {
                if (log.debugEnabled) {
                    log.debug "Creating module: $moduleName"
                }
                defineModule(moduleName)
                mod = getModule(moduleName)
            }
        }
        return mod
    }
    
    /**
     * Get the existing or create a new ad-hoc ResourceMeta for the URI.
     * @returns The resource instance - which may have a null processedFile if the resource cannot be found
     */
    ResourceMeta getResourceMetaForURI(uri, adHocResource = true, Closure postProcessor = null) {

        // Declared resources will already exist, but ad-hoc or synthetic may need to be created
        def res = processedResourcesByURI.getOrCreateAdHocResource(uri) { -> 

            if (!adHocResource) {
                throw new IllegalArgumentException(
                    "We can't create resources on the fly unless they are 'ad-hoc'! Resource URI: $uri")
            }
            
            // If we don't already have it, its either not been declared in the DSL or its Synthetic and its
            // not already been retrieved
            boolean synthetic = false
            def r = syntheticResourcesByURI[uri]
            if (r) {
                synthetic = true
            }

            def mod = getOrCreateSyntheticOrImplicitModule(synthetic)
    
            if (!r) {
                // Need to create ad-hoc resource, its not synthetic
                if (log.debugEnabled) {
                    log.debug "Creating new implicit resource for ${uri}"
                }
                r = new ResourceMeta(sourceUrl: uri, workDir: getWorkDir(), module:mod)
            }
        
            r = prepareResource(r, adHocResource)

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
            
            return r
        } // end of closure

        return res
    }
    
    /**
     * Workaround for replaceAll problems with \ in Java
     */
    String makeFileSystemPathFromURI(uri) {
        def chars = uri.chars
        chars.eachWithIndex { c, i ->
            if (c == '/') {
                chars[i] = File.separatorChar
            }
        }
        new String(chars)
    }
    
    File makeFileForURI(String uri) {
        def splitPoint = uri.lastIndexOf('/')
        def fileSystemDir = splitPoint > 0 ? makeFileSystemPathFromURI(uri[0..splitPoint-1]) : ''
        def fileSystemFile = makeFileSystemPathFromURI(uri[splitPoint+1..-1])
//        println "Getting workDir: ${workDir} - this ${this}, this.getWorkDir = ${this.getWorkDir()}"
        def staticDir = new File(getWorkDir(), fileSystemDir)
        
        // force the structure
        if (!staticDir.exists()) {
            // Do not assert this, we are re-entrant and may get multiple simultaneous calls.
            // We just want to be sure one of them works
            staticDir.mkdirs()
            if (!staticDir.exists()) {
                log.error "Unable to create static resource cache directory: ${staticDir}"
            }
        }
        
        if (log.debugEnabled) {
            log.debug "Creating file object for URI [$uri] from [${staticDir}] and [${fileSystemFile}]"
        }
        def f = new File(staticDir, fileSystemFile)
        // Delete the existing file - it may be from previous release, we cannot tell.
        if (f.exists()) {
            assert f.delete()
        }
        return f
    }
    
    /**
     * Execute the processing chain for the resource, returning list of URIs to add to uri -> resource mappings
     * for this resource
     */
    ResourceMeta prepareResource(ResourceMeta r, boolean adHocResource) {
        if (log.debugEnabled) {
            log.debug "Preparing resource ${r.sourceUrl} (${r.dump()})"
        }
        if (r.delegating) {
            if (log.debugEnabled) {
                log.debug "Skipping prepare resource for [${r.sourceUrl}] as it is delegated"
            }
            return
        }
        
        if (!adHocResource && findResourceForURI(r.sourceUrl)) {
            log.warn "Skipping prepare resource for [${r.sourceUrl}] - You have multiple modules declaring this same resource."
        }

        def uri = r.sourceUrl
        if (!uri.contains('://')) {
            r.beginPrepare(this)
            
            if (!r.processedFile?.exists()) {
                def origResourceURL = servletContext.getResource(uri)
                if (!origResourceURL) {
                    if (log.errorEnabled) {
                        log.error "Resource not found: ${uri} when preparing resource ${r.dump()}"
                    }
                    throw new FileNotFoundException("Cannot locate resource [$uri]")
                }
        
                r.contentType = servletContext.getMimeType(uri)
                if (log.debugEnabled) {
                    log.debug "Resource [$uri] has content type [${r.contentType}]"
                }

                def conn = origResourceURL.openConnection()
                def origResource = origResourceURL.newInputStream()
                try {
                    def f = makeFileForURI(uri)
                    // copy the file ready for mutation
                    r.processedFile = f
                    r.originalLastMod = conn.lastModified
                    
                    r.actualUrl = r.sourceUrl

                    // Now copy in the resource from this app deployment into the cache, ready for mutation
                    r.processedFile << origResource
                } finally {
                    conn = null
                    origResource?.close()                    
                    origResource = null
                }
            }

            // Now iterate over the mappers...
            if (log.debugEnabled) {
                log.debug "Applying mappers to ${r.processedFile}"
            }
        
            // Apply all mappers / or only those until the resource becomes delegated
            // Once delegated, its the delegate that needs to be processed, not the original
            for (m in resourceMappers) {
                if (log.debugEnabled) {
                    log.debug "Applying mapper ${m.name} to ${r.processedFile} - delegating? ${r.delegating}"
                }
                if (r.delegating) {
                    break;
                }
                m.invokeIfNotExcluded(r)
                if (log.debugEnabled) {
                    log.debug "Applied mapper ${m.name} to ${r.processedFile}"
                }
                r.wasProcessedByMapper(m)
            }
            
            r.endPrepare(this)
        } else {
            r.actualUrl = r.sourceUrl

            log.warn "Skipping mappers for ${r.actualUrl} because its an absolute URL."
        }
        
        return r
    }
        
    void storeModule(ResourceModule m) {
        if (log.debugEnabled) {
            log.debug "Storing resource module definition ${m.dump()}"
        }
        
        m.resources.each { r ->
            processedResourcesByURI.addDeclaredResource { ->
                prepareResource(r, false)
            }
        }
        modulesByName[m.name] = m
    }
    
    def defineModule(String name) {
        storeModule(new ResourceModule(name, this))
    }

    /**
     * @deprecated
     */
    def module(String name, String url) {
        storeModule(new ResourceModule(name, [url:url], false, this))
    }

    /**
     * @deprecated
     */
    def module(String name, Map urlInfo) {
        storeModule(new ResourceModule(name, urlInfo, false, this))
    }

    /**
     * @deprecated
     */
    def module(String name, List urlsOrInfos) {
        storeModule(new ResourceModule(name, urlsOrInfos, false, this))
    }

    /**
     * @deprecated
     */
    def module(String name, List urlsOrInfos, List moduleDeps) {
        def m = new ResourceModule(name, urlsOrInfos, false, this)
        storeModule(m)
        moduleDeps?.each { d ->
            m.addModuleDependency(d)
        }
    }
    
    def module(builderInfo) {
        def m = new ResourceModule(builderInfo.name, builderInfo.resources, builderInfo.defaultBundle, this)
        storeModule(m)
        builderInfo.dependencies?.each { d ->
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
        if (log.infoEnabled) {
            log.info "Forgetting all known resources..."
        }
        modulesByName.clear()
        modulesInDependencyOrder.clear()
        syntheticResourcesByURI.clear()
        processedResourcesByURI = new ResourceMetaStore()
    }
    
    private loadResources() {
        if (log.infoEnabled) {
            log.info "Loading resource declarations..."
        }
        forgetResources()
        
        def declarations = ModuleDeclarationsFactory.getModuleDeclarations(grailsApplication)
        
        def modules = []
        def builder = new ModulesBuilder(modules)

        declarations.each { sourceClassName, dsl ->
            if (log.debugEnabled) {
                log.debug("evaluating resource modules from $sourceClassName")
            }
            
            dsl.delegate = builder
            dsl.resolveStrategy = Closure.DELEGATE_FIRST
            dsl()
        }

        // Always do app modules after
        def appModules = ModuleDeclarationsFactory.getApplicationConfigDeclarations(grailsApplication)
        if (appModules) {
            if (log.debugEnabled) {
                log.debug("evaluating resource modules from application Config")
            }
            appModules.delegate = builder
            appModules.resolveStrategy = Closure.DELEGATE_FIRST
            appModules()
        }
        
        if (log.debugEnabled) {
            log.debug("resource modules after evaluation: $modules")
        }
        
        // Now merge in any overrides
        if (log.debugEnabled) {
            log.debug "Merging in module overrides ${builder._moduleOverrides}"
        }
        builder._moduleOverrides.each { overriddenModule ->
            if (log.debugEnabled) {
                log.debug "Merging in module overrides for ${overriddenModule}"
            }
            def existingModule = modules.find { it.name == overriddenModule.name }
            if (existingModule) {
                if (overriddenModule.defaultBundle) {
                    if (log.debugEnabled) {
                        log.debug "Overriding module [${existingModule.name}] defaultBundle with [${overriddenModule.defaultBundle}]"
                    }
                    existingModule.defaultBundle = overriddenModule.defaultBundle
                }
                if (overriddenModule.dependencies) {
                    if (log.debugEnabled) {
                        log.debug "Overriding module [${existingModule.name}] dependencies with [${overriddenModule.dependencies}]"
                    }
                    // Replace, not merge
                    existingModule.dependencies = overriddenModule.dependencies
                }
                overriddenModule.resources.each { res ->
                    def existingResources = existingModule.resources.findAll { 
                        it.id ? (it.id == res.id) : (it.url == res.id)
                    }
                    if (existingResources) {
                        if (log.debugEnabled) {
                            log.debug "Overriding ${overriddenModule.name} resources with id ${res.id} with "+
                                "new settings: ${res}"
                        }
                        // Merge, not replace - for each matching resource
                        existingResources.each { r ->
                            r.putAll(res)
                        }
                    }
                } 
            } else {
                if (log.warnEnabled) {
                    log.warn "Attempt to override resource module ${overriddenModule.name} but "+
                        "there is nothing to override, this module does not exist"
                }
            }
        }
        
        // Create modules and prepare the resources
        modules.each { m -> module(m) }
        
        // Now pre-prepare the bundles
        prepareSyntheticResources()
    }
    
    /**
     * Prepare the resources that were generated as a result of loading other modules
     * e.g. the bundles
     */
    void prepareSyntheticResources() {
        def resources = modulesByName[SYNTHETIC_MODULE]?.resources

        if (log.infoEnabled) {
            log.info "Preparing declared synthetic resources: ${resources?.sourceUrl}"
        }
        resources?.each { r ->            
            processedResourcesByURI.addDeclaredResource { ->
                prepareResource(r, false)
            }
        }
    }
    
    static removeQueryParams(uri) {
        def qidx = uri.indexOf('?')
        qidx > 0 ? uri[0..qidx-1] : uri
    }
    
    def getDefaultSettingsForURI(uri, typeOverride = null) {
        
        if (!typeOverride) {
            // Strip off query args
            def extUrl = ResourceService.removeQueryParams(uri)
            
            def ext = FilenameUtils.getExtension(extUrl)
            if (log.debugEnabled) {
                log.debug "Extension extracted from ${uri} ([$extUrl]) is ${ext}"
            }
            typeOverride = ext
        }
        
        DEFAULT_MODULE_SETTINGS[typeOverride]
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
                s1 << "             -- id: ${resource.id}\n"
                s1 << "             -- original Url: ${resource.originalUrl}\n"
                s1 << "             -- local file: ${resource.processedFile}\n"
                s1 << "             -- mime type: ${resource.contentType}\n"
                s1 << "             -- actual Url: ${resource.actualUrl}\n"
                s1 << "             -- source Extension: ${resource.sourceUrlExtension}\n"
                s1 << "             -- query params/fragment: ${resource.sourceUrlParamsAndFragment}\n"
                s1 << "             -- url for linking: ${resource.linkUrl}\n"
                s1 << "             -- link override: ${resource.linkOverride}\n"
                s1 << "             -- attributes: ${resource.attributes}\n"
                s1 << "             -- tag attributes: ${resource.tagAttributes}\n"
                s1 << "             -- disposition: ${resource.disposition}\n"
                s1 << "             -- delegating?: ${resource.delegate ? 'Yes: '+resource.delegate.actualUrl : 'No'}\n"
            }
        }
        def s2 = new StringBuilder()
        processedResourcesByURI.keySet().sort().each { uri ->
            def res = processedResourcesByURI[uri]
            s2 << "Resource URI: ${uri} => ${res.processedFile}\n"
        }
        def s3 = new StringBuilder()
        syntheticResourcesByURI.keySet().sort().each { uri ->
            def res = syntheticResourcesByURI[uri]
            s3 << "Resource URI: ${uri} => ${res.processedFile}\n"
        }
        updateDependencyOrder()
        def s4 = "Dependency load order: ${modulesInDependencyOrder}\n"
        
        if (toLog) {
            log.debug '-'*50
            log.debug "Resource definitions"
            log.debug(s1)
            log.debug '-'*50
            log.debug "Resource URI cache"
            log.debug '-'*50
            log.debug(s2)
            log.debug '-'*50
            log.debug "Synthetic Resources"
            log.debug '-'*50
            log.debug(s3)
            log.debug '-'*50
            log.debug "Module load order"
            log.debug '-'*50
            log.debug(s4)
            log.debug '-'*50
        } 
        return s1.toString() + s2.toString() + s3.toString() + s4.toString()
    }
    
    /**
     * Returns the config object under 'grails.resources'
     */
    ConfigObject getConfig() {
        grailsApplication.config.grails.resources
    }
    
    /**
     * Used to retrieve a resources config param, or return the supplied
     * default value if no explicit value was set in config
     */
    def getConfigParamOrDefault(String key, defaultValue) {
        def param = key.tokenize('.').inject(config) { conf, v -> conf[v] }

        if (param instanceof ConfigObject) {
            param.size() == 0 ? defaultValue : param
        } else {
            param
        }
    }
    
    boolean isDebugMode(ServletRequest request) {
        if (getConfigParamOrDefault('debug', false)) {
            config.debug
        } else if (request != null) {
            isExplicitDebugRequest(request)
        } else {
            false
        }
    }
    
    private isExplicitDebugRequest(ServletRequest request) {
        if (Environment.current == Environment.DEVELOPMENT) {
            def requestContainsDebug = request.getParameter('_debugResources') != null
            def wasReferredFromDebugRequest = request.getHeader('Referer')?.contains('?_debugResources=')

            requestContainsDebug || wasReferredFromDebugRequest
        } else {
            false
        }
    }
    
    def reload() {
        log.info("Performing a full reload")
        resourceMappers = ResourceMappersFactory.createResourceMappers(grailsApplication, config.mappers)
        loadResources()
    }
}
