package org.grails.plugin.resource

import grails.test.*

class ResourceServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testRelativeCSSUris() {
        def svc = new ResourceService()
        
        assertEquals "images/bg_fade.png", svc.resolveURI('css/main.css', '../images/bg_fade.png')
        assertEquals "/images/bg_fade.png", svc.resolveURI('/css/main.css', '../images/bg_fade.png')
        assertEquals "/css/images/bg_fade.png", svc.resolveURI('/css/main.css', './images/bg_fade.png')
        assertEquals "css/images/bg_fade.png", svc.resolveURI('css/main.css', './images/bg_fade.png')
        assertEquals "bg_fade.png", svc.resolveURI('main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", svc.resolveURI('/main.css', 'bg_fade.png')
        assertEquals "css/bg_fade.png", svc.resolveURI('css/main.css', 'bg_fade.png')
        assertEquals "/css/bg_fade.png", svc.resolveURI('/css/main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", svc.resolveURI('/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", svc.resolveURI('css/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", svc.resolveURI('/css/main.css', '/bg_fade.png')
        assertEquals "http://somewhere.com/images/x.png", svc.resolveURI('css/main.css', 'http://somewhere.com/images/x.png')
    }
}
