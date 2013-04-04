package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase

import org.springframework.util.AntPathMatcher

class PathMatcherTests extends GrailsUnitTestCase {
    static final PATH_MATCHER = new AntPathMatcher()

    void testDeepMatching() {
        assertTrue PATH_MATCHER.match('**/.svn', 'web-app/images/.svn')
        assertFalse PATH_MATCHER.match('**/.svn', 'web-app/images/.svn/test.jpg')
        assertTrue PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/test.jpg')
        assertTrue PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/images/logos/test.jpg')
        assertFalse PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/images/logos/test.png')
        assertTrue PATH_MATCHER.match('**/.svn/**/*.*', 'web-app/images/.svn/images/logos/test.png')
        assertTrue PATH_MATCHER.match('**/.svn/**/*.*', 'web-app/images/.svn/css/test.css')
    }
}
