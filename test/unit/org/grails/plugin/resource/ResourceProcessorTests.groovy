package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ResourceProcessorTests extends GrailsUnitTestCase {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder()
    File temporarySubfolder
    def svc
    
    protected void setUp() {
        super.setUp()
        mockLogging(ResourceProcessor, true)
        temporarySubfolder = temporaryFolder.newFolder('test-tmp')

        svc = new ResourceProcessor()
        
        svc.grailsApplication = [
            config : [grails:[resources:[work:[dir:temporarySubfolder.getAbsolutePath()]]]],
            mainContext : [servletContext:[
                getResource: { uri -> 
                    assertTrue uri.indexOf('#') < 0
                    new URL('file:./test/test-files'+uri) 
                },
                getMimeType: { uri -> "test/nothing" }
            ]]
        ]
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testPrepareURIWithHashFragment() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

    void testPrepareAbsoluteURLWithQueryParams() {
        def r = new ResourceMeta()
        r.sourceUrl = 'http://crackhouse.ck/css/somehack.css?x=y#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals 'http://crackhouse.ck/css/somehack.css', meta.actualUrl
        assertEquals 'http://crackhouse.ck/css/somehack.css?x=y#whatever', meta.linkUrl
    }

    // GRESOURCES-116
    void testPrepareAbsoluteURLWithMissingExtension() {
        def r = new ResourceMeta()
        r.workDir = new File('/tmp/test')
        r.sourceUrl = 'http://maps.google.com/maps/api/js?v=3.5&sensor=false'
        r.disposition = 'head'
        r.tagAttributes = [type: 'js']

        ResourceMeta meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals 'http://maps.google.com/maps/api/js', meta.actualUrl
        assertEquals 'http://maps.google.com/maps/api/js?v=3.5&sensor=false', meta.linkUrl
        assertEquals([type: 'js'], meta.tagAttributes)
    }

    void testBuildResourceURIForGrails1_4() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

    void testBuildResourceURIForGrails1_3AndLower() {
        def r = new ResourceMeta()
        r.sourceUrl = '/somehack.xml#whatever'
        
        def meta = svc.prepareResource(r, true)
        assertNotNull meta
        assertEquals '/somehack.xml', meta.actualUrl
        assertEquals '/somehack.xml#whatever', meta.linkUrl
    }

    void testProcessLegacyResourceIncludesExcludes() {
        
        svc.adHocIncludes = ['/**/*.css', '/**/*.js', '/images/**']
        svc.adHocExcludes = ['/**/*.exe', '/**/*.gz', '/unsafe/**/*.css']

        def testData = [
            [requestURI: '/css/main.css', expected:true],
            [requestURI: '/js/code.js', expected:true],
            [requestURI: '/css/logo.png', expected:false],
            [requestURI: '/images/logo.png', expected:true],
            [requestURI: '/downloads/virus.exe', expected:false],
            [requestURI: '/downloads/archive.tar.gz', expected:false],
            [requestURI: '/unsafe/nested/problematic.css', expected:false]
        ]
        
        testData.each { d ->
            def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]
            
            // We know if it tried to handle it if it 404s, we can't be bothered to creat resourcemeta for all those
            def didHandle = false
            def response = [
                sendError: { code, msg = null -> didHandle = (code == 404) },
                sendRedirect: { uri -> }
            ]
            
            svc.processLegacyResource(request, response)
            
            assertEquals "Failed on ${d.requestURI}", d.expected, didHandle
        }
    }

    void testProcessLegactResourceIncludesExcludesSpecificFile() {
        
        svc.adHocIncludes = ['/**/*.js']
        svc.adHocExcludes = ['/**/js/something.js']

        def testData = [
            [requestURI: '/js/other.js', expected:true],
            [requestURI: '/js/something.js', expected:false],
            [requestURI: 'js/something.js', expected:false],
            [requestURI: '/xxx/js/something.js', expected:false],
            [requestURI: 'xxx/js/something.js', expected:false]
        ]
        
        testData.each { d ->
            def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]
            
            // We know if it tried to handle it if it 404s, we can't be bothered to create resourcemeta for all those
            def didHandle = false
            def response = [
                sendError: { code, msg = null -> didHandle = true },
                sendRedirect: { uri -> }
            ]
            
            svc.processLegacyResource(request, response)
            
            assertEquals "Failed on ${d.requestURI}", d.expected, didHandle
        }
    }
    
    void testDependencyOrdering() {
        svc.modulesByName = [
            a: [name:'a', dependsOn:['b']],
            e: [name:'e', dependsOn:['f', 'a']],
            b: [name:'b', dependsOn:['c']],
            c: [name:'c', dependsOn:['q']],
            d: [name:'d', dependsOn:['b', 'c']],
            f: [name:'f', dependsOn:['d']],
            z: [name:'z', dependsOn:[]],
            q: [name:'q', dependsOn:[]]
        ]
        svc.updateDependencyOrder()

        def res = svc.modulesInDependencyOrder
        def pos = { v ->
            res.indexOf(v)
        }

        println "Dependency order: ${res}"
        
        assertEquals res.size()-2, svc.modulesByName.keySet().size() // take off the synth + adhoc
        
        assertTrue pos('a') > pos('b')

        assertTrue pos('e') > pos('f')
        assertTrue pos('e') > pos('a')

        assertTrue pos('b') > pos('c')

        assertTrue pos('c') > pos('q')

        assertTrue pos('d') > pos('b')
        assertTrue pos('d') > pos('c')

        assertTrue pos('f') > pos('d')
    }
    
    void testWillNot404OnAdhocResourceWhenAccessedDirectlyFromStaticUrl() {
		svc.adHocIncludes = ['/**/*.xml']
		svc.staticUrlPrefix = '/static'
        def request = [contextPath:'resources', requestURI: 'resources/static/somehack.xml']
        
        def out = new ByteArrayOutputStream();
        def redirectUri = null
        
        def response = [
            sendError: { code, msg = null -> },
            sendRedirect: { uri -> redirectUri = uri },
            setContentLength: { l -> },
            setDateHeader: { d, l -> },
            outputStream: out
	    ]
	    
    
    	svc.processModernResource(request, response);
    	
    	// the response was written
    	assertTrue(out.size() > 0)
    	assertNull(redirectUri);
    	
    	// the legacy resource should now redirect
   	    svc.processLegacyResource(
   	      	[contextPath:'resources', 
   	    	requestURI: 'resources/somehack.xml'], 
   	    	response);
   	    	
    	assertNotNull(redirectUri);   	    	
    }
}

