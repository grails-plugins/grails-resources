package org.grails.plugin.resource.test

class TestResourceMapper {

    def priority = Integer.MAX_VALUE
    
    def map(resource, config) {
        def file = new File(resource.processedFile.parentFile, "_${resource.processedFile.name}")
        assert resource.processedFile.renameTo(file)
        resource.processedFile = file
        resource.updateActualUrlFromProcessedFile()
    }

}