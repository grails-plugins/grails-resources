package org.grails.plugin.resource

import grails.test.*

class URLUtilsTests extends GrailsUnitTestCase {
    void testRelativeCSSUris() {
        assertEquals "images/bg_fade.png", URLUtils.resolveURI('css/main.css', '../images/bg_fade.png')
        assertEquals "/images/bg_fade.png", URLUtils.resolveURI('/css/main.css', '../images/bg_fade.png')
        assertEquals "/css/images/bg_fade.png", URLUtils.resolveURI('/css/main.css', './images/bg_fade.png')
        assertEquals "css/images/bg_fade.png", URLUtils.resolveURI('css/main.css', './images/bg_fade.png')
        assertEquals "bg_fade.png", URLUtils.resolveURI('main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.resolveURI('/main.css', 'bg_fade.png')
        assertEquals "css/bg_fade.png", URLUtils.resolveURI('css/main.css', 'bg_fade.png')
        assertEquals "/css/bg_fade.png", URLUtils.resolveURI('/css/main.css', 'bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.resolveURI('/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.resolveURI('css/main.css', '/bg_fade.png')
        assertEquals "/bg_fade.png", URLUtils.resolveURI('/css/main.css', '/bg_fade.png')
        assertEquals "http://somewhere.com/images/x.png", URLUtils.resolveURI('css/main.css', 'http://somewhere.com/images/x.png')
    }
}