package org.grails.plugin.resource

import grails.test.GroovyPagesTestCase
import org.junit.Before

import javax.activation.FileTypeMap
import javax.activation.MimetypesFileTypeMap

class ResourceTagLibIntegTests extends GroovyPagesTestCase {
    
    def grailsResourceProcessor

    @Before
    void setUp() {
        super.setUp()

        // adjust the mime type map used at the MockServletContext
        // to return the correct mime type for CSS and JS files
        // so that the bundle resource mapper will be applied to them
        MimetypesFileTypeMap fileTypeMap = (MimetypesFileTypeMap) FileTypeMap.getDefaultFileTypeMap()
        fileTypeMap.addMimeTypes(["text/javascript   js", "text/css   css"].join("\n"))

        // reload all modules with bundling applied to them
        grailsResourceProcessor.reloadAll()
    }
    
    def testExternalWithAbsoluteURI() {
        def result = applyTemplate('<r:external uri="https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js"/>', [:])
        assertTrue result.indexOf('"https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js') != -1
    }

    def testExternalWithAdhocResourceURIThatIsExcluded() {
        def result = applyTemplate('<r:external uri="js/core.js"/>', [:])
        assertTrue result.indexOf('/js/core.js') != -1
    }

    def testExternalWithAdhocResourceDirAndFileThatIsExcluded() {
        def result = applyTemplate('<r:external dir="js" file="core.js"/>', [:])
        assertTrue result.indexOf('/js/core.js') != -1
    }

    def testExternalWithAdhocResourceURI() {
        def result = applyTemplate('<r:external uri="js/adhoc.js"/>', [:])
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }

    def testExternalWithAdhocResourceURIWithSlash() {
        def result = applyTemplate('<r:external uri="/js/adhoc.js"/>', [:])
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }

    def testExternalWithAdhocResourceDirAndFile() {
        def result = applyTemplate('<r:external dir="js" file="adhoc.js"/>', [:])
        assertTrue result.indexOf('/static/js/_adhoc.js') != -1
    }
	
	def testGoogleFontsWithQueriesInModule() {
		def template = '''<html>
							<head>
							  <r:require modules="testurl"/>
							  <r:layoutResources/>
							</head>
							<body>
							  <h1>Hi</h1>
							</body>
						  </html>'''
		def result = applyTemplate(template, [:])
		def expectedLink = '<link href="http://fonts.googleapis.com/css?family=PT+Sans:400,700&subset=latin,cyrillic"'
		assertTrue result.contains(expectedLink)
	}
	
	def testGoogleMapsInModule() {
		def template = '''<html>
							<head>
							  <r:require modules="google-maps"/>
							  <r:layoutResources/>
							</head>
							<body>
							  <h1>Hi</h1>
							  <r:layoutResources/>
							</body>
						  </html>'''
		def result = applyTemplate(template, [:])
		def expectedScript = '<script src="http://maps.googleapis.com/maps/api/js?libraries=places&sensor=false"'
		assertTrue result.contains(expectedScript)
	}

    def testDispositionsOfTransitiveDependencies() {
        String template = '''
            <r:require modules="GPRESOURCES-207_module_A"/>

            <r:layoutResources disposition="disposition_A"/>
            <r:layoutResources disposition="disposition_B"/>
            <r:layoutResources disposition="disposition_C"/>
            <r:layoutResources disposition="disposition_D"/>
        '''
        String result = applyTemplate(template)

        assertTrue 'direct dependency "GPRESOURCES-207_module_A" - resource for disposition C', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
        assertTrue 'direct dependency "GPRESOURCES-207_module_A" - resource for disposition D', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_D.js")
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition C', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_C.js")
        // disposition B -- only via transitive dependency
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition B', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_B.js")
        // disposition A -- only via transitive dependency
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition A', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
    }

    def testDispositionsOfTransitiveDependenciesWithStashedResource() {
        String template = '''
            <r:require modules="GPRESOURCES-207_module_A"/>
            <r:script disposition="disposition_B">
            /*stashed*/
            </r:script>

            <r:layoutResources disposition="disposition_A"/>
            <r:layoutResources disposition="disposition_B"/>
            <r:layoutResources disposition="disposition_C"/>
            <r:layoutResources disposition="disposition_D"/>
        '''
        String result = applyTemplate(template)

        assertTrue 'direct dependency "GPRESOURCES-207_module_A" - resource for disposition C', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_C.js")
        assertTrue 'direct dependency "GPRESOURCES-207_module_A" - resource for disposition D', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_D.js")
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition C', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_C.js")
        assertTrue 'stashed script - disposition B', result.contains("/*stashed*/")
        // disposition B -- via transitive dependency and stashed script
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition B', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_B.js")
        // disposition A -- only via transitive dependency
        assertTrue 'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition A', result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
    }


    def testDuplicateIncludes() {
        String template = '''
            <r:require modules="GPRESOURCES-210_module_A"/>
            <r:layoutResources disposition="duplicate_includes_check"/>
        '''

        String result = applyTemplate(template)

        assertEquals 1, result.count("/static/_bundle-bundle_GPRESOURCES-210_module_A_duplicate_includes_check.js")
    }

    def testStyleTagWithDefaultDisposition() {
        String template = '''
            <r:style>
            /* stashed styles */
            </r:style>

            <r:layoutResources disposition="head"/>
        '''

        String result = applyTemplate(template)

        assertTrue result ==~ /\s*<style type="text\/css">\s*\/\* stashed styles \*\/\s*<\/style>\s*/
    }

    def testStyleTagWithCustomDisposition() {
        String template = '''
            <r:style disposition="custom_disposition">
            /* stashed styles */
            </r:style>

            <r:layoutResources disposition="custom_disposition"/>
        '''

        String result = applyTemplate(template)

        assertTrue result ==~ /\s*<style type="text\/css">\s*\/\* stashed styles \*\/\s*<\/style>\s*/
    }
}
