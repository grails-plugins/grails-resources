package org.grails.plugin.resource

import grails.util.Environment
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ResourceTagLib {
    static namespace = "r"
    
    static writeAttrs( attrs, output) {
        // Output any remaining user-specified attributes
        attrs.each { k, v ->
           output << k
           output << '="'
           output << v.encodeAsHTML()
           output << '" '    
        }
    }

    static LINK_WRITERS = [
        js: { url, constants, attrs ->
            def o = new StringBuilder()
            o << "<script src=\"${url.encodeAsHTML()}\" "

            // Output info from the mappings
            writeAttrs(constants, o)
            writeAttrs(attrs, o)

            o << '></script>'
            return o    
        },
        
        link: { url, constants, attrs ->
            def o = new StringBuilder()
            o << "<link href=\"${url.encodeAsHTML()}\" "

            // Output info from the mappings
            writeAttrs(constants, o)
            writeAttrs(attrs, o)

            o << '/>'
            return o
        }
    ]
    
    static LINK_RESOURCE_MAPPINGS = [
        css:[type:"text/css", rel:'stylesheet'],
        rss:[type:'application/rss+xml', rel:'alternate'], 
        atom:[type:'application/atom+xml', rel:'alternate'], 
        favicon:[type:'image/x-icon', rel:'shortcut icon'],
        appleicon:[type:'image/x-icon', rel:'apple-touch-icon'],
        js:[writer:'js', type:'text/javascript']
    ]

    static LINK_EXTENSIONS_TO_TYPES = [
        ico:'favicon',
        gif:'favicon',
        png:'favicon'
    ]
    
    def resourceService
    
    boolean usingResource(url) {
        url = url.toString()
        if (log.debugEnabled) {
            log.debug "Checking if this request has already pulled in [$url]"
        }
        def trk = request.resourceTracker
        if (trk == null) {
            trk = new HashSet()
            request.resourceTracker = trk
        }
        
        if (!trk.contains(url)) {
            trk.add(url)
            if (log.debugEnabled) {
                log.debug "This request has not already pulled in [$url]"
            }
            return true
        } else {
            if (log.debugEnabled) {
                log.debug "This request has already pulled in [$url], we'll be smart and skip it"
            }
            return false
        }
    }
    
    boolean notAlreadyDeclared(name) {
        def trk = request.resourceModuleTracker
        if (!trk) {
            trk = new HashSet()
            request.resourceModuleTracker = trk
        }
        
        if (!trk.contains(name)) {
            trk.add(name)
            return true
        } else {
            return false
        }
    }
    
    /**
     * Render an appropriate resource link for an external resource
     * This accepts a "url" attribute which is a Map like that passed to g.resource,
     * or "uri" attribute which is an app-relative uri e.g. 'js/main.js
     * or "plugin"/"dir"/"file" attributes like g.resource
     *
     * This is *not* just for use with declared resources, you can use it for anything e.g. feeds.
     * The "type" attribute can override the type e.g. "rss" if the type cannot be extracted from the extension of
     * the url.
     */
    def resourceLink = { attrs ->
        if (log.debugEnabled) {
            log.debug "resourceLink with $attrs"
        }

        def url = attrs.remove('url')
        def defer = attrs.defer

        if (url == null) {
            if (attrs.uri) {
                // Might be app-relative resource URI 
                url = r.resource(uri:attrs.remove('uri'), defer:defer)
            } else {
                url = r.resource(plugin:attrs.remove('plugin'), dir:attrs.remove('dir'), 
                    file:attrs.remove('file'), defer:defer).toString()
            }
        } else if (url instanceof Map) {
            url = r.resource(url.clone+[defer:defer]).toString()
        }
    
        if (defer) {
            // Just get out, we've called r.resource which has created the implicit resource and added it to implicit module
            // and layoutResources will render the implicit module
            return
        }
        
        if (usingResource(url)) {
            def t = attrs.remove('type')
            if (!t) {
                def extUrl = url.indexOf('?') > 0 ? url[0..url.indexOf('?')-1] : url
                def ext = extUrl[url.lastIndexOf('.')+1..-1]
                t = LINK_EXTENSIONS_TO_TYPES[ext]
                if (!t) {
                    t = ext
                }
            }
            if (log.debugEnabled) {
                log.debug "Resource [${url}] has type [$t]"
            }
            
            def typeInfo = LINK_RESOURCE_MAPPINGS[t]?.clone() 
            if (!typeInfo) {
                throwTagError "Unknown resourceLink type: ${t}"
            }
        
            def writerName = typeInfo.remove('writer')
            def writer = LINK_WRITERS[writerName ?: 'link']
            def wrapper = attrs.remove('wrapper')

            // Allow attrs to overwrite any constants
            attrs.each { typeInfo.remove(it.key) }

            def output = writer(url, typeInfo, attrs)
            if (wrapper) {
                out << wrapper(output)
            } else {
                out << output
            }
        }
    }
    
    /**
     * Indicate that a page requires a named resource module
     * This is stored in the request until layoutResources is called, we then sort out what needs rendering or not later
     */
    def dependsOn = { attrs ->
        def trk = request.resourceDependencyTracker
        if (!trk) {
            trk = [ResourceService.IMPLICIT_MODULE] // Always include this
            request.resourceDependencyTracker = trk
        }
        
        def moduleNames = attrs.module ? [attrs.module] : attrs.modules.split(',')*.trim()
        moduleNames?.each { name ->
            if (log.debugEnabled) {
                log.debug "Checking if module [${name}] is already declared for this page..."
            }
            if (notAlreadyDeclared(name)) {
                if (log.debugEnabled) {
                    log.debug "Adding module [${name}] declaration for this page..."
                }
                trk << name  
            }
        }
    }
    
    /**
     * Render the resources. First invocation renders head JS and CSS, second renders deferred JS only, and any more spews.
     */
    def layoutResources = { attrs ->
        def trk = request.resourceDependencyTracker
        if (!request.resourceRenderedHeadResources) {
            if (log.debugEnabled) {
                log.debug "Rendering non-deferred resources..."
            }
            trk.each { module ->
                out << r.renderModule(name:module, deferred:false)
            }
            request.resourceRenderedHeadResources = true
        } else if (!request.resourceRenderedFooterResources) {
            if (log.debugEnabled) {
                log.debug "Rendering deferred resources..."
            }
            trk.each { module ->
                out << r.renderModule(name:module, deferred:true)
            }
            request.resourceRenderedFooterResources = true
        } else {
            throw new RuntimeException('You have invoked [layoutResources] more than twice. Invoke once in head and once in footer only.')
        }
    }
    
    /**
     * For inline javascript that needs to be either deferred to the end of the page or put into "E.S.P." (in future)
     */
    def script = { attrs, body ->
        // @todo impl this. Accept "defer" (put in footer) "scope" (user = don't externalise, default = externalise with E.S.P.)
        out << body()
    }
    
    /**
     * Render the resources of the given module, and all its dependencies
     * Boolean attribute "deferred" determines whether or not the JS with "defer:true" gets rendered or not
     */
    def renderModule = { attrs ->
        def name = attrs.name
        if (log.debugEnabled) {
            log.debug "renderModule ${attrs}"
        }

        if (log.debugEnabled) {
            log.debug "Getting info for module [${name}]"
        }

        def module = resourceService.getModule(name)
        if (!module) {
            if (name != ResourceService.IMPLICIT_MODULE) {
                throw new IllegalArgumentException("No module found with name [$name]")
            } else {
                // No implicit module, fine
                return
            }
        }
        
        def s = new StringBuilder()
        
        def renderingDeferred = (attrs.deferred?.toString().toBoolean()) ?: false // Convert null to false

        // Write out any dependent modules first
        if (module.dependsOn) {
            if (log.debugEnabled) {
                log.debug "Rendering the dependencies of module [${name}]"
            }
            module.dependsOn.each { modName ->
                s << r.renderModule(name:modName, deferred:renderingDeferred)
            }
        }
        
        if (log.debugEnabled) {
            log.debug "Rendering the resources of module [${name}]"
        }
        
        def debugMode = (Environment.current == Environment.DEVELOPMENT) && params.debugResources
        def _defaultDefer = defaultDefer
        
        module.resources.each { r ->
            if (!r.exists()) {
                throw new IllegalArgumentException("Module [$name] depends on resource [${r.sourceUrl}] but the file cannot be found")
            }
            def resIsDeferred = r.defer == null ? _defaultDefer : r.defer
            log.debug "Res: ${r.sourceUrl} - defer ${r.defer} - rendering deferred ${renderingDeferred}"
            if (resIsDeferred == renderingDeferred) {
                def args = r.tagAttributes?.clone() ?: [:]
                args.uri = debugMode ? r.sourceUrl : r.actualUrl
                args.wrapper = r.prePostWrapper
                if (log.debugEnabled) {
                    log.debug "Rendering one of the module's resource links: ${args}"
                }
                s << resourceLink(args)
                s << '\n'
            }
        }
        out << s
    }

    boolean getDefaultDefer() {
        // @todo eval this only once, is config change dependent
        def _defaultDefer = ConfigurationHolder.config.grails.resources.defer.default 
        if (!(_defaultDefer instanceof Boolean)) {
            _defaultDefer = true
        }
        return _defaultDefer
    }
    
    /**
     * Get the URL for an ad-hoc resource - NOT for declared resources
     * @todo this currently won't work for absolute="true" invocations, it should just passthrough these
     */
    def resource = { attrs ->
        def ctxPath = request.contextPath
        def defer = attrs.remove('defer')
        def uri = attrs.uri ? ctxPath+attrs.uri : g.resource(attrs).toString()
        def debugMode = (Environment.current == Environment.DEVELOPMENT) && params.debugResources

        // Get out quick and add param to tell filter we don't want any fancy stuff
        if (debugMode) {
            out << uri+"?debug=y"
            return
        }
        
        // Chop off context path
        def reluri = uri[ctxPath.size()..-1]
        // Get or create ResourceMeta
        def res = resourceService.getResourceMetaForURI(reluri, true, { res ->
            log.debug "Defer is $defer"
            if (defer != null) {
                res.defer = defer.toBoolean()
            }
            log.debug "Resource Defer is ${res.defer}"
        })
        if (res) {
            out << ctxPath+resourceService.staticUrlPrefix+res.actualUrl
        } else {
            // We don't know about this, back out and use grails URI
            if (log.warnEnabled) {
                log.warn "Invocation of <r:resource> for a resource that apparently doesn't exist: $uri"
            }
            out << uri
        }
    }
}
