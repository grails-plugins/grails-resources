package org.grails.plugin.resource

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
        if (log.debugEnabled) {
            log.debug "Checking if this request has already pulled in [$url]"
        }
        def trk = request.resourceTracker
        if (!trk) {
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
	  	def url = attrs.remove('url')
	  	if (url == null) {
	  	    if (attrs.uri) {
	  	        // Might be app-relative URI
	  	        url = g.createLink(uri:attrs.remove('uri'))
	  	    } else {
        	    url = r.resource(plugin:attrs.remove('plugin'), dir:attrs.remove('dir'), file:attrs.remove('file')).toString()
    	    }
    	} else if (url instanceof Map) {
    	    url = g.resource(url).toString()
    	}
    	
    	if (usingResource(url)) {
        	def t = attrs.remove('type')
            if (!t) {
                def ext = url[url.lastIndexOf('.')+1..-1]
                t = LINK_EXTENSIONS_TO_TYPES[ext]
                if (!t) {
                    t = ext
                }
            }
            if (log.debugEnabled) {
                log.debug "Resource [${url}] has type [$t]"
            }
            
            def typeInfo = [:] + LINK_RESOURCE_MAPPINGS[t]
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
        if (usingModule(name)) {
            def module = resourceService.getModule(name)
            if (!module) {
                throw new IllegalArgumentException("No module found with name [$name]")
            }
            def s = new StringBuilder()
            // Write out any dependent modules first
            if (module.dependsOn) {
                module.dependsOn.each { modName ->
                    s << r.module(name:modName)
                }
            }
            module.resources.each { r ->
                def args = r.attributes.clone()
                args.uri = r.actualUrl
                args.wrapper = r.prePostWrapper
                s << resourceLink(args)
                s << '\n'
            }
            out << s
        }
    }

    def resource = { attrs ->
        // Resolve against modified resource names
        out << g.resource(attrs)
    }
}
