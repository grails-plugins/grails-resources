package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.mapper.ResourceMapper

@TestMixin(GrailsUnitTestMixin)
class ResourceMapperTests {
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

    void testResourceExclusionOfMapper() {
          def artefact = new DummyMapper()
          artefact.defaultIncludes = ['**/*.*']
          artefact.name = 'minify'
          artefact.map = { res, config ->
          }

          def m = new ResourceMapper(artefact, [minify:[:]])

          def artefact2 = new DummyMapper()
          artefact2.defaultIncludes = ['**/*.*']
          artefact2.name = 'other'
          artefact2.map = { res, config ->
          }

          def m2 = new ResourceMapper(artefact2, [other:[:]])

          def testMeta = new ResourceMeta()
          testMeta.sourceUrl = '/images/test.png'
          testMeta.actualUrl = '/images/test.png'
          testMeta.contentType = "image/png"
          testMeta.excludedMappers = ['minify'] as Set

          assertFalse m.invokeIfNotExcluded(testMeta)
          assertTrue m2.invokeIfNotExcluded(testMeta)
    }

    void testResourceExclusionOfOperation() {
          def artefact = new DummyMapper()
          artefact.defaultIncludes = ['**/*.*']
          artefact.name = 'yuicssminifier'
          artefact.operation = 'minify'
          artefact.map = { res, config ->
          }

          def m = new ResourceMapper(artefact, [minify:[:]])

          def artefact2 = new DummyMapper()
          artefact2.defaultIncludes = ['**/*.*']
          artefact2.name = 'googlecssminifier'
          artefact2.operation = 'minify'
          artefact2.map = { res, config ->
          }

          def m2 = new ResourceMapper(artefact2, [other:[:]])

          def testMeta = new ResourceMeta()
          testMeta.sourceUrl = '/images/test.css'
          testMeta.actualUrl = '/images/test.css'
          testMeta.contentType = "text/css"
          testMeta.excludedMappers = ['minify'] as Set

          assertFalse m.invokeIfNotExcluded(testMeta)
          assertFalse m2.invokeIfNotExcluded(testMeta)

          testMeta.excludedMappers = null
          assertTrue m.invokeIfNotExcluded(testMeta)
          assertTrue m2.invokeIfNotExcluded(testMeta)
    }
}

class DummyMapper {
    def defaultExcludes
    def defaultIncludes
    def name
    def map
    def operation
}