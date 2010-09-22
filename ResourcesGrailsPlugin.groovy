import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ResourcesGrailsPlugin {
    // the plugin version
    def version = "1.0-alpha2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    // the other plugins this plugin depends on
    def dependsOn = [logging:'1.0 > *']
    
    def loadAfter = ['logging']
    
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/views/index.gsp",
            "web-app/css/**/*.*",
            "web-app/js/**/*.*"
    ]

    // TODO Fill in these fields
    def author = "Marc Palmer"
    def authorEmail = "marc@grailsrocks.com"
    def title = "Resources"
    def description = '''\\
HTML resource management enhancements to replace g.resource etc.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/resources"

    static DEFAULT_ADHOC_EXTENSIONS = ['css','js','gif','jpg','png']
    
    def getConfigUriPrefix = {
        def config = ConfigurationHolder.config.grails.resources
        def prf = config.uri.prefix
        if (!(prf instanceof String)) {
            prf = 'static'
        }
        return prf
    }
    
    def getConfigAdHocExtensions = {
        def config = ConfigurationHolder.config.grails.resources
        def prf = config.adhoc.extensions
        if (!(prf instanceof List)) {
            prf = DEFAULT_ADHOC_EXTENSIONS
        }
        return prf
    }

    def doWithWebDescriptor = { webXml ->
        
        def prf = getConfigUriPrefix()
        def adHocFileExtensions = getConfigAdHocExtensions()
        
		log.info("Adding servlet filter")
		def filters = webXml.filter[0]
	    filters + {
			'filter' {
				'filter-name'("DeclaredResourcesPluginFilter")
				'filter-class'("org.grails.plugin.resource.ProcessingFilter")
			}
			'filter' {
				'filter-name'("AdHocResourcesPluginFilter")
				'filter-class'("org.grails.plugin.resource.ProcessingFilter")
				'init-param' {
				    'param-name'("adhoc")
				    'param-value'("true")
				}
			}
      	}
		def mappings = webXml.'filter-mapping' // this does only yield 2 filter mappings
		mappings + {
			'filter-mapping' {
			    'filter-name'("DeclaredResourcesPluginFilter")
			    'url-pattern'("/${prf}/*")
	    	}
            // To be pre-Servlets 2.5 safe, we have 1 extension mapping per filter-mapping entry
            // Lame, but Tomcat 5.5 is not SSDK 2.5
			adHocFileExtensions.each { ext ->
    			'filter-mapping' {
    			    'filter-name'("AdHocResourcesPluginFilter")
    			    'url-pattern'("*.${ext}")
    	    	}
	    	}
      	}
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { applicationContext ->
        def prf = getConfigUriPrefix()
        
        applicationContext.resourceService.staticUrlPrefix = '/'+prf
    }

    def onChange = { event ->
        // @todo monitor the static resources mapped and flush mappings from cache
    }

    def onConfigChange = { event ->
        event.application.mainContext.resourceService.loadResourcesFromConfig()
    }
}
