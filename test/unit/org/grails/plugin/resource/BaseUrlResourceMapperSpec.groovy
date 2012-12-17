package org.grails.plugin.resource

import grails.plugin.spock.UnitSpec
import org.grails.plugin.resource.BaseUrlResourceMapper

class BaseUrlResourceMapperSpec extends UnitSpec{

    def mapper

    def setup(){
        mapper = new BaseUrlResourceMapper()
    }

    def "test that mappers are configured correctly"(){
        setup:
            def resource = [ linkUrl : '/images.jpg' ]
            def config = [ enabled: true, default: 'http://www.google.com/' ]
        when:
            mapper.map( resource, config )
        then:
            resource.linkOverride == 'http://www.google.com/images.jpg'
    }

    def "when mappers are disabled, links are not processed"(){
        setup:
            def resource = [ linkUrl : '/images.jpg' ]
            def config = [ enabled: false, default: 'http://www.google.com/' ]
        when:
            mapper.map( resource, config )
        then:
            resource.linkOverride == null
    }

    def "a resource can set a unique url based on module name"(){
        setup:
            def resource = [ linkUrl : '/images.jpg', module: [ name: 'uno'] ]
            def config = [ enabled: true, default:'http://www.google.com/', modules : [ uno: 'http://uno.com/' ] ]
        when:
            mapper.map( resource, config )
        then:
            resource.linkOverride == 'http://uno.com/images.jpg'
    }

    def "a resource with no modules default to base url"(){
        setup:
            def resource = [ linkUrl : '/images.jpg', module: [ name: 'uno'] ]
            def config = [ enabled: true, default:'http://www.google.com/', modules : [ dos: 'http://dos.com/' ] ]
        when:
            mapper.map( resource, config )
        then:
            resource.linkOverride == 'http://www.google.com/images.jpg'
    }

}
