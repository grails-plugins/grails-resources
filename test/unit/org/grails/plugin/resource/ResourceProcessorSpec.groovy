package org.grails.plugin.resource

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext;

import spock.lang.Specification
import spock.lang.Unroll

class ResourceProcessorSpec extends Specification {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder()
    ResourceProcessor resourceProcessor
    MockHttpServletRequest request
    MockHttpServletResponse response
    ConfigObject config
    
    def setup() {
        resourceProcessor = new ResourceProcessor()
        resourceProcessor.staticUrlPrefix = 'static'
        config = new ConfigObject()
        File temporarySubfolder = temporaryFolder.newFolder('test-tmp')
        config.grails.resources.work.dir = temporarySubfolder.getAbsolutePath()
        def servletContext = new MockServletContext()
        resourceProcessor.grailsApplication = [
            config : config,
            mainContext : [servletContext: servletContext]
        ]
        resourceProcessor.servletContext = servletContext
        resourceProcessor.afterPropertiesSet()
        request = new MockHttpServletRequest(servletContext)
        request.contextPath = '/'
        response = new MockHttpServletResponse()
    }   

    @Unroll
    def "check default adhoc.includes/excludes settings - access to url #url should be #accepted"() {
        given:
        request.requestURI = url
        when: 'default adhoc.includes and adhoc.excludes are used'
        def uri = null
        def exception=null
        def result=false
        try {
            uri=resourceProcessor.removeQueryParams(resourceProcessor.extractURI(request, false))
            result=resourceProcessor.canProcessLegacyResource(uri)
        } catch (Exception e) {
            result=false
            exception=e
        }
        then:
        result == accepted
        when: 'adhoc.includes setting is accepting all resources'
        resourceProcessor.adHocIncludes = ['/**/*']
        try {
            result=resourceProcessor.canProcessLegacyResource(uri)
        } catch (Exception e) {
            result=false
            exception=e
        }
        then:
        result == accepted
        where:
        url | accepted
        '/static/images/image.png' | true
        '/static/js/file.js' | true
        '/static/WEB-INF/web.xml' | false
        '/static/wEb-iNf/web.xml' | false
        '/static/web-inf/web.xml' | false
        '/static/META-INF/MANIFEST.MF' | false
        '/static/meta-inf/MANIFEST.MF' | false
    }
}
