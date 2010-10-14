package org.grails.plugin.resource

class CSSPreprocessorResourceMapper {

    def priority = Integer.MAX_VALUE
    
    def resourceService
    
    /**
     * Find all url() and fix up the url if it is not absolute
     * NOTE: This needs to run after any plugins that move resources around, but before any that obliterate
     * the content i.e. before minify or gzip
     */
    def map(resource, config) {
        def processor = new CSSLinkProcessor()
        
        processor.process(resource, resourceService) { prefix, originalUrl, suffix ->
            
            // We don't do absolutes or full URLs - perhaps we should do "/" at some point? If app 
            // is mapped to root context then some people might do this but its lame
            if (originalUrl.startsWith('/') || (originalUrl.indexOf('://') > 0)) {
                return "${prefix}${originalUrl}${suffix}"
            }

            def uri
            try {
                uri = 'resource:'+URLUtils.relativeURI(resource.sourceUrl, originalUrl)
            } catch (URISyntaxException sex) {
                if (log.warnEnabled) {
                    log.warn "Cannot resolve CSS resource, leaving link as is: ${originalUrl}"
                }
            }

            if (uri) {
                if (uri.indexOf('/../') >= 0) {
                    uri = originalUrl // Fall back to original, its above processed root
                }
                if (log.debugEnabled) {
                    log.debug "Calculated absoluted URI of CSS resource [$originalUrl] as [$uri]"
                }
                return "${prefix}${uri}${suffix}"
            } else {
                return "${prefix}${originalUrl}${suffix}"
            }

        }
    }
}