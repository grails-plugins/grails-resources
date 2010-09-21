package org.grails.plugin.resource

import grails.util.Environment

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
    
    boolean usingModule(name) {
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
    
    def resourceLink = { attrs ->
        if (log.debugEnabled) {
            log.debug "resourceLink with $attrs"
        }
	  	def url = attrs.remove('url')
	  	if (url == null) {
	  	    if (attrs.uri) {
	  	        // Might be app-relative resource URI 
//	  	        url = g.createLink(uri:resourceService.staticUrlPrefix+attrs.remove('uri'))
	  	        url = r.resource(uri:attrs.remove('uri'))
	  	    } else {
        	    url = r.resource(plugin:attrs.remove('plugin'), dir:attrs.remove('dir'), file:attrs.remove('file')).toString()
    	    }
    	} else if (url instanceof Map) {
    	    url = r.resource(url).toString()
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
            def output = writer(url, typeInfo, attrs)
            if (wrapper) {
                out << wrapper(output)
            } else {
                out << output
            }
        }
    }
    
    def module = { attrs ->
        def name = attrs.name
        if (log.debugEnabled) {
            log.debug "Checking if module [${name}] is already loaded..."
        }
        if (usingModule(name)) {
            if (log.debugEnabled) {
                log.debug "Getting info for module [${name}]"
            }
            def module = resourceService.getModule(name)
            if (!module) {
                throw new IllegalArgumentException("No module found with name [$name]")
            }
            def s = new StringBuilder()
            // Write out any dependent modules first
            if (module.dependsOn) {
                if (log.debugEnabled) {
                    log.debug "Rendering the dependencies of module [${name}]"
                }
                module.dependsOn.each { modName ->
                    s << r.module(name:modName)
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
                def args = r.tagAttributes?.clone() ?: [:]
                args.uri = debugMode ? r.sourceUrl : r.actualUrl
                args.wrapper = r.prePostWrapper
                if (log.debugEnabled) {
                    log.debug "Rendering one of the module's resource links: ${args}"
                }
                s << resourceLink(args)
                s << '\n'
            }
            out << s
        }
    }

    /**
     * Get the URL for a resource
     * @todo this currently won't for for absolute="true" invocations
     */
    def resource = { attrs ->
        def ctxPath = request.contextPath
        def uri = attrs.uri ? ctxPath+attrs.uri : g.resource(attrs).toString()
        def debugMode = (Environment.current == Environment.DEVELOPMENT) && params.debugResources

        // Get out quick and add param to tell filter we don't want any fancy stuff
        if (debugMode) {
            out << uri+"?debug=y"
            return
        }
        
        // Chop off context path
        def reluri = uri[ctxPath.size()..-1]
        def res = resourceService.getResourceMetaForURI(reluri)
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
