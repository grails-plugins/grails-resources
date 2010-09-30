package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class ResourceServiceTests extends GrailsUnitTestCase {
    def svc
    
    protected void setUp() {
        super.setUp()
        mockLogging(ResourceService, true)
        svc = new ResourceService()
        
        svc.grailsApplication = [
            config : new ConfigObject()
        ]
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testRelativeCSSUris() {
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
                    new ByteArrayInputStream("TEST CONTENT".bytes) 
                }
            }
        
            def base = new File('./test-tmp/')
            def newBase = new File('./test-tmp/cached/')
            newBase.mkdirs()
            svc.addResourceMapper "TESTMOVER", { r ->
                // Pull it up 1 dir to screw up the URIs
                def f = new File(newBase, '_'+r.processedFile.name)
                assert r.processedFile.renameTo(f)
                r.workDir = base
                r.processedFile = f
                r.updateActualUrlFromProcessedFile()
            }

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = base
            r.actualUrl = r.sourceUrl
            r.processedFile = new File(base, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()
    
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

    /**
     * This simulates a mapping where the image resources are renamed but left in the same location,
     * and the actualUrl is not mutated (i.e. like zipping)
     */
    void testCSSRewritingWithRenamedFilesBySameUrl() {
        def oldServletContext = ServletContextHolder.servletContext
        try {
            ServletContextHolder.servletContext = new MockServletContext() {
                InputStream getResourceAsStream(String uri) {
                    new ByteArrayInputStream("TEST CONTENT".bytes) 
                }
            }

            def base = new File('./test-tmp/')
            svc.addResourceMapper "TESTZIPPER", { r ->
                // Pull it up 1 dir to screw up the URIs
                def f = new File(r.processedFile.name+'.gz')
                r.workDir = base
                assert r.processedFile.renameTo(f)
                r.processedFile = f
            }

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = base
            r.actualUrl = r.sourceUrl
            r.processedFile = new File(base, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

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
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
"""

            assertEquals expected, outcome
        } finally {
            ServletContextHolder.servletContext = oldServletContext
        }
    }
}
