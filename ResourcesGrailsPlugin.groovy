import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ResourcesGrailsPlugin {

    def version = "1.0-alpha10"
    def grailsVersion = "1.2 > *"
    def dependsOn = [logging:'1.0 > *']
    def loadAfter = ['logging']
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/views/index.gsp",
            "grails-app/controllers/**/*.groovy",
            "web-app/css/**/*.*",
            "web-app/js/**/*.*",
            "web-app/images/**/*.*"
    ]

    def author = "Marc Palmer"
    def authorEmail = "marc@grailsrocks.com"
    def title = "Resources"
    def description = 'HTML resource management enhancements to replace g.resource etc.'
    def documentation = "http://grails.org/plugin/resources"

    static DEFAULT_URI_PREFIX = 'static'
    static DEFAULT_ADHOC_PATTERNS = ["/images/*", "*.css", "*.js"].asImmutable()
    
    def getResourcesConfig() {
        ConfigurationHolder.config.grails.resources
    }
    
    def getUriPrefix() {
        def prf = resourcesConfig.uri.prefix
        prf instanceof String ? prf : DEFAULT_URI_PREFIX
    }
    
    def getAdHocPatterns() {
        def patterns = resourcesConfig.adhoc.patterns
        patterns instanceof List ? patterns : DEFAULT_ADHOC_PATTERNS
    }

    def doWithWebDescriptor = { webXml ->
        def adHocPatterns = getAdHocPatterns()
        
        log.info("Adding servlet filter")
        def filters = webXml.filter[0]
        filters + {
            'filter' {
                'filter-name'("DeclaredResourcesPluginFilter")
                'filter-class'("org.grails.plugin.resource.ProcessingFilter")
            }
            if (adHocPatterns) {
                'filter' {
                    'filter-name'("AdHocResourcesPluginFilter")
                    'filter-class'("org.grails.plugin.resource.ProcessingFilter")
                    'init-param' {
                        'param-name'("adhoc")
                        'param-value'("true")
                    }
                }
            }
        }
        def mappings = webXml.'filter-mapping' // this does only yield 2 filter mappings
        mappings + {
            'filter-mapping' {
                'filter-name'("DeclaredResourcesPluginFilter")
                'url-pattern'("/${uriPrefix}/*")
            }
            // To be pre-Servlets 2.5 safe, we have 1 extension mapping per filter-mapping entry
            // Lame, but Tomcat 5.5 is not SSDK 2.5
            adHocPatterns.each { pattern ->
                'filter-mapping' {
                    'filter-name'("AdHocResourcesPluginFilter")
                    'url-pattern'(pattern.toString())
                }
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        applicationContext.resourceService.staticUrlPrefix = "/${uriPrefix}"
    }

    def onChange = { event ->
        // @todo monitor the static resources mapped and flush mappings from cache
    }

    def onConfigChange = { event ->
        event.application.mainContext.resourceService.loadResourcesFromConfig()
    }
}
