package org.grails.plugin.resource

import grails.test.*

class ResourceMetaTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testMovingFileUpdatesActualUrlCorrectly() {
        def r = new ResourceMeta()
        r.sourceUrl = "/jquery/images/bg.png"
        r.workDir = new File('/tmp/test')
        r.processedFile = new File('/tmp/test/123456789.png')
        r.updateActualUrlFromProcessedFile()
        
        assertEquals "/jquery/images/bg.png", r.sourceUrl
        assertEquals "/123456789.png", r.actualUrl
        assertEquals "/123456789.png", r.linkUrl
    }

    void testRenamingFileUpdatesActualUrlCorrectly() {
        def r = new ResourceMeta()
        r.sourceUrl = "/jquery/images/bg.png"
        r.workDir = new File('/tmp/test')
        r.processedFile = new File('/tmp/test/jquery/images/bg.png.gz')
        r.updateActualUrlFromProcessedFile()
        
        // All results must be abs to the work dir, with leading /
        assertEquals "/jquery/images/bg.png", r.sourceUrl
        assertEquals "/jquery/images/bg.png.gz", r.actualUrl
        assertEquals "/jquery/images/bg.png.gz", r.linkUrl
    }

    void testCSSURLWithHackyMozillaAnchorCrapStripsAnchor() {
        def r = new ResourceMeta()
        r.workDir = new File('/tmp/test')
        r.sourceUrl = "/jquery/images/bg.png#crackaddicts"
        r.processedFile = new File('/tmp/test/jquery/images/bg.png')
        r.updateActualUrlFromProcessedFile()
        
        // All results must be abs to the work dir, with leading /
        assertEquals "Source url should have anchor stripped from it", "/jquery/images/bg.png", r.sourceUrl
        assertEquals "/jquery/images/bg.png", r.actualUrl
        assertEquals "#crackaddicts", r.sourceUrlParamsAndFragment
        assertEquals "/jquery/images/bg.png#crackaddicts", r.linkUrl
    }

    void testCSSURLWithAnchorAndQueryParamsMaintained() {
        def r = new ResourceMeta()
        r.workDir = new File('/tmp/test')
        r.sourceUrl = "/jquery/images/bg.png?you=got&to=be&kidding=true#crackaddicts"
        r.processedFile = new File('/tmp/test/jquery/images/bg.png')
        r.updateActualUrlFromProcessedFile()
        
        // All results must be abs to the work dir, with leading /
        assertEquals "Source url should have anchor stripped from it", "/jquery/images/bg.png", r.sourceUrl
        assertEquals "/jquery/images/bg.png", r.actualUrl
        assertEquals "?you=got&to=be&kidding=true#crackaddicts", r.sourceUrlParamsAndFragment
        assertEquals "/jquery/images/bg.png?you=got&to=be&kidding=true#crackaddicts", r.linkUrl
    }

    void testAbsoluteURLWithAnchorAndQueryParamsMaintained() {
        def r = new ResourceMeta()
        r.workDir = new File('/tmp/test')
        r.sourceUrl = "http://crackhouse.ck/jquery/images/bg.png?you=got&to=be&kidding=true#crackaddicts"
        r.updateActualUrlFromProcessedFile()
        
        // All results must be abs to the work dir, with leading /
        assertEquals "Source url should have anchor stripped from it", "http://crackhouse.ck/jquery/images/bg.png", r.sourceUrl
        assertEquals "http://crackhouse.ck/jquery/images/bg.png", r.actualUrl
        assertEquals "?you=got&to=be&kidding=true#crackaddicts", r.sourceUrlParamsAndFragment
        assertEquals "http://crackhouse.ck/jquery/images/bg.png?you=got&to=be&kidding=true#crackaddicts", r.linkUrl
    }

    void testRelativePathCalculations() {        
        def data = [
            // Expected, base, target
            ["../images/logo.png", '/css/main.css', '/images/logo.png'],
            ["../logo.png", '/css/main.css', '/logo.png'],
            [ 'images/ui-bg_fine-grain_10_eceadf_60x60.png', '/css/xx/jquery-ui-1.8.16.custom.css', "/css/xx/images/ui-bg_fine-grain_10_eceadf_60x60.png"],
            ["_yyyyyy.png", '/_xxxxxx.css', '/_yyyyyy.png'],
            ["notgonnahappen/_yyyyyy.png", '/_xxxxxx.css', '/notgonnahappen/_yyyyyy.png'],
            ["../notgonnahappen/really/_yyyyyy.png", '/css/_xxxxxx.css', '/notgonnahappen/really/_yyyyyy.png'],
            ["../../notgonnahappen/really/_yyyyyy.png", '/css/deep/_xxxxxx.css', '/notgonnahappen/really/_yyyyyy.png'],
            ["../../_yyyyyy.png", '/css/deep/_xxxxxx.css', '/_yyyyyy.png'],
            ["_xxxx.png", '/css/_zzzzz.css', '/css/_xxxx.png'],
            ["../css/_xxxx.png", '/css2/_zzzzz.css', '/css/_xxxx.png'],
            ["../css2/_xxxx.png", '/css/_zzzzz.css', '/css2/_xxxx.png']
        ]

        data.each { d ->
            println "Trying: ${d}"
            def r = new ResourceMeta(actualUrl:d[2])
            assertEquals d[0], r.relativeTo(new ResourceMeta(actualUrl:d[1]) )
        }
    }
}

