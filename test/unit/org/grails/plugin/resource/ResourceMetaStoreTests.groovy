package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.util.ResourceMetaStore

@TestMixin(GrailsUnitTestMixin)
class ResourceMetaStoreTests {
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

    void testRequestingResourceThatDoesNotExist() {
        def store = new ResourceMetaStore()
        def resURI = '/images/idonotexist.jpg'
        def res = store.getOrCreateAdHocResource(resURI, { throw new FileNotFoundException('Where my file?') } )
        assertNull "Resource should not have existed", res
        
        assertNull store.latches[resURI]
    }
}