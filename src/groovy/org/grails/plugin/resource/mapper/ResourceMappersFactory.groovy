package org.grails.plugin.resource.mapper

import grails.spring.BeanBuilder
import org.grails.plugin.resources.artefacts.ResourceMapperClass
import java.lang.reflect.Modifier
import org.slf4j.LoggerFactory

/**
 * This creates the ResourceMapper facades for each resource mapper artefact and puts them into the bean context
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceMappersFactory {

    static List<ResourceMapper> createResourceMappers(grailsApplication, mappersConfig) {
        
        def log = LoggerFactory.getLogger('org.grails.plugin.resource.mapper.ResourceMappersFactory')
        
        // filter out abstracts
        def resourceMapperClasses = grailsApplication.resourceMapperClasses.findAll {
            !Modifier.isAbstract(it.clazz.modifiers)
        }
        
        // Create an application context to allow the mapper instances to be autowired
        def beanNames = []
        def bb = new BeanBuilder(grailsApplication.mainContext, grailsApplication.classLoader)
        bb.beans {
            for (resourceMapperClass in resourceMapperClasses) {
                def name = resourceMapperClass.fullName
                def instanceName = "${name}Instance"
                
                "$instanceName"(resourceMapperClass.clazz) { 
                    it.autowire = true 
                }
                
                "$name"(ResourceMapper, ref(instanceName), mappersConfig) 
                
                beanNames << name
            }
        }
        
        def ctx = bb.createApplicationContext()
        beanNames.collect { ctx.getBean(it) }.sort(PHASE_PRIORITY_COMPARATOR)
    }
    
    static public final PHASE_PRIORITY_COMPARATOR = [
        compare: { ResourceMapper lhs, ResourceMapper rhs -> 
            if (lhs == null || rhs == null) {
                throw new NullPointerException("compareTo() called with a null parameter")
            }
            
            def phaseComp = lhs.phase <=> rhs.phase
            if (phaseComp != 0) {
                return phaseComp
            } else {
                // Same phase, compare priorities, fall back to name (arbitrary but makes order reliable with dupe priorities)
                return lhs.priority <=> rhs.priority ?: lhs.name <=> rhs.name
            }
        }
    ] as Comparator
}