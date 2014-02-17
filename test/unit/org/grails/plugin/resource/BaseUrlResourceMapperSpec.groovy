package org.grails.plugin.resource

import spock.lang.Specification

class BaseUrlResourceMapperSpec extends Specification {

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

    //GPRESOURCES-184
    def "mapper uses delegate-resource's name for aggreagated resources"() {
        setup:
        def resourceBundle = [getLinkUrl: { '/bundle.js' }] as AggregatedResourceMeta
        resourceBundle.resources = [bundledResource('uno')]
        def config = [ enabled: true, default:'http://www.google.com/', modules : [ uno: 'http://uno.com/' ] ]

        when:
        mapper.map(resourceBundle, config)

        then:
        resourceBundle.linkOverride == 'http://uno.com/bundle.js'
    }

    //GPRESOURCES-184
    def "mapper throws an exception when configured to map modules bundled together to different urls"() {
        setup:
        def resourceBundle = [getLinkUrl: { '/bundle.js' }] as AggregatedResourceMeta
        resourceBundle.resources = [bundledResource('uno'), bundledResource('dos')]
        def config = [ enabled: true, default:'http://www.google.com/', modules : [ uno: 'http://uno.com/' ] ]

        when:
        mapper.map(resourceBundle, config)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains('All modules bundled together must have the same baseUrl override')
        exception.message.contains(resourceBundle.resources.first().bundle)
    }
    
	//GPRESOURCES-16
	def "mapper uses rotating baseUrl but only when config.default configured as a list"() {
		setup:
		def resource = [ linkUrl : '/images.jpg' ]
		def config = [ enabled: true, default: 'http://www.google.com/' ]

		when:
		mapper.map( resource, config )

		then:
		resource.linkOverride == 'http://www.google.com/images.jpg'
	}
	
	def "mapper uses rotating baseUrl but only when config.default configured as a non-empty list"() {
		setup:
		def resource = [ linkUrl : '/images.jpg' ]
		def config = [ enabled: true, default: [] ]

		when:
		mapper.map( resource, config )

		then:
		resource.linkOverride == null
	}
	
	def "mapper uses rotating baseUrl when baseUrl configured as a list"() {
		setup:
		def resourceOne = [ linkUrl : '/images.jpg' ]
		def resourceTwo = [ linkUrl : '/images.png' ]
		def resourceThree = [ linkUrl : '/images.gif' ]
		def config = [ enabled: true, default: ['http://cdn1.google.com/', 'http://cdn2.google.com/', 'http://cdn3.google.com/']]

		when:
		mapper.map( resourceOne, config )
		mapper.map( resourceTwo, config )
		mapper.map( resourceThree, config )
		
		then:
		resourceOne.linkOverride == 'http://cdn3.google.com/images.jpg'
		resourceTwo.linkOverride == 'http://cdn3.google.com/images.png'
		resourceThree.linkOverride == 'http://cdn2.google.com/images.gif'
	}
	//GPRESOURCES-16

    private ResourceMeta bundledResource(String moduleName) {
        def module = [name: moduleName] as ResourceModule
        [module: module, bundle: 'bundle_head'] as ResourceMeta
    }

}
