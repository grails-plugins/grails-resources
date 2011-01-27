import org.grails.plugin.resource.*

/**
 * Bootstraps the plugin by loading the app resources from config
 */

class ResourcesBootStrap {
 
    def resourceService
    
    def init = { servletContext ->
        resourceService.reload()
        
        resourceService.dumpResources()
    }
    
    def destroy = {
        
    }
}