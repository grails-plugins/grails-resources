package org.grails.plugin.resource

import org.apache.commons.logging.LogFactory

class CSSRewriter {
    
    static log = LogFactory.getLog('org.grails.plugin.resource.CSSRewriter')

    static CSS_URL_PATTERN = ~/(url\s*\(['"]?\s*)(.+?)(\s*['"]?\s*\))/
    
    static isCSSRewriteCandidate(resource, resourceService) {
        def enabled = resourceService.config.rewrite.css instanceof Boolean ? resourceService.config.rewrite.css : true
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
    static mapper = { resource, resourceService ->
        
        if (!isCSSRewriteCandidate(resource, resourceService)) {
            println "Its not rewritable"
            return
        }
        
        // Move existing to tmp file, then write to the correct file
        def origFile = new File(resource.processedFile.toString()+'.tmp')
        origFile.delete()
        resource.processedFile.renameTo(origFile)
        def rewrittenFile = resource.processedFile
        if (log.debugEnabled) {
            log.debug "Pre-processing CSS resource ${resource.sourceUrl} to rewrite links"
        }

       // Replace all urls to resources we know about to their processed urls
       rewrittenFile.withPrintWriter("utf-8") { writer ->
           origFile.eachLine { line ->
               def fixedLine = line.replaceAll(CSS_URL_PATTERN) { Object[] args ->
                   def prefix = args[1]
                   def originalUrl = args[2].trim()
                   def suffix = args[3]

                   // We don't do absolutes or full URLs - perhaps we should do "/" at some point? If app 
                   // is mapped to root context then some people might do this but its lame
                   if (originalUrl.startsWith('/') || (originalUrl.indexOf('://') > 0)) {
                       return "${prefix}${originalUrl}${suffix}"
                   }

                   def uri = URLUtils.resolveURI(resource.sourceUrl, originalUrl)
                   if (log.debugEnabled) {
                       log.debug "Calculated URI of CSS resource [$originalUrl] as [$uri]"
                   }

                   try {
                       // This triggers the processing chain if necessary for any resource referenced by the CSS
                       def linkedToResource = resourceService.getResourceMetaForURI(uri, false) { res ->
                           // If there's no decl for the resource, create it with image disposition
                           // otherwise we pop out as a favicon...
                           res.disposition = 'image'
                       }

                       if (log.debugEnabled) {
                           log.debug "Calculating URL of ${linkedToResource.dump()} relative to ${resource.dump()}"
                       }

                       def fixedUrl = linkedToResource.relativeTo(resource)
                       def replacement = "${prefix}${fixedUrl}${suffix}"

                       if (log.debugEnabled) {
                           log.debug "Rewriting CSS URL '${args[0]}' to '$replacement'"
                       }

                       return replacement
                   } catch (IllegalArgumentException e) {
                       // @todo We don't want to do this really... or do we? New exception type better probably
                       log.warn "Cannot resolve CSS resource, leaving link as is: ${originalUrl}"
                       return "${prefix}${originalUrl}${suffix}"
                   }
               }
               writer.println(fixedLine)
            }
        }  
        origFile.delete()      
    }
}