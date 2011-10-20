package org.grails.plugin.resource

import org.apache.commons.logging.LogFactory

import org.apache.commons.io.FilenameUtils

/**
 * Holder for info about a resource that is made up of other resources
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class AggregatedResourceMeta extends ResourceMeta {

    def log = LogFactory.getLog(this.class)

    def resources = []
    def inheritedModuleDependencies = new HashSet()

    void add(ResourceMeta r, Closure postProcessor = null) {
        resources << r
        inheritedModuleDependencies << r.module
        
        // Update our aggregated sourceUrl
        sourceUrl = "${sourceUrl}, ${r.sourceUrl}"
        
        r.delegateTo(this)
        if (postProcessor) {
            postProcessor(this)
        }
    }

    Writer getWriter() {
        processedFile.newWriter('UTF-8', true)
    }

    @Override
    void beginPrepare(grailsResourceProcessor) {
        buildAggregateResource(grailsResourceProcessor)
    }

    void buildAggregateResource(grailsResourceProcessor) {
        def out = getWriter()
        
        // @todo I'm not sure we really want this here?
        grailsResourceProcessor.updateDependencyOrder()
        def moduleOrder = grailsResourceProcessor.modulesInDependencyOrder

        def newestLastMod = 0
        
        // Add the resources to the file in the order determined by module dependencies!
        moduleOrder.each { m ->
            resources.each { r ->
                if (r.module.name == m) {
                    // Append to the existing file
                    if (log.debugEnabled) {
                        log.debug "Appending contents of ${r.processedFile} to ${processedFile}"
                    }
                    out << r.processedFile.getText("UTF-8")
                    out << "\r\n"
                    
                    if (r.originalLastMod > newestLastMod) {
                        newestLastMod = r.originalLastMod
                    }
                }
            }
        }
        
        out << "\r\n"
        out.close()
        
        this.originalLastMod = newestLastMod
    }
}