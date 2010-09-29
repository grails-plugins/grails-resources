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
    
    void testCSSRewritingWithMovingFiles() {
        def svc = new ResourceService()
        svc.addResourceMapper "test", { r ->
            def f = new File(r.processedFile.parentFile, '_'+r.processedFile.name)
            assert r.processedFile.renameTo(f)
            r.processedFile = f
            r.updateActualUrlFromProcessedFile()
        }

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.processedFile = new File('./test-tmp/rewritten.css')
        assert r.processedFile.parentFile.mkdirs()
        
        def css = """
.bg1 { background: url(../images/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
"""
        def cssInput = new ByteArrayInputStream(css.bytes)
        svc.fixCSSResourceLinks(r, cssInput)

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(../images/_bg1.png) }
.bg2 { background: url(images/_bg2.png) }
.bg3 { background: url(/images/_bg3.png) }
.bg4 { background: url(_bg4.png) }
"""
        
        assertEquals expected, outcome
    }
}
