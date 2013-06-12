package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase

class URLUtilsTests extends GrailsUnitTestCase {

    void testRelativeCSSUris() {
        assertEquals "images/bg_fade.png", URLUtils.relativeURI('css/main.css', '../images/bg_fade.png')
        assertEquals "/images/bg_fade.png", URLUtils.relativeURI('/css/main.css', '../images/bg_fade.png')
        assertEquals "/css/images/bg_fade.png", URLUtils.relativeURI('/css/main.css', './images/bg_fade.png')
        assertEquals "css/images/bg_fade.png", URLUtils.relativeURI('css/main.css', './images/bg_fade.png')
        assertEquals "bg_fade.png", URLUtils.relativeURI('main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.relativeURI('/main.css', 'bg_fade.png')
        assertEquals "css/bg_fade.png", URLUtils.relativeURI('css/main.css', 'bg_fade.png')
        assertEquals "/css/bg_fade.png", URLUtils.relativeURI('/css/main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.relativeURI('/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.relativeURI('css/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.relativeURI('/css/main.css', '/bg_fade.png')
        assertEquals "http://somewhere.com/images/x.png", URLUtils.relativeURI('css/main.css', 'http://somewhere.com/images/x.png')
    }

    void testIsRelativeForServerRelativeUrls() {
        assertTrue URLUtils.isRelativeURL("/server/relative")
    }

    void testIsRelativeForRelativeToCurrentPath() {
        assertTrue URLUtils.isRelativeURL("relative/to/current/path")
    }

    void testIsRelativeForRelativeToCurrentPathViaParent() {
        assertTrue URLUtils.isRelativeURL("../relative/to/current/path")
    }

    void testIsRelativeForDataUrls() {
        assertFalse URLUtils.isRelativeURL("data:xyz")
    }

    void testIsRelativeForPageFragments() {
        assertFalse URLUtils.isRelativeURL("#fragment_only")
    }

    void testIsRelativeForAbsoluteUrls() {
        assertFalse URLUtils.isRelativeURL("http://www.example.org/absolute/path")
    }

}