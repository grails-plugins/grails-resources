package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase;

public class BundleResourceMapperTests extends GrailsUnitTestCase {

    BundleResourceMapper mapper = new BundleResourceMapper()

    void testRecognisedArtifactsAreBundledIfRequested() {

        Map<String, Boolean> expectedBundling = [
            'text/css': true,
            'text/javascript': true,
            'application/javascript': true,
            'application/x-javascript': true,
            'everything/nothing': false
        ]

        List resultingBundle
        Map grailsResourceProcessor = [
                findSyntheticResourceById: { String bundleId -> return resultingBundle },
        ]

        mapper.grailsResourceProcessor = grailsResourceProcessor

        expectedBundling.each { String resourceType, Boolean shouldBundle ->
            resultingBundle = [[existing:'bundle']]
            Map resource = [identifier:UUID.randomUUID(), contentType: resourceType, bundle:'myBundle', sourceUrlExtension:'js']

            mapper.map(resource, null)

            assert (resultingBundle[1]?.identifier == resource.identifier) == shouldBundle
        }

    }

}
