package org.grails.plugin.resource

import org.grails.plugin.resource.mapper.MapperPhase

/**
 * Mapper that applies an optional base url to resources, e.g. for putting content out to 
 * one or more pull CDNs
 * @author Tomas Lin
 * @since 1.2
 */
class BaseUrlResourceMapper {

    static priority = 0

    static phase = MapperPhase.ABSOLUTISATION

    def map(resource, config) {
        if (config.enabled) {
			def url

			if (resource.module?.name && config.modules[resource.module.name]) {
				url = config.modules[resource.module.name]
			}

			if (!url) {
				url = config.default
			}

			if (url) {
				if (url.endsWith('/')) {
					url = url[0..-2]
				}
				resource.linkOverride = url + resource.linkUrl
			}
		}
    }

}