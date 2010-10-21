package org.grails.plugin.resource

import org.apache.commons.logging.LogFactory

import org.apache.commons.io.FilenameUtils

/**
 * Holder for info about a resource that is made up of other resources
 */
class AggregatedResourceMeta extends ResourceMeta {

    def log = LogFactory.getLog(this.class)

    def resources = []

    static DISPOSITION_PRIORITIES = [ 'head', 'defer']
    
    void add(ResourceMeta r, Closure postProcessor = null) {
        resources << r
        
        // Update our aggregated sourceUrl
        sourceUrl = "${sourceUrl}, ${r.sourceUrl}"
        
        r.delegateTo(this)
        if (postProcessor) {
            postProcessor(this)
        }
    }

    @Override
    void beginPrepare(resourceService) {
        def writer = processedFile.newWriter('UTF-8')
        if (contentType == 'text/css') {
            writer << '@charset "UTF-8";\n'
        }
        
        resourceService.updateDependencyOrder()
        def moduleOrder = resourceService.modulesInDependencyOrder
    
        def disposIdx = DISPOSITION_PRIORITIES.indexOf(disposition)
        if (disposIdx == -1) {
            disposIdx = Integer.MAX_VALUE
        }
        
        // Add the resources to the file in the order determined by module dependencies!
        moduleOrder.each { m ->
            resources.each { r ->
                if (r.module.name == m) {
                    // Append to the existing file
                    if (log.debugEnabled) {
                        log.debug "Appending contents of ${r.processedFile} to ${processedFile}"
                    }
                    writer << r.processedFile.getText("UTF-8")
                    writer << "\r\n"
                    
                    // Copy the most appropriate disposition i.e. head trumps defer
                    def idx = DISPOSITION_PRIORITIES.indexOf(r.disposition)
                    if (idx < disposIdx) {
                        disposition = r.disposition
                        disposIdx = idx
                    }
                }
            }
        }
        
        writer << "\r\n"
        writer.close()
    }
}