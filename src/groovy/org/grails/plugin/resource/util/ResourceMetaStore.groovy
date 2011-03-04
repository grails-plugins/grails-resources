package org.grails.plugin.resource.util

import org.apache.commons.logging.LogFactory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

import org.grails.plugin.resource.ResourceMeta

/**
 * A special URI -> ResourceMeta store that is non-reentrant and will create
 * entries on demand, causing other threads to wait until the resource has been created
 * if creation has already started
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class ResourceMetaStore {
    def log = LogFactory.getLog(this.class)

    Map latches = new ConcurrentHashMap()
    Map resourcesByURI = new ConcurrentHashMap()
    
    static CLOSED_LATCH = new CountDownLatch(0)
    
    /**
     * Note that this is not re-entrant save, and is only to be called at app startup, before requests come in
     */
    void addDeclaredResource(Closure resourceCreator) {
        def resource = resourceCreator()
        if (log.debugEnabled) {
            log.debug "Adding declared resource ${resource}"
        }
        
        // It may be null if it is not found / broken in some way
        if (resource) {
            addResource(resource, false)
        }
    }

    private addResource(resource, boolean adHocResource = false) {
        def uris = []

        // Add the actual linking URL to the cache so resourceLink resolves
        // ONLY if its not delegating, or we get a bunch of crap in here / hide the delegated resource
        if (!resource.delegating) {
            if (log.debugEnabled) {
                log.debug "Updating URI to resource cache for ${resource}"
            }
            uris << resource.actualUrl
        }

        // Add the original source url to the cache as well, if it was an ad-hoc resource
        // As the original URL is used, we need this to resolve to the actualUrl for redirect
        if (adHocResource || resource.delegating) {
            uris << resource.sourceUrl
            resource = resource.delegating ? resource.delegate : resource
        }
        
        uris.each { u ->
            if (log.debugEnabled) {
                log.debug "Storing mapping for resource URI $u to ${resource}"
            }
            resourcesByURI[u] = resource
            latches[u] = CLOSED_LATCH // so that future calls for alternative URLs succeed
        }
    }
    
    /** 
     * A threadsafe synchronous method to get an existing resource or create an ad-hoc resource
     */
    ResourceMeta getOrCreateAdHocResource(String uri, Closure resourceCreator) {
        def latch = latches.get(uri)

        if (latch == null) {
            def thisLatch = new CountDownLatch(1)
            def otherLatch = latches.putIfAbsent(uri, thisLatch)
            if (otherLatch == null) {
                // process resource
                def resource
                try {
                    resource = resourceCreator()
                    if (log.debugEnabled) {
                        log.debug "Creating resource for URI $uri returned ${resource}"
                    }
                } catch (Throwable t) {
                    latches[uri] = CLOSED_LATCH // so that future calls for broken (not found) resources don't block forever
                    throw t
                }

                // It may be null if it is not found / broken in some way
                if (resource) {
                    addResource(resource, true)
                }
                
                // indicate that we are done
                thisLatch.countDown()
                return resource                
            } else {
                otherLatch.await()
                return resourcesByURI[uri]
            }
        } else {
            latch.await()
            return resourcesByURI[uri]
        }
    }
    
    def keySet() {
        resourcesByURI.keySet()
    }
    
    def getAt(String key) {
        resourcesByURI[key]
    }
}