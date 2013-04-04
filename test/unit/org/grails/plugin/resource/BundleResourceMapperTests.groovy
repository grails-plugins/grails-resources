package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase

class BundleResourceMapperTests extends GrailsUnitTestCase {

    private BundleResourceMapper mapper = new BundleResourceMapper()

    void testRecognisedArtifactsAreBundledIfRequested() {

        Map<String, Boolean> expectedBundling = [
            'text/css': true,
            'text/javascript': true,
            'application/javascript': true,
            'application/x-javascript': true,
            'everything/nothing': false
        ]

        List resultingBundle

        ResourceProcessor.metaClass.findSyntheticResourceById = { String bundleId -> resultingBundle }
        mapper.grailsResourceProcessor = new ResourceProcessor()

        expectedBundling.each { String resourceType, Boolean shouldBundle ->
            resultingBundle = [[existing:'bundle']]
				Map resource = [identifier:UUID.randomUUID(), contentType: resourceType, bundle:'myBundle', sourceUrlExtension:'js']

            mapper.map resource, new ConfigObject()

            assert (resultingBundle[1]?.identifier == resource.identifier) == shouldBundle
        }
    }
}
