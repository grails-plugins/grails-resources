package org.grails.plugin.resource.test

import org.grails.plugin.resource.mapper.MapperPhase

class TestResourceMapper {

    def phase = MapperPhase.MUTATION

    def map(resource, config) {
        def file = new File(resource.processedFile.parentFile, "_${resource.processedFile.name}")
        assert resource.processedFile.renameTo(file)
        resource.processedFile = file
        resource.updateActualUrlFromProcessedFile()
    }

}