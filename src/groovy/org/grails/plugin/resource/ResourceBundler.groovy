package org.grails.plugin.resource

import org.apache.commons.logging.LogFactory

/**
 *
 * Create bundle if doesn't exist
 * Append self to bundle if does exist
 * Change processed file to point at bundle
 * Make sure resources does not apply zip etc multiple times to the bundled file
 * Clear the bundle file at startup (how?)
 *
 * Perhaps just incorporate into resources core for now, during first copy and before applying mappers?
 
 *
 
 The tricky problem of changing the base url of possibly deeply nested CSS files and their relative links.
 
 Example input CSS at "/js/library/main.css":
 
 body { background: url(../images/bg.png) }
 
 Example in bundled file "/bundle-app.css" - BREAKS IMAGE LINK:

 body { background: url(../images/bg.png) }

 Example in bundled file after flattening of all files and renaming to "/9349484849494.css":

 body { background: url(8977867868s7a6d7sdsad6786asd.png) }



 Example input CSS at "/js/library/main.css":
 
 body { background: url(../images/bg.png) }
 
 >>>>>> after CSSabsoluting:

 body { background: url(/js/library/images/bg.png) }

 >>>>> after bundling - file /bundle-app.css:

 body { background: url(/js/library/images/bg.png) }

 >>>>> after caching/hashing file /786876786868.css:

 body { background: url(/js/library/images/bg.png) }

 >>>>> after final CSSRewrite bundled file /786876786868.css:

 body { background: url(889798798798ddfjks.png) }

 */
class ResourceBundler {
    
    static log = LogFactory.getLog('org.grails.plugin.resource.ResourceBundler')

    static BUNDLE_DIR = "bundles"
    
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
    static mapper = { resource, resourceService ->
        def bundleId = resource.attributes.bundle
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
            bundleResource.add(resource) { b ->
                // Append to the existing file
                if (log.debugEnabled) {
                    log.debug "Appending contents of ${resource.processedFile} to ${b.processedFile}"
                }
                // @todo would be nice to add a comment here indicating name of file pulled in
                // BUT we don't really want to be content type specific...
                // The sourceUrl shows the files.
                b.processedFile.append(resource.processedFile.newInputStream())
                b.processedFile.append("\n")
                // @todo do we delete the original now?
            }
        }
    }
}
