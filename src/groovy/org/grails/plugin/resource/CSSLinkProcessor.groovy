package org.grails.plugin.resource

import org.apache.commons.logging.LogFactory

/**
 * This class is used to parse out and replace CSS links
 * 
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class CSSLinkProcessor {
    
    def log = LogFactory.getLog(CSSLinkProcessor)
    
    // We need to successfully match any kind of url(), mappers are responsible for checking type
    static CSS_URL_PATTERN = ~/(url\s*\(['"]?\s*['"]?)(.+?)(\s*['"]?\s*['"]?\))/
    
    boolean isCSSRewriteCandidate(resource, grailsResourceProcessor) {
        def enabled = grailsResourceProcessor.config.rewrite.css instanceof Boolean ? grailsResourceProcessor.config.rewrite.css : true
        def yes = enabled && (resource.contentType == "text/css" || resource.tagAttributes?.type == "css")
        if (log.debugEnabled) {
            log.debug "Resource ${resource.actualUrl} being CSS rewritten? $yes"
        }
        return yes
    }
    
    /**
     * Find all url() and fix up the url if it is not absolute
     * NOTE: This needs to run after any plugins that move resources around, but before any that obliterate
     * the content i.e. before minify or gzip
     */
    void process(ResourceMeta resource, grailsResourceProcessor, Closure urlMapper) {
        
        if (!isCSSRewriteCandidate(resource, grailsResourceProcessor)) {
            if (log.debugEnabled) {
                log.debug "CSS link processor skipping ${resource} because its not a CSS rewrite candidate"
            }
            return
        }
        
        // Move existing to tmp file, then write to the correct file
        def origFile = new File(resource.processedFile.toString()+'.tmp')
        
        // Make sure it doesn't exist already
        new File(origFile.toString()).delete() // On MS Windows if we don't do this origFile gets corrupt after delete
        
        resource.processedFile.renameTo(origFile)
        def rewrittenFile = resource.processedFile
        if (log.debugEnabled) {
            log.debug "Pre-processing CSS resource ${resource.sourceUrl} to rewrite links"
        }

        // Replace all urls to resources we know about to their processed urls
        rewrittenFile.withPrintWriter("utf-8") { writer ->
           origFile.eachLine('UTF-8') { line ->
               def fixedLine = line.replaceAll(CSS_URL_PATTERN) { Object[] args ->
                   def prefix = args[1]
                   def originalUrl = args[2].trim()
                   def suffix = args[3]

                   return urlMapper(prefix, originalUrl, suffix)
               }
               writer.println(fixedLine)
            }
        }  
        
        // Delete the temp file
        origFile.delete()      
    }
}