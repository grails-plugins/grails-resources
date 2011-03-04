import org.grails.plugin.resource.*

/**
 * Bootstraps the plugin by loading the app resources from config
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
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