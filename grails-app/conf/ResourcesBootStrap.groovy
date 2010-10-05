import org.grails.plugin.resource.CSSRewriter

/**
 * Bootstraps the plugin by loading the app resources from config
 */

class ResourcesBootStrap {
 
    def resourceService
    
    def init = { servletContext ->
        // Add default mappers
        resourceService.addResourceMapper("cssrewrite", CSSRewriter.mapper, 500)
        resourceService.addResourceMapper("bundle", ResourceBundler.mapper, 100)
        
        resourceService.loadResourcesFromConfig()
    }
    
    def destroy = {
        
    }
}