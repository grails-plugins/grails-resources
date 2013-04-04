package org.grails.plugin.resource.module

import grails.test.GroovyPagesTestCase

/**
 * Integration tests of constructing modules.
 *
 * @author peter
 */
class ModulesIntegTests extends GroovyPagesTestCase {

    def grailsResourceProcessor
    def grailsApplication

    protected makeMockResource(uri) {
        [uri:uri, disposition:'head', exists: { -> true }]
    }

    def testGrailsApplicationAccessInClosure() {

        def template = '''<html>
                            <head>
                              <r:require modules="testAppAccess"/>
                              <r:layoutResources/>
                            </head>
                            <body>
                              <h1>Hi</h1>
                            </body>
                          </html>'''
        def result = applyTemplate(template, [:])

        assertTrue result.contains("<!--${grailsApplication.ENV_DEVELOPMENT}-->")
    }
}
