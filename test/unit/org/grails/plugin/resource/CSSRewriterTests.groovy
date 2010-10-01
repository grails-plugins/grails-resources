package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class CSSRewriterTests extends GrailsUnitTestCase {
    
    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSRewritingWithMovingFiles() {
        mockLogging(CSSRewriter)

        def svc = [
            getResourceMetaForURI : {  uri, adHoc, postProc = null ->
                def namepart = uri[uri.lastIndexOf('/')..-1]
                def s = '/cached'+namepart
                println "In mock svc returning res for $s"
                new ResourceMeta(actualUrl: s)
            },
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
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        CSSRewriter.mapper(r, svc)

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(../cached/bg1.png) }
.bg2 { background: url(../cached/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(../cached/bg4.png) }
"""
    
        assertEquals expected, outcome
    }

    /**
     * This simulates a mapping where the image resources are renamed but left in the same location,
     * and the actualUrl is not mutated (i.e. like zipping)
     */
    void testCSSRewritingWithRenamedFilesBySameUrl() {
        mockLogging(CSSRewriter)

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
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        CSSRewriter.mapper(r, svc)

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
"""

        assertEquals expected, outcome
    }
}