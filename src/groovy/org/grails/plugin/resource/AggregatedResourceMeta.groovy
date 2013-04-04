package org.grails.plugin.resource

/**
 * Holder for info about a resource that is made up of other resources
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class AggregatedResourceMeta extends ResourceMeta {

    List<ResourceMeta> resources = []
    Set<ResourceModule> inheritedModuleDependencies = []

    boolean containsResource(ResourceMeta r) {
        resources.find { r.sourceUrl == it.sourceUrl }
    }

    @Override
    boolean isDirty() {
        resources.any { it.dirty }
    }

    void add(ResourceMeta r, Closure postProcessor = null) {
        r.delegateTo(this)

        if (!containsResource(r)) {
            resources << r
            inheritedModuleDependencies << r.module

            // Update our aggregated sourceUrl
            sourceUrl = "${sourceUrl}, ${r.sourceUrl}"
        }

        if (postProcessor) {
            postProcessor(this)
        }
    }

    Writer getWriter() {
        processedFile.newWriter('UTF-8', true)
    }

    protected void initFile(grailsResourceProcessor) {
        int commaPos = sourceUrl.indexOf(',')
        if (commaPos == -1) {
            commaPos = sourceUrl.size()
        }
        actualUrl = commaPos ? sourceUrl[0..commaPos-1] : sourceUrl

        processedFile = grailsResourceProcessor.makeFileForURI(actualUrl)
        processedFile.createNewFile()

        contentType = grailsResourceProcessor.getMimeType(actualUrl)
    }

    @Override
    void beginPrepare(grailsResourceProcessor) {
        initFile(grailsResourceProcessor)

        originalSize = resources.originalSize.sum()

        buildAggregateResource(grailsResourceProcessor)
    }

    void buildAggregateResource(grailsResourceProcessor) {
        List<String> moduleOrder = grailsResourceProcessor.modulesInDependencyOrder

        long newestLastMod = 0

        def bundledContent = new StringBuilder()

        // Add the resources to the file in the order determined by module dependencies!
        for (String m in moduleOrder) {
            for (ResourceMeta r in resources) {
                if (r.module.name == m) {
                    // Append to the existing file
                    if (log.debugEnabled) {
                        log.debug "Appending contents of $r.processedFile to $processedFile"
                    }
                    bundledContent << r.processedFile.getText("UTF-8")
                    bundledContent << "\r\n"

                    if (r.originalLastMod > newestLastMod) {
                        newestLastMod = r.originalLastMod
                    }
                }
            }
        }

        def out = getWriter()
        out << bundledContent
        out << "\r\n"
        out.close()

        originalLastMod = newestLastMod
    }
}
