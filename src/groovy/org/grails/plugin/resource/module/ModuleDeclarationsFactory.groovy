package org.grails.plugin.resource.module

import grails.util.Environment

import org.grails.plugin.resources.artefacts.ResourcesClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This factory collects up all the application and plugin resource module declarations
 *
 * @author Luke Daley (ld@ldaley.com)
 */
class ModuleDeclarationsFactory {

    private static Logger log = LoggerFactory.getLogger(this)

    /**
     * Gathers all of the module declarations (i.e. DSL closures) present in the application.
     *
     * The closures are returned ordered by the name of the script class that defined them, except
     * for the last item being from the application config if a declaration was present there. They are
     * keyed by the name of the class that defined them.
     *
     * Scripts are parsed with environment sensitivity.
     */
    static Map<String,Closure> getModuleDeclarations(grailsApplication, String environment = Environment.current.name) {

        ConfigSlurper slurper = new ConfigSlurper(environment)

        // give module DSL closures access to grailsApplication
        slurper.setBinding([grailsApplication: grailsApplication])

        // order so we are guaranteed of consistency
        List<ResourcesClass> orderedResourceClasses = grailsApplication.resourcesClasses.sort { it.name }

        if (log.debugEnabled) {
            log.debug("resource config order: ${orderedResourceClasses*.clazz*.name}")
        }

        def moduleDeclarations = [:]

        // gather all the module declarations
        for (ResourcesClass rc in orderedResourceClasses) {
            if (log.debugEnabled) {
                log.debug("consuming resources config from $rc.clazz.name")
            }

            def modules = slurper.parse(rc.clazz).modules
            if (modules instanceof Closure) {
                moduleDeclarations[rc.clazz.name] = modules
            } else {
                if (modules instanceof ConfigObject) {
                    log.warn("resources artefact $rc.clazz.name does not define any modules")
                } else {
                    log.warn("resources artefact $rc.clazz.name mapper element is not a Closure")
                }
            }
        }

        moduleDeclarations.findAll { it != null }
    }

    static Closure getApplicationConfigDeclarations(grailsApplication, String environment = Environment.current.name) {
        // get the modules from app config (last so they take precedence)
        def appModuleDeclarations = grailsApplication.config.grails.resources.modules
        if (appModuleDeclarations instanceof Closure) {
            return appModuleDeclarations
        }

        if (appModuleDeclarations instanceof ConfigObject) {
            log.warn("'grails.resources.modules' in config does not define any modules")
        } else {
            log.warn("'grails.resources.modules' in config is not a Closure")
        }
    }
}
