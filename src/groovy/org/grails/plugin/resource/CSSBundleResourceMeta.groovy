package org.grails.plugin.resource

/**
 * Holder for info about a resource that is made up of other resources
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class CSSBundleResourceMeta extends AggregatedResourceMeta {

    @Override
    void beginPrepare(ResourceProcessor grailsResourceProcessor) {
        initFile(grailsResourceProcessor)

        def out = getWriter()
        out << '@charset "UTF-8";\n'
        out.close()

        buildAggregateResource(grailsResourceProcessor)
    }
}
