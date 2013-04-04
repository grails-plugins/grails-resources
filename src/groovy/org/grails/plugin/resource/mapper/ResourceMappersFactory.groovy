package org.grails.plugin.resource.mapper

import grails.spring.BeanBuilder

import java.lang.reflect.Modifier

import org.grails.plugin.resources.artefacts.ResourceMapperClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This creates the ResourceMapper facades for each resource mapper artefact and puts them into the bean context
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceMappersFactory {

    static Logger log = LoggerFactory.getLogger(this)

    static List<ResourceMapper> createResourceMappers(grailsApplication, mappersConfig) {

        // filter out abstracts
        List<ResourceMapperClass> resourceMapperClasses = grailsApplication.resourceMapperClasses.findAll { ResourceMapperClass rm ->
            !Modifier.isAbstract(rm.clazz.modifiers)
        }

        // Create an application context to allow the mapper instances to be autowired
        def beanNames = []
        def bb = new BeanBuilder(grailsApplication.mainContext, grailsApplication.classLoader)
        bb.beans {
            for (ResourceMapperClass resourceMapperClass in resourceMapperClasses) {
                String name = resourceMapperClass.fullName
                String instanceName = "${name}Instance"

                "$instanceName"(resourceMapperClass.clazz) {
                    it.autowire = true
                }

                "$name"(ResourceMapper, ref(instanceName), mappersConfig)

                beanNames << name
            }
        }

        def ctx = bb.createApplicationContext()
        List<ResourceMapper> mapperOrdering = beanNames.collect { ctx.getBean(it) }.sort { ResourceMapper lhs, ResourceMapper rhs ->
            if (lhs == null || rhs == null) {
                throw new NullPointerException("compareTo() called with a null parameter")
            }

            int phaseComp = lhs.phase <=> rhs.phase
            if (phaseComp != 0) {
                return phaseComp
            }
            // Same phase, compare priorities, fall back to name (arbitrary but makes order reliable with dupe priorities)
            return lhs.priority <=> rhs.priority ?: lhs.name <=> rhs.name
        }

        def operations = mapperOrdering.operation
        for (ResourceMapper m in mapperOrdering) {
            if (m.name in operations) {
                throw new IllegalArgumentException(
                    "The mapper ${m.name} is not valid because there is an operation with the same name. Please change the name of the mapper.")
            }
        }

        // Let's throw people a bone with some nice debug
        if (log.debugEnabled) {
            def s = new StringBuilder()
            def phase
            for (ResourceMapper m in mapperOrdering) {
                if (m.phase != phase) {
                    s << "Phase: ${m.phase}\n"
                    phase = m.phase
                }
                s << "  ${m.priority ?: 0}: ${m.name}\n"
            }
            log.debug "Resource mappers will be run in the following order:\n$s"
        }
        return mapperOrdering
    }
}
