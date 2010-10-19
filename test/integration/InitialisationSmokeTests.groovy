class InitialisationSmokeTests {

    def resourceService
    
    /**
     * We are testing that the resources plugin operates correctly in an integration testing environment. 
     * That is, it does not cause issues for users when installed and they are running integration tests.
     * 
     * @see TestOnlyResources
     */
    void testInitialisedOk() {
        assert resourceService.getModule("jquery") != null
    }

}