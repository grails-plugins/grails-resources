package org.grails.plugin.resource

import grails.util.Environment
import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.apache.commons.io.FilenameUtils
import org.grails.plugin.resource.util.HalfBakedLegacyLinkGenerator

/**
 * This taglib handles creation of all the links to resources, including the smart de-duping of them.
 *
 * This is also a general-purpose linking tag library for writing <head> links to resources. See resourceLink.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceTagLib {
    static namespace = "r"
    
    static writeAttrs( attrs, output) {
        // Output any remaining user-specified attributes
        attrs.each { k, v ->
            if (v != null) {
               output << k
               output << '="'
               output << v.encodeAsHTML()
               output << '" '    
           }
        }
    }

    static LINK_WRITERS = [
        js: { url, constants, attrs ->
            def o = new StringBuilder()
            o << "<script src=\"${url}\" "

            // Output info from the mappings
            writeAttrs(constants, o)
            writeAttrs(attrs, o)

            o << '></script>'
            return o    
        },
        
        link: { url, constants, attrs ->
            def o = new StringBuilder()
            o << "<link href=\"${url}\" "

            // Output info from the mappings
            writeAttrs(constants, o)
            writeAttrs(attrs, o)

            o << '/>'
            return o
        }
    ]

    static SUPPORTED_TYPES = [
        css:[type:"text/css", rel:'stylesheet', media:'screen, projection'],
        js:[type:'text/javascript', writer:'js'],

        gif:[rel:'shortcut icon'],
        jpg:[rel:'shortcut icon'],
        png:[rel:'shortcut icon'],
        ico:[rel:'shortcut icon'],
        appleicon:[rel:'apple-touch-icon']
    ]
    
    def resourceService
    
    def grailsLinkGenerator
    
    boolean notAlreadyIncludedResource(url) {
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
     *
     * @attr uri
     * @attr type
     */
    def doResourceLink = { attrs ->
        def uri = attrs.remove('uri')
        def type = attrs.remove('type')
        def urlForExtension = ResourceService.removeQueryParams(uri)
        if (!type) {
            type = FilenameUtils.getExtension(urlForExtension)
        }
        
        def typeInfo = SUPPORTED_TYPES[type]?.clone()
        if (!typeInfo) {
            throwTagError "I can't work out the type of ${uri} with type [${type}]. Please check the URL, resource definition or specify [type] attribute"
        }
        
        def writerName = typeInfo.remove('writer')
        def writer = LINK_WRITERS[writerName ?: 'link']

        // Allow attrs to overwrite any constants
        attrs.each { typeInfo.remove(it.key) }

        out << writer(uri, typeInfo, attrs)
    }
    
    /**
     * Render an appropriate resource link for a resource - WHETHER IT IS PROCESSED BY THIS PLUGIN OR NOT.
     *
     * IMPORTANT: The point is that devs can use this for smart links in <head> whether or not they are using the resource
     * processing mechanisms. This gives utility to all, and allows us to have a single tag in Grails, meaning
     * that users need make no changes when they move to install processing plugins like zipped-resources.
     *
     * This accepts a "url" attribute which is a Map like that passed to g.resource,
     * or "uri" attribute which is an app-relative uri e.g. 'js/main.js
     * or "plugin"/"dir"/"file" attributes like g.resource
     *
     * This is *not* just for use with declared resources, you can use it for anything e.g. feeds.
     * The "type" attribute can override the type e.g. "rss" if the type cannot be extracted from the extension of
     * the url.
     */
    def resourceLink = { attrs ->
        GrailsUtil.deprecated "Tag [r:resourceLink] is deprecated please use [r:external] instead"
        out << external(attrs)
    }

    def external = { attrs ->
        if (log.debugEnabled) {
            log.debug "external with $attrs"
        }

        def url = attrs.remove('url')
        def disposition = attrs.remove('disposition')

        def info 
        
        def resolveArgs = [:]
        def type = attrs.remove('type')
        
        if (url == null) {
            if (attrs.uri) {
                // Might be app-relative resource URI 
                resolveArgs.uri = attrs.remove('uri')
            } else {
                resolveArgs.plugin = attrs.remove('plugin')
                resolveArgs.dir = attrs.remove('dir')
                resolveArgs.file = attrs.remove('file')
            }
        } else if (url instanceof Map) {
            resolveArgs.putAll(url)
        }


        // If a disposition specificed, we may be ad hoc so use that, else rever to default for type
        if (disposition == null) {
            // Get default disposition for this type
            disposition = 'head'
        }
        resolveArgs.disposition = disposition

        info = resolveResourceAndURI(resolveArgs)

        // Copy in the tag attributes from the resource's declaration
        if (info.resource && info.resource.tagAttributes) {
            attrs.putAll(info.resource.tagAttributes)
        }
        
        // If we found a resource (i.e. not debug mode) and disposition is not what we're rendering, skip
        if (info.resource && (disposition != info.resource.disposition)) {
            // Just get out, we've called r.resource which has created the implicit resource and added it to implicit module
            // and layoutResources will render the implicit module
            return
        }
        
        // Don't do resource check if this isn't a defer/head resource
        if (!(disposition in ['defer', 'head']) || 
                notAlreadyIncludedResource(info.resource?.linkUrl ?: info.uri)) {
            attrs.type = type
            if (info.debug) {
                attrs.uri = info.resource?.linkUrl
            }
            if (!attrs.uri) {
                attrs.uri = info.uri
            }

            def wrapper = attrs.remove('wrapper') ?: info.resource?.prePostWrapper

            def output = doResourceLink(attrs).toString()

            if (wrapper) {
                out << wrapper(output)
            } else {
                out << output
            }
        }
    }
    
    def use = { attrs ->
        GrailsUtil.deprecated "Tag [r:use] is deprecated please use [r:require] instead"
        out << r.require(attrs)
    }
    
    /**
     * Indicate that a page requires a named resource module
     * This is stored in the request until layoutResources is called, we then sort out what needs rendering or not later
     */
    def require = { attrs ->
        def trk = request.resourceDependencyTracker
        if (!trk) {
            trk = [ResourceService.IMPLICIT_MODULE] // Always include this
            request.resourceDependencyTracker = trk
        }
        
        def moduleNames
        if (attrs.module) {
            moduleNames = [attrs.module]
        } else {
            if (attrs.modules instanceof List) {
                moduleNames = attrs.modules
            } else {
                moduleNames = attrs.modules.split(',')*.trim()
            }
        }
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
        // @todo rewrite this to accept disposition attr and if not present
        // do the auto toggle then.
        def trk = request.resourceDependencyTracker
        if (!request.resourceRenderedHeadResources) {
            if (log.debugEnabled) {
                log.debug "Rendering non-deferred resources..."
            }
            trk.each { module ->
                out << r.renderModule(name:module, disposition:"head")
            }
            
            def pageScripts = request['resourceRequestScripts:head']
            if (pageScripts) {
                out << "<script type=\"text/javascript\">${pageScripts}</script>"
                request['resourceRequestScripts:head'] = null // help out the GC
            }
            request.resourceRenderedHeadResources = true
        } else if (!request.resourceRenderedFooterResources) {
            if (log.debugEnabled) {
                log.debug "Rendering deferred resources..."
            }
            trk.each { module ->
                out << r.renderModule(name:module, disposition:"defer")
            }
            def pageScripts = request['resourceRequestScripts:defer']
            if (pageScripts) {
                out << "<script type=\"text/javascript\">${pageScripts}</script>"
                request['resourceRequestScripts:defer'] = null // help out the GC
            }
            request.resourceRenderedFooterResources = true
        } else {
            throw new RuntimeException('You have invoked [layoutResources] more than twice. Invoke once in head and once in footer only.')
        }
    }
    
    void storeRequestScript(text, disposition) {
        def trkName = 'resourceRequestScripts:'+disposition
        def trk = request[trkName]
        if (!trk) {
            trk = new StringBuilder() // Always include this
            request[trkName] = trk
        }
        trk << text
    }
    
    /**
     * For inline javascript that needs to be executed in the <head> section after all dependencies
     * @todo Later, we implement ESP hooks here and add scope="user" or scope="shared"
     */
    def script = { attrs, body ->
        def dispos = attrs.remove('disposition') ?: 'defer'
        storeRequestScript(body(), dispos)
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
        
        def renderingDisposition = attrs.remove('disposition')

        // Write out any dependent modules first
        if (module.dependsOn) {
            if (log.debugEnabled) {
                log.debug "Rendering the dependencies of module [${name}]"
            }
            module.dependsOn.each { modName ->
                s << r.renderModule(name:modName, disposition:renderingDisposition)
            }
        }
        
        if (log.debugEnabled) {
            log.debug "Rendering the resources of module [${name}]"
        }
        
        def debugMode = resourceService.isDebugMode(request)
        
        module.resources.each { r ->
            if (!r.exists() && !r.actualUrl?.contains('://')) {
                throw new IllegalArgumentException("Module [$name] depends on resource [${r.sourceUrl}] but the file cannot be found")
            }
            if (log.debugEnabled) {
                log.debug "Resource: ${r.sourceUrl} - disposition ${r.disposition} - rendering disposition ${renderingDisposition}"
            }
            if (r.disposition == renderingDisposition) {
                def args = [:]
                // args.uri needs to be the source uri used to identify the resource locally
                args.uri = debugMode ? r.originalUrl : "${r.actualUrl}"
                args.wrapper = r.prePostWrapper
                args.disposition = r.disposition
                
                if (r.tagAttributes) {
                    args.putAll(r.tagAttributes) // Copy the attrs originally provided e.g. type override
                }
                
                if (log.debugEnabled) {
                    log.debug "Rendering one of the module's resource links: ${args}"
                }
                s << external(args)
                s << '\n'
            }
        }
        out << s
    }

    /**
     * Get the uri to use for linking, and - if relevant - the resource instance
     * @return Map with uri/url property and *maybe* a resource property
     */
    def resolveResourceAndURI(attrs) {
        if (log.debugEnabled) {
            log.debug "resolveResourceAndURI: ${attrs}"
        }
        def ctxPath = request.contextPath
        def uri = attrs.remove('uri')
        def abs = uri?.indexOf('://') >= 0
        if (!uri || !abs) {
            if (uri) {
                uri = ctxPath + uri
            } else {
                // use the link generator to avoid stack overflow calling back into us
                // via g.resource
                attrs.contextPath = ctxPath
                uri = grailsLinkGenerator.resource(attrs)
            }
        }
        
        def debugMode = resourceService.isDebugMode(request)

        // Get out quick and add param to tell filter we don't want any fancy stuff
        if (debugMode) {
            
            // Some JS libraries can't handle different query params being sent to other dependencies
            // so we reuse the same timestamp for the lifecycle of the request
    
            // Here we allow a refresh arg that will generate a new timestamp, normally we used the last we 
            // generated. Otherwise, you can't debug anything in a JS debugger as the URI of the JS 
            // is different every time.
            if (params._refreshResources && !request.'grails-resources.debug-timestamp-refreshed') {
                // Force re-generation of a new timestamp in debug mode
                session.removeAttribute('grails-resources.debug-timestamp')
                request.'grails-resources.debug-timestamp-refreshed' = true
            }
            
            def timestamp = session['grails-resources.debug-timestamp']
            if (!timestamp) {
                timestamp = System.currentTimeMillis()
                session['grails-resources.debug-timestamp'] = timestamp
            }

            uri += (uri.indexOf('?') >= 0) ? "&_debugResources=y&n=$timestamp" : "?_debugResources=y&n=$timestamp"
            return [uri:uri, debug:true]
        } 
        
        def disposition = attrs.remove('disposition')

        // Chop off context path
        def reluri = ResourceService.removeQueryParams(abs ? uri : uri[ctxPath.size()..-1])
        
        // Get or create ResourceMeta
        def res = resourceService.getResourceMetaForURI(reluri, true, null, { res ->
            // If this is an ad hoc resource, we need to store if it can be deferred or not
            if (disposition != null) {
                res.disposition = disposition
            }
        })
        
        // We need to handle a) absolute links here for CDN, and b) base url
        def linkUrl = res ? res.linkUrl : uri
        def baseUrl = '' // @todo get from config
        if (linkUrl.contains('://') || baseUrl) {
            // @todo do we need to toggle http/https here based on current request protocol?
            return [uri:baseUrl ? baseUrl+linkUrl : linkUrl, resource:res]
        } else {
            uri = ctxPath+resourceService.staticUrlPrefix+linkUrl
            return [uri:uri, resource:res]
        }
    }
     
    /**
     * Get the URL for a resource
     * @todo this currently won't work for absolute="true" invocations, it should just passthrough these
     */
    def resource = { attrs ->
        def info = resolveResourceAndURI(attrs)
        if (info.resource) {
            // We know we located the resource
            out << info.uri
        } else {
            // We don't know about this, back out and use grails URI but warn
            if (!info.debug && log.warnEnabled) {
                log.warn "Invocation of <r:resource> for a resource that apparently doesn't exist: ${info.uri}"
            }
            out << info.uri
        }
    }
    
    /**
     * Write out an HTML <img> tag using resource processing for the image
     */
    def img = { attrs ->
        def args = attrs.clone()
        args.disposition = "image"
        if (!attrs.uri && !attrs.dir) {
            attrs.dir = "images"
        }
        def info = resolveResourceAndURI(args)
        def res = info.resource

        attrs.remove('uri')
        def o = new StringBuilder()
        o << "<img src=\"${info.uri.encodeAsHTML()}\" "
        if (res) {
            def attribs = res.tagAttributes ? res.tagAttributes.clone() : [:]
    		def excludes = ['dir', 'uri', 'file', 'plugin']
            attribs += attrs.findAll { !(it.key in excludes) }
            attrs = attribs
        }
        writeAttrs(attrs, o)
        o << "/>"
        out << o
    }
}
