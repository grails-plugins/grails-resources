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

    
    void testRelativePathCalculations() {        
        def data = [
            // Expected, base, target
            ["../images/logo.png", './test-tmp/css/main.css', './test-tmp/images/logo.png'],
            ["../logo.png", './test-tmp/css/main.css', './test-tmp/logo.png'],
            ["_yyyyyy.png", './test-tmp/_xxxxxx.css', './test-tmp/_yyyyyy.png'],
            ["notgonnahappen/_yyyyyy.png", './test-tmp/_xxxxxx.css', './test-tmp/notgonnahappen/_yyyyyy.png'],
            ["../notgonnahappen/really/_yyyyyy.png", './test-tmp/css/_xxxxxx.css', './test-tmp/notgonnahappen/really/_yyyyyy.png'],
            ["../../notgonnahappen/really/_yyyyyy.png", './test-tmp/css/deep/_xxxxxx.css', './test-tmp/notgonnahappen/really/_yyyyyy.png'],
            ["../../_yyyyyy.png", './test-tmp/css/deep/_xxxxxx.css', './test-tmp/_yyyyyy.png']
        ]

        data.each { d ->
            def r = new ResourceMeta()
            r.processedFile = new File(d[2])
            assertEquals d[0], r.relativeTo(new File(d[1]))
        }
    }
}

