import org.grails.plugin.resource.*

/**
 * Bootstraps the plugin by loading the app resources from config
 */

class ResourcesBootStrap {
 
    def resourceService
    
    def init = { servletContext ->
        // Add default mappers
        resourceService.addResourceMapper("csspreprocess", CSSPreprocessor.mapper, 100)
        resourceService.addResourceMapper("bundle", ResourceBundler.mapper, 150)
        // >>>>>>>>>>>> In here goes caching
        resourceService.addResourceMapper("cssrewrite", CSSRewriter.mapper, 500)
        // >>>>>>>>>>>> In here goes zipping
        
        resourceService.loadResourcesFromConfig()
    }
    
    def destroy = {
        
    }
}