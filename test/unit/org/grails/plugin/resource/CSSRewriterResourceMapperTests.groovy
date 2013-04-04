package org.grails.plugin.resource

class CSSRewriterResourceMapperTests extends AbstractResourcePluginTests {

    protected void setUp() {
        super.setUp()
        mockLogging(CSSRewriterResourceMapper)
    }

    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSRewritingWithMovingFiles() {

        def r = new ResourceMeta(sourceUrl:'/css/main.css')

        ResourceProcessor.metaClass.getConfig = { -> [rewrite: [css: true]] as ConfigObject }
        ResourceProcessor.metaClass.getResourceMetaForURI = { String uri, Boolean adHoc, String declRes, Closure postProc ->
            def namepart = uri[uri.lastIndexOf('/')..-1]
            def s = '/cached' + namepart
            def newRes = new ResourceMeta(actualUrl: s)
            r.declaringResource = declRes
            if (postProc) postProc(newRes)
            assertEquals 'CSS rewriter did not set declaring resource correctly', r.sourceUrl, declRes
            return newRes
        }

        def svc = new ResourceProcessor()

        r.workDir = temporarySubfolder
        r.actualUrl = r.sourceUrl
        r.contentType = "text/css"
        r.processedFile = new File(temporarySubfolder, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSRewriterResourceMapper(grailsResourceProcessor: svc).map r, new ConfigObject()

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

        ResourceProcessor.metaClass.getConfig = { -> [rewrite: [css: true]] as ConfigObject }
        ResourceProcessor.metaClass.getResourceMetaForURI = { String uri, Boolean adHoc, String declRes, Closure postProc ->
            new ResourceMeta(actualUrl: uri, processedFile: new File(uri + '.gz'))
        }
        def svc = new ResourceProcessor()

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = temporarySubfolder
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(temporarySubfolder, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSRewriterResourceMapper(grailsResourceProcessor: svc).map r, new ConfigObject()

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

        ResourceProcessor.metaClass.getConfig = { -> [rewrite: [css: true]] as ConfigObject }
        ResourceProcessor.metaClass.getResourceMetaForURI = { String uri, Boolean adHoc, String declRes, Closure postProc ->
            new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
        }
        def svc = new ResourceProcessor()

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = temporarySubfolder
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(temporarySubfolder, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSRewriterResourceMapper(grailsResourceProcessor: svc).map r, new ConfigObject()

        def outcome = r.processedFile.text
        def expected = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""

        assertEquals expected, outcome
    }

    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSRewritingWithQueryParamsAndFragment() {

        ResourceProcessor.metaClass.getConfig = { -> [rewrite: [css: true]] as ConfigObject }
        ResourceProcessor.metaClass.getResourceMetaForURI = { String uri, Boolean adHoc, String declRes, Closure postProc ->
            def r = new ResourceMeta(sourceUrl: uri)
            r.actualUrl = r.sourceUrl
            r.actualUrl += '.gz' // frig it as if a mapper changed it
            return r
        }
        ResourceProcessor.metaClass.getResource = { String uri -> new URL('file:./test/test-files' + uri) }
        ResourceProcessor.metaClass.getMimeType = { String uri -> 'test/nothing' }
        def svc = new ResourceProcessor()

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = temporarySubfolder
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(temporarySubfolder, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { behaviour: url(resource:/image.png?arg1=value1) }
.bg2 { background: url(resource:/image.png#bogus-but-what-the-hell) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSRewriterResourceMapper(grailsResourceProcessor: svc).map r, new ConfigObject()

        def outcome = r.processedFile.text

        println "Output: $outcome"
        def expected = """
.bg1 { behaviour: url(../image.png.gz?arg1=value1) }
.bg2 { background: url(../image.png.gz#bogus-but-what-the-hell) }
"""
        assertEquals expected, outcome
    }

    void testCSSRewritingWithAbsoluteLinkOverride() {

        ResourceProcessor.metaClass.getConfig = { -> [rewrite: [css: true]] as ConfigObject }
        ResourceProcessor.metaClass.getResourceMetaForURI = { String uri, Boolean adHoc, String declRes, Closure postProc ->
            new ResourceMeta(sourceUrl: uri, actualUrl: "http://mycdn.somewhere.com/myresources/x.jpg")
        }
        ResourceProcessor.metaClass.getResource = { String uri -> new URL('file:./test/test-files' + uri) }
        ResourceProcessor.metaClass.getMimeType = { String uri -> 'test/nothing' }
        def svc = new ResourceProcessor()

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = temporarySubfolder
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(temporarySubfolder, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(resource:/image.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSRewriterResourceMapper(grailsResourceProcessor: svc).map r, new ConfigObject()

        def outcome = r.processedFile.text

        println "Output: $outcome"
        def expected = """
.bg1 { background: url(http://mycdn.somewhere.com/myresources/x.jpg) }
"""

        assertEquals expected, outcome
    }
}
