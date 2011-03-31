package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.util.ResourceMetaStore

class ResourceMetaStoreTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testAddingDeclaredResourceAddsBothProcessedAndSourceUrls() {
        def r = new ResourceMeta()
        r.sourceUrl = "/jquery/images/bg.png"
        r.workDir = new File('/tmp/test')
        r.processedFile = new File('/tmp/test/123456789.png')
        r.updateActualUrlFromProcessedFile()
 
        def store = new ResourceMetaStore()
        store.addDeclaredResource( { 
            r.actualUrl = "/jquery/images/_bg.png"
            return r
        } )
        
        assertEquals ResourceMetaStore.CLOSED_LATCH, store.latches["/jquery/images/bg.png"]
        assertEquals ResourceMetaStore.CLOSED_LATCH, store.latches["/jquery/images/_bg.png"]
    }
}