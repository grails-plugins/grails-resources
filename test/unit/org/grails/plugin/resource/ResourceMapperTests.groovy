package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.mapper.ResourceMapper

class ResourceMapperTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }
    
    void testDefaultIncludesExcludes() {
        def artefact = new DummyMapper()
        artefact.defaultExcludes = ['**/*.jpg', '**/*.png']
        artefact.defaultIncludes = ['**/*.*']
        artefact.name = 'foobar'
        artefact.map = { res, config ->
        }
        
        def m = new ResourceMapper(artefact, [foobar:[:]])
        
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/images/test.png'
        testMeta.actualUrl = '/images/test.png'
        testMeta.contentType = "image/png"
        
        assertFalse m.invokeIfNotExcluded(testMeta)

        def testMetaB = new ResourceMeta()
        testMetaB.sourceUrl = '/images/test.jpg'
        testMetaB.actualUrl = '/images/test.jpg'
        testMetaB.contentType = "image/jpeg"
        
        assertFalse m.invokeIfNotExcluded(testMetaB)

        def testMeta2 = new ResourceMeta()
        testMeta2.sourceUrl = '/images/test.zip'
        testMeta2.actualUrl = '/images/test.zip'
        testMeta2.contentType = "application/zip"
        
        assertTrue m.invokeIfNotExcluded(testMeta2)

    }
}

class DummyMapper {
    def defaultExcludes
    def defaultIncludes
    def name
    def map
}