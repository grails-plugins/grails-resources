package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext

import grails.test.*

class ResourceServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
        mockLogging(ResourceService, true)
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

    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSRewritingWithMovingFiles() {
        def oldServletContext = ServletContextHolder.servletContext
        try {
            ServletContextHolder.servletContext = new MockServletContext() {
                InputStream getResourceAsStream(String uri) { 
                    println "In gRaS: $uri"
                    new ByteArrayInputStream("TEST CONTENT".bytes) 
                }
            }
        
            def svc = new ResourceService()
            def base = new File('./test-tmp/')
            def newBase = new File('./test-tmp/cached/')
            newBase.mkdirs()
            svc.addResourceMapper "TESTMOVER", { r ->
                // Pull it up 1 dir to screw up the URIs
                def f = new File(newBase, '_'+r.processedFile.name)
                assert r.processedFile.renameTo(f)
                r.processedFile = f
                r.updateActualUrlFromProcessedFile()
            }

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.processedFile = new File(base, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()
    
            /*
            We're going to test that these files are moved and mapped to CSS correctly
        
            /css/main.css       =>  /_main.css
        
            /images/theme/bg1.png   =>  /theme/_bg1.png     ==> CSS link    theme/_bg1.png
            /images/bg2.png         =>  /_bg2.png           ==> CSS link    images/_bg2.png
            /images/bg3.png         =>  /_bg3.png           ==> CSS link    /images/_bg3.png
            /images/bg4.png         =>  /_bg4.png           ==> CSS link    _bg4.png
            */
            def css = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
"""
            def cssInput = new ByteArrayInputStream(css.bytes)
            svc.fixCSSResourceLinks(r, cssInput)

            def outcome = r.processedFile.text
            def expected = """
.bg1 { background: url(../cached/_bg1.png) }
.bg2 { background: url(../cached/_bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(../cached/_bg4.png) }
"""
        
            assertEquals expected, outcome
        } finally {
            ServletContextHolder.servletContext = oldServletContext
        }
    }
}
