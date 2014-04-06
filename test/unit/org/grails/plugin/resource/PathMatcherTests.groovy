package org.grails.plugin.resource

import org.springframework.util.AntPathMatcher
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

@TestMixin(GrailsUnitTestMixin)
class PathMatcherTests {
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
