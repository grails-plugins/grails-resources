package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class CSSPreprocessorTests extends GrailsUnitTestCase {
    
    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSPreprocessing() {
        mockLogging(CSSPreprocessor)

        def svc = [
            config : [ rewrite: [css: true] ]
        ]
        def base = new File('./test-tmp/')
        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = "text/css"
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
.bg5 { background: url(http://google.com/images/bg5.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        CSSPreprocessor.mapper(r, svc)

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/css/images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(resource:/css/bg4.png) }
.bg5 { background: url(http://google.com/images/bg5.png) }
"""
    
        assertEquals expected, outcome
    }
    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSPreprocessingWithInvalidURI() {
        mockLogging(CSSPreprocessor)

        def svc = [
            getResourceMetaForURI : {  uri, adHoc, postProc = null ->
                new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
            },
            config : [ rewrite: [css: true] ]
        ]

        def base = new File('./test-tmp/')

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        CSSPreprocessor.mapper(r, svc)

        def outcome = r.processedFile.text
        def expected = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""

        assertEquals expected, outcome
    }
}