package org.grails.plugin.resource

/**
 * This mapper is the first phase of CSS rewriting.
 *
 * It will find any relative URIs in the CSS and convert them to a "resource:<originalURI-made-absolute>" 
 * so that later after mappers have been applied, the URIs can be fixed up and restored to URIs relative to the
 * new CSS output file's location. For example a bundle or "hashandcache" mapper may move the CSS file to a completely
 * different place, thus breaking all the relative links to images.
 *
 * @see CSSRewriter mapper for phase 2 of the process.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class CSSPreprocessorResourceMapper {

    def priority = 100 // This has to be very close to the beginning
    
    static defaultIncludes = ['**/*.css']

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