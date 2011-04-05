package org.grails.plugin.resource

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class CSSRewriterResourceMapperTests extends GrailsUnitTestCase {
    
    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSRewritingWithMovingFiles() {
        mockLogging(CSSRewriterResourceMapper)

        def r = new ResourceMeta(sourceUrl:'/css/main.css')

        def svc = [
            getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                def namepart = uri[uri.lastIndexOf('/')..-1]
                def s = '/cached'+namepart
                def newRes = new ResourceMeta(actualUrl: s)
                r.declaringResource = declRes
                if (postProc) postProc(newRes)
                assertEquals 'CSS rewriter did not set declaring resource correctly', 
                    r.sourceUrl, declRes
                return newRes
            },
            config : [ rewrite: [css: true] ]
        ]

        def base = new File('./test-tmp/')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = "text/css"
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        CSSRewriterResourceMapper.newInstance().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(../cached/bg1.png) }
.bg2 { background: url(../cached/bg2.png) }
.bg3 { background: url(../cached/bg3.png) }
.bg4 { background: url(../cached/bg4.png) }
"""
    
        assertEquals expected, outcome
    }

    /**
     * This simulates a mapping where the image resources are renamed but left in the same location,
     * and the actualUrl is not mutated (i.e. like zipping)
     */
    void testCSSRewritingWithRenamedFilesBySameUrl() {
        mockLogging(CSSRewriterResourceMapper)

        def svc = [
            getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
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
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        
        CSSRewriterResourceMapper.newInstance().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(../images/bg2.png) }
.bg3 { background: url(../images/bg3.png) }
.bg4 { background: url(../bg4.png) }
"""

        assertEquals expected, outcome
    }

    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSRewritingWithInvalidURI() {
        mockLogging(CSSRewriterResourceMapper)

        def svc = [
            getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
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
    
        CSSRewriterResourceMapper.newInstance().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""

        assertEquals expected, outcome
    }

}