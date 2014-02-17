import grails.test.mixin.integration.IntegrationTestMixin
import grails.test.mixin.TestMixin

@TestMixin(IntegrationTestMixin)
class InitialisationSmokeTests {

    def grailsResourceProcessor
    
    /**
     * We are testing that the resources plugin operates correctly in an integration testing environment. 
     * That is, it does not cause issues for users when installed and they are running integration tests.
     * 
     * @see TestOnlyResources
     */
    void testInitialisedOk() {
        //@todo this temporarily removed until Grails fixes the problems with servletContext resource loading/another workaround found
        //assert grailsResourceProcessor.getModule("jquery") != null
    }

}