package org.grails.plugin.resource

import grails.test.*

import org.grails.plugin.resource.util.HalfBakedLegacyLinkGenerator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

class ResourceTagLibTests extends TagLibUnitTestCase {
    protected void setUp() {
        super.setUp()
        
        Object.metaClass.encodeAsHTML = { -> delegate.toString() }
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testLinkResolutionForGrails2() {
        tagLib.grailsLinkGenerator = [
            resource: { attrs ->
                "${attrs.contextPath}/${attrs.dir}/${attrs.file}"
            }
        ]
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes,  postProc -> 
                assertEquals "/images/favicon.ico", uri
                def r = new ResourceMeta()
                r.with {
                    sourceUrl = uri
                    actualUrl = uri
                }
                return r
            },
            staticUrlPrefix: '/static'
        ]

        tagLib.request.contextPath = "/CTX"
        
        def res = tagLib.resolveResourceAndURI(dir:'images', file:'favicon.ico')
        assertEquals "/CTX/static/images/favicon.ico", res.uri
    }
    
    void testLinkResolutionForGrails2ResourceExcluded() {
        tagLib.grailsLinkGenerator = [
            resource: { attrs ->
                "${attrs.contextPath}/${attrs.dir}/${attrs.file}"
            }
        ]
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes,  postProc -> 
                assertEquals "/images/favicon.ico", uri
                return null // Excluded
            },
            staticUrlPrefix: '/static'
        ]

        tagLib.request.contextPath = "/CTX"
        
        def res = tagLib.resolveResourceAndURI(dir:'images', file:'favicon.ico')
        assertEquals "/CTX/images/favicon.ico", res.uri
    }
    
    void testLinkResolutionForGrails1_3AndEarlier() {
        tagLib.grailsLinkGenerator = new HalfBakedLegacyLinkGenerator()
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes,  postProc -> 
                assertEquals "/images/favicon.ico", uri
                def r = new ResourceMeta()
                r.with {
                    sourceUrl = uri
                    actualUrl = uri
                }
                return r
            },
            staticUrlPrefix: '/static'
        ]

        tagLib.request.contextPath = "/CTX"

        def res = tagLib.resolveResourceAndURI(dir:'images', file:'favicon.ico')
        assertEquals "/CTX/static/images/favicon.ico", res.uri
    }
    
    void testAbsoluteDirFileLinkResolution() {
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes,  postProc -> 
                assertEquals "/images/default-avatar.png", uri
                def r = new ResourceMeta()
                r.with {
                    sourceUrl = uri
                    actualUrl = uri
                }
                return r
            },
            staticUrlPrefix: '/static'
        ]

        // We're just testing what happens if the link generator gave us back something absolute
        tagLib.request.contextPath = "/CTX"

        tagLib.grailsLinkGenerator = [resource: { args -> "http://myserver.com/CTX/static/"+args.dir+'/'+args.file } ]
        def res = tagLib.resolveResourceAndURI(absolute:true, dir:'images', file:'default-avatar.png')
        assertEquals "http://myserver.com/CTX/static/images/default-avatar.png", res.uri
    }
    
    void testResourceLinkWithRelOverride() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/css/test.less'
        testMeta.actualUrl = '/css/test.less'
        testMeta.disposition = 'head'
        
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes,  postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.external(uri:'/css/test.less', rel:'stylesheet/less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
        assertTrue output.contains('href="/static/css/test.less"')
    }

    void testResourceLinkWithRelOverrideFromResourceDecl() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/css/test.less'
        testMeta.actualUrl = '/css/test.less'
        testMeta.contentType = "stylesheet/less"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [rel:'stylesheet/less']

        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.external(uri:'/css/test.less', type:'css').toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet/less"')
        assertTrue output.contains('href="/static/css/test.less"')
    }

    void testResourceLinkWithWrapperAttribute() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/css/ie.css'
        testMeta.actualUrl = '/css/ie.css'
        testMeta.contentType = "text/css"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [rel:'stylesheet']

        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.external(uri:'/css/ie.less', type:'css', wrapper: { s -> "WRAPPED${s}WRAPPED" }).toString()
        println "Output was: $output"
        assertTrue output.contains('rel="stylesheet"')
        assertFalse "Should not contain the wrapper= attribute in output", output.contains('wrapper=')
        assertTrue output.contains('WRAPPED<link')
        assertTrue output.contains('/>WRAPPED')
    }

    void testRenderModuleWithNonExistentResource() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/this/is/bull.css'
        testMeta.contentType = "test/stylesheet"
        testMeta.disposition = 'head'
        testMeta._resourceExists = false
        testMeta.tagAttributes = [rel:'stylesheet']
        
        def testMod = new ResourceModule() 
        testMod.resources << testMeta
        
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static',
            getModule : { name -> testMod }
        ]

        shouldFail(IllegalArgumentException) {
            def output = tagLib.renderModule(name:'test').toString()
        }
    }

    void testImgTagWithAttributes() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/images/test.png'
        testMeta.actualUrl = '/images/test.png'
        testMeta.contentType = "image/png"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [width:'100', height:'50', alt:'mugshot']
        
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.img(uri:'/images/test.png').toString()
        println "Output was: $output"
        assertTrue output.contains('width="100"')
        assertTrue output.contains('height="50"')
        assertTrue output.contains('alt="mugshot"')
        assertTrue output.contains('src="/static/images/test.png"')
        assertFalse output.contains('uri=')
    }

    void testImgTagWithAttributesDefaultDir() {
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = '/images/test.png'
        testMeta.actualUrl = '/images/test.png'
        testMeta.contentType = "image/png"
        testMeta.disposition = 'head'
        testMeta.tagAttributes = [width:'100', height:'50', alt:'mugshot']

        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> false },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        tagLib.grailsLinkGenerator = [
            resource: {attrs ->
                assertEquals 'test.png', attrs.file
                assertEquals 'images', attrs.dir

                return '/images/test.png'
            }
        ]
        def output = tagLib.img(file:'test.png').toString()
        println "Output was: $output"
        assertTrue output.contains('width="100"')
        assertTrue output.contains('height="50"')
        assertTrue output.contains('src="/static/images/test.png"')
        assertFalse output.contains('file=')
        assertFalse output.contains('dir=')
    }

    def testDebugModeResourceLinkWithAbsoluteCDNURL() {

        def url = 'https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js'
        def testMeta = new ResourceMeta()
        testMeta.sourceUrl = url
        testMeta.actualUrl = url
        testMeta.disposition = 'head'
        
        tagLib.request.contextPath = "/resourcestests"
        
        tagLib.grailsResourceProcessor = [
            isDebugMode: { r -> true },
            getResourceMetaForURI: { uri, adhoc, declRes, postProc -> testMeta },
            staticUrlPrefix: '/static'
        ]
        def output = tagLib.external(uri:url, type:"js").toString()
        println "Output was: $output"
        assertTrue output.contains('src="https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js?_debugResources')
    }
    
    def testRequireUpdatesRequestAttributes() {
        tagLib.grailsResourceProcessor = [
            addModuleDispositionsToRequest: { req, module -> }
        ]

        def output = tagLib.require(modules:['thingOne', 'thingTwo']).toString()
        
        def tracker = tagLib.request.resourceModuleTracker
        assertNotNull tracker
        assertEquals 3, tracker?.size()
        assertTrue tracker.containsKey('thingOne')
        assertEquals true, tracker.thingOne
        assertTrue tracker.containsKey('thingTwo')
        assertEquals true, tracker.thingOne
        assertTrue tracker.containsKey(ResourceProcessor.IMPLICIT_MODULE)
        assertEquals false, tracker[ResourceProcessor.IMPLICIT_MODULE]
    }
    
    def testRequireIndicatesModuleNotMandatory() {
        tagLib.grailsResourceProcessor = [
            addModuleDispositionsToRequest: { req, module -> }
        ]

        def output = tagLib.require(modules:['thingOne', 'thingTwo'], strict:false).toString()
        
        def tracker = tagLib.request.resourceModuleTracker
        assertNotNull tracker
        assertEquals 3, tracker?.size()
        assertTrue tracker.containsKey('thingOne')
        assertEquals false, tracker.thingOne
        assertTrue tracker.containsKey('thingTwo')
        assertEquals false, tracker.thingTwo
        assertTrue tracker.containsKey(ResourceProcessor.IMPLICIT_MODULE)
        assertEquals false, tracker[ResourceProcessor.IMPLICIT_MODULE]
    }

    def testExternalTagCanWorkWithUrlUriOrDir() {

        try {
            tagLib.external(uri: '/fake/url')
            tagLib.external(url: '/fake/url')
            tagLib.external(file: 'myfile.js')
        } catch (GrailsTagException e) {
            fail 'We should allow the tag to be used with any of the above attributes present'
        } catch (Exception e) {
            // We expect this because the rest of the tag isn't mocked.
        }
        
    }

    def testExternalTagRequiresUrlUriOrDir() {

        try {
            tagLib.external([:])
            fail 'Should have thrown an exception due to missing required attributes'
        } catch (Exception e) {
            assert e.message == 'For the &lt;r:external /&gt; tag, one of the attributes [uri, url, file] must be present'
        }

    }
}
