package org.grails.plugin.resource

import grails.test.GroovyPagesTestCase

class ResourceTagLibIntegTests extends GroovyPagesTestCase {
    
    def grailsResourceProcessor
    
    protected makeMockResource(uri) {
        [
            uri:uri, 
            disposition:'head', 
            exists: { -> true }
        ]
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
}
