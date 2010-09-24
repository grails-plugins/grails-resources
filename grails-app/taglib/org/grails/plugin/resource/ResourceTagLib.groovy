package org.grails.plugin.resource

import grails.util.Environment
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.apache.commons.io.FilenameUtils

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

    static SUPPORTED_TYPES = [
        css:[type:"text/css", rel:'stylesheet'],
        js:[type:'text/javascript', writer:'js'],

        gif:[type:'image/x-icon', rel:'shortcut icon'],
        jpg:[type:'image/x-icon', rel:'shortcut icon'],
        png:[type:'image/x-icon', rel:'shortcut icon'],
        ico:[type:'image/x-icon', rel:'shortcut icon'],
        appleicon:[rel:'apple-touch-icon'],
        /*
        Be nice to do this later but we need dynamic resource impl'd first
        rss:[type:'application/rss+xml', rel:'alternate'], 
        atom:[type:'application/atom+xml', rel:'alternate'], 
        */
    ]
    
    def resourceService
    
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
        if (log.debugEnabled) {
            log.debug "resourceLink with $attrs"
        }

        def url = attrs.remove('url')
        def disposition = attrs.remove('disposition')

        def info 
        
        def resolveArgs = [:]
        def type = attrs.remove('type')
        def urlForExtension
        
        if (url == null) {
            if (attrs.uri) {
                // Might be app-relative resource URI 
                resolveArgs.uri = attrs.remove('uri')
                urlForExtension = ResourceService.removeQueryParams(resolveArgs.uri)
            } else {
                resolveArgs.plugin = attrs.remove('plugin')
                resolveArgs.dir = attrs.remove('dir')
                resolveArgs.file = attrs.remove('file')
                urlForExtension = resolveArgs.file
            }
        } else if (url instanceof Map) {
            resolveArgs.putAll(url)
            urlForExtension = url.file
        }

        // Work out type from extension if not specified as an arg
        if (!type) {
            type = FilenameUtils.getExtension(urlForExtension)
        }
        
        println "Type is ${type}"
        def typeInfo = SUPPORTED_TYPES[type]?.clone()  // must clone, we mutate this
        if (!typeInfo) {
            throwTagError "I can't work out the type of ${urlForExtension}. Please check the URL or specify [type] attribute"
        }

        // If a disposition specificed, we may be ad hoc so use that, else rever to default for type
        if (disposition == null) {
            // Get default disposition for this type
            disposition = 'head'
        }
        resolveArgs.disposition = disposition

        info = resolveResourceAndURI(resolveArgs)
        
        if (disposition != info.resource.disposition) {
            // Just get out, we've called r.resource which has created the implicit resource and added it to implicit module
            // and layoutResources will render the implicit module
            return
        }
        
        // Don't do resource check if this isn't a defer/head resource
        if (!(disposition in ['defer', 'head']) || notAlreadyIncludedResource(info.uri)) {
            def writerName = typeInfo.remove('writer')
            def writer = LINK_WRITERS[writerName ?: 'link']
            def wrapper = attrs.remove('wrapper')

            // Allow attrs to overwrite any constants
            attrs.each { typeInfo.remove(it.key) }

            def output = writer(info.uri, typeInfo, attrs)
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
                out << r.renderModule(name:module, disposition:"head")
            }
            request.resourceRenderedHeadResources = true
        } else if (!request.resourceRenderedFooterResources) {
            if (log.debugEnabled) {
                log.debug "Rendering deferred resources..."
            }
            trk.each { module ->
                out << r.renderModule(name:module, disposition:"defer")
            }
            request.resourceRenderedFooterResources = true
        } else {
            throw new RuntimeException('You have invoked [layoutResources] more than twice. Invoke once in head and once in footer only.')
        }
    }
    
    /**
     * For inline javascript that needs to be executed in the <head> section after all dependencies
     */
    def initScript = { attrs, body ->
    }
    
    /**
     * For inline javascript that needs to be either deferred to the end of the page or put into "E.S.P." (in future)
     */
    def pageScript = { attrs, body ->
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
        
        def debugMode = (Environment.current == Environment.DEVELOPMENT) && params.debugResources
        
        module.resources.each { r ->
            if (!r.exists()) {
                throw new IllegalArgumentException("Module [$name] depends on resource [${r.sourceUrl}] but the file cannot be found")
            }
            if (log.debugEnabled) {
                log.debug "Resource: ${r.sourceUrl} - disposition ${r.disposition} - rendering disposition ${renderingDisposition}"
            }
            if (r.disposition == renderingDisposition) {
                def args = r.tagAttributes?.clone() ?: [:]
                args.uri = debugMode ? r.sourceUrl : "${r.linkUrl}"
                args.wrapper = r.prePostWrapper
                args.disposition = r.disposition
                if (log.debugEnabled) {
                    log.debug "Rendering one of the module's resource links: ${args}"
                }
                s << resourceLink(args)
                s << '\n'
            }
        }
        out << s
    }

    /**
     * Get the uri to use for linking, and - if relevant - the resource instance
     * @return Map with uri property and *maybe* a resource property
     */
    def resolveResourceAndURI(attrs) {
        if (log.debugEnabled) {
            log.debug "resolveResourceAndURI: ${attrs}"
        }
        def ctxPath = request.contextPath
        def uri = attrs.remove('uri')
        uri = uri ? ctxPath+uri : g.resource(attrs).toString()
        def debugMode = (Environment.current == Environment.DEVELOPMENT) && params.debugResources

        // Get out quick and add param to tell filter we don't want any fancy stuff
        if (debugMode) {
            return [uri:uri+"?debug=y", debug:true]
        } 
        
        def disposition = attrs.remove('disposition')

        // Chop off context path
        def reluri = uri[ctxPath.size()..-1]
        
        // Get or create ResourceMeta
        def res = resourceService.getResourceMetaForURI(reluri, true, { res ->
            // If this is an ad hoc resource, we need to store if it can be deferred or not
            if (disposition != null) {
                res.disposition = disposition
            }
        })
        
        uri = ctxPath+resourceService.staticUrlPrefix+res.linkUrl
        return [uri:uri, resource:res]
    }
     
    /**
     * Get the URL for an ad-hoc resource - NOT for declared resources
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
                log.warn "Invocation of <r:resource> for a resource that apparently doesn't exist: $uri"
            }
            out << uri
        }
    }
    
    /**
     * Write out an HTML <img> tag using resource processing for the image
     */
    def img = { attrs ->
        attrs.disposition = "image"
        def info = resolveResourceAndURI(attrs)
        def res = info.resource

        def o = new StringBuilder()
        o << "<img src=\"${info.uri.encodeAsHTML()}\" "
        if (res) {
            def attribs = res.tagAttributes.clone()
            attribs.putAll(attrs)
            attrs = attribs
        }
        writeAttrs(attrs, o)
        o << "/>"
        out << o
    }
}
