import grails.util.Environment

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.core.io.FileSystemResource

import org.grails.plugin.resource.util.HalfBakedLegacyLinkGenerator

/**
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourcesGrailsPlugin {

    def version = "1.1.BUILD-SNAPSHOT"
    def grailsVersion = "1.2 > *"

    def loadAfter = ['logging'] // retained to ensure correct loading under Grails < 2.0

    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/views/index.gsp",
            "grails-app/controllers/**/*.groovy",
            "web-app/css/**/*.*",
            "web-app/js/**/*.*",
            "web-app/images/**/*.*",
            "grails-app/resourceMappers/**/test/*",
            "grails-app/conf/*Resources.groovy"
    ]

    def artefacts = [getResourceMapperArtefactHandler(), getResourcesArtefactHandler()]
    def watchedResources = [
        "file:./grails-app/resourceMappers/**/*.groovy",
        "file:./plugins/*/grails-app/resourceMappers/**/*.groovy",
        "file:./grails-app/conf/*Resources.groovy",
        "file:./plugins/*/grails-app/conf/*Resources.groovy",
        "file:./web-app/**/*.*" // Watch for resource changes, we need excludes here for WEB-INF+META-INF when grails impls this
    ]

    def author = "Marc Palmer, Luke Daley"
    def authorEmail = "marc@grailsrocks.com, ld@ldaley.com"
    def title = "Resources"
    def description = 'HTML resource management enhancements to replace g.resource etc.'
    def documentation = "http://grails.org/plugin/resources"

    static DEFAULT_URI_PREFIX = 'static'
    static DEFAULT_ADHOC_PATTERNS = ["/images/*", "*.css", "*.js"].asImmutable()
    
    def getResourcesConfig(application) {
        application.config.grails.resources
    }
    
    def getUriPrefix(application) {
        def prf = getResourcesConfig(application).uri.prefix
        prf instanceof String ? prf : DEFAULT_URI_PREFIX
    }
    
    def getAdHocPatterns(application) {
        def patterns = getResourcesConfig(application).adhoc.patterns
        patterns instanceof List ? patterns : DEFAULT_ADHOC_PATTERNS
    }
    
    def doWithSpring = { ->
        if (!springConfig.containsBean('grailsLinkGenerator')) {
            grailsLinkGenerator(HalfBakedLegacyLinkGenerator) {
                pluginManager = ref('pluginManager')
            }
        }
    }
    
    def doWithWebDescriptor = { webXml ->
        def adHocPatterns = getAdHocPatterns(application)

        def declaredResFilter = [   
                name:'DeclaredResourcesPluginFilter', 
                filterClass:"org.grails.plugin.resource.ProcessingFilter",
                urlPatterns:["/${getUriPrefix(application)}/*"]
        ]
        def adHocFilter = [   
            name:'AdHocResourcesPluginFilter', 
            filterClass:"org.grails.plugin.resource.ProcessingFilter",
            params: [adhoc:true],
            urlPatterns: adHocPatterns
        ]

        def filtersToAdd = [declaredResFilter]
        if (adHocPatterns) {
            filtersToAdd << adHocFilter
        }

        if ( Environment.current == Environment.DEVELOPMENT) {
            filtersToAdd << [   
                name:'ResourcesDevModeFilter', 
                filterClass:"org.grails.plugin.resource.DevModeSanityFilter",
                urlPatterns:['/*']
            ]
        }
        
        log.info("Adding servlet filters")
        def filters = webXml.filter[0]
        filters + {
            filtersToAdd.each { f ->
                log.info "Adding filter: ${f.name} with class ${f.filterClass} and init-params: ${f.params}"
                'filter' {
                    'filter-name'(f.name)
                    'filter-class'(f.filterClass)
                    f.params?.each { k, v ->
                        'init-param' {
                            'param-name'(k)
                            'param-value'(v.toString())
                        }
                    }
                }
            }
        }
        def mappings = webXml.'filter-mapping'[0] 
        mappings + {
            filtersToAdd.each { f ->
                f.urlPatterns?.each { p ->
                    log.info "Adding url pattern ${p} for filter ${f.name}"
                    'filter-mapping' {
                        'filter-name'(f.name)
                        'url-pattern'(p)
                    }
                }
            }
        }
    }

    def doWithDynamicMethods = { applicationContext ->
        applicationContext.resourceService.staticUrlPrefix = "/${getUriPrefix(application)}"
        applicationContext.resourceService.reload()
    }

    boolean isResourceWeShouldProcess(File file) {
        // @todo Improve this, but for now tracing the ancestry of every file is seriously zzzz and overkill
        // when STS creats 100s of .class changes
        boolean shouldProcess = (file.parent.indexOf('WEB-INF') < 0) && (file.parent.indexOf('META-INF') < 0)
        return shouldProcess
    }
    
    def onChange = { event ->
        if (event.source instanceof FileSystemResource) {
            if (isResourceWeShouldProcess(event.source.file)) {
                event.application.mainContext.resourceService.reload()
            }
        } else {
            [getResourceMapperArtefactHandler().TYPE, getResourcesArtefactHandler().TYPE].each {
                if (handleChange(application, event, it, log)) {
                    log.info("reloading resources due to change of $event.source.name")
                    event.application.mainContext.resourceService.reload()
                }
            }
        }
    }

    protected handleChange(application, event, type, log) {
        if (application.isArtefactOfType(type, event.source)) {
            log.debug("reloading $event.source.name ($type)")
            def oldClass = application.getArtefact(type, event.source.name)
            application.addArtefact(type, event.source)
            // Reload subclasses
            application.getArtefacts(type).each {
                if (it.clazz != event.source && oldClass.clazz.isAssignableFrom(it.clazz)) {
                    def newClass = application.classLoader.reloadClass(it.clazz.name)
                    application.addArtefact(type, newClass)
                }
            }
            
            true
        } else {
            false
        }
    }

    def onConfigChange = { event ->
        event.application.mainContext.resourceService.reload()
    }

    /**
     * We have to soft load this class so this file can be compiled on it's own.
     */
    static getResourceMapperArtefactHandler() {
        softLoadClass('org.grails.plugin.resources.artefacts.ResourceMapperArtefactHandler')
    }

    static getResourcesArtefactHandler() {
        softLoadClass('org.grails.plugin.resources.artefacts.ResourcesArtefactHandler')
    }

    static softLoadClass(String className) {
        try {
            getClassLoader().loadClass(className)
        } catch (ClassNotFoundException e) {
            null
        }
    }
}
