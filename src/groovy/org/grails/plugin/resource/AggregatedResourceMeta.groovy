package org.grails.plugin.resource

import org.apache.commons.io.FilenameUtils

/**
 * Holder for info about a resource that is made up of other resources
 */
class AggregatedResourceMeta extends ResourceMeta {

    def resources = []
    
    void add(ResourceMeta r, Closure postProcessor = null) {
        resources << r
        
        // Update our aggregated sourceUrl
        sourceUrl = "${sourceUrl}, ${r.sourceUrl}"
        
        r.delegateTo(this)
        postProcessor(this)
    }
}