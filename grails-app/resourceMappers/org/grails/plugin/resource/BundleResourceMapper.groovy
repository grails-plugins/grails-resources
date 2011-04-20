package org.grails.plugin.resource

import org.grails.plugin.resource.mapper.MapperPhase

/**
 * This mapper creates synthetic AggregatedResourceMeta instances for any bundle
 * names found in the resource declarations, and gathers up info about those resources
 * so that when the bundle itself is requested, the aggregated file is created and returned.
 * 
 * This sets any ResourceMeta to which this mapper applies, to be "delegating" to the new aggregated resource
 * so when those resources are rendered/requested, the bundle URI is written out.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class BundleResourceMapper {
    
//    def priority = 500
    
    def phase = MapperPhase.AGGREGATION
    
    def resourceService
    
    /**
     * Find resources that belong in bundles, and create the bundles, and make the resource delegate to the bundle
     * Create a new aggregated resource for the bundle and shove all the resourceMetas into it.
     * We rely on the smart linking stuff to avoid writing out the same bundle multiple times, so you still have
     * dependencies to the individual resources but these delegate to the aggregated resource, and hence all such
     * resources will return the same link url, and not be included more than once.
     *
     * I'm not sure this is the best way, but it means we don't need to mess with existing module and resource definitions
     * which would undoubtedly cause us all kinds of pain.
     */
    def map(resource, config) {
        def bundleId = resource.bundle
        if (bundleId) {
            // Find/create bundle for this extension type
            def bundle = "/bundle-$bundleId.${resource.sourceUrlExtension}"
            
            def bundleResource = resourceService.findSyntheticResourceForURI(bundle)
            if (!bundleResource) {
                // Creates a new resource and empty file
                bundleResource = resourceService.newSyntheticResource(bundle, AggregatedResourceMeta)
                bundleResource.contentType = resource.contentType
                bundleResource.processedFile.createNewFile()
            }

            // After we update this, the resource's linkUrl will return the url of the bundle
            bundleResource.add(resource)
        }
    }
}
