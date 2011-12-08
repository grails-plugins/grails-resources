package org.grails.plugin.resource.mapper

import org.grails.plugin.resource.mapper.MapperPhase

class BaseUrlResourceMapper {

    static priority = 0
    static phase = MapperPhase.ABSOLUTISATION

    def map(resource, config) {

        println 'BaseUrlResourceMapper' + config


        if( config.enabled ){

			def url

            println config

			if( resource.module?.name && config.moduleUrls[ resource.module.name ] ){
				url = config.moduleUrls[ resource.module.name ]
			}

			if( !url ){
				url = config.baseUrl
			}

			if( url ){
				if( url.endsWith('/') ){
					url = url[0..-2]
				}
				resource.linkOverride = url + resource.linkUrl
			}
		}

    }

}