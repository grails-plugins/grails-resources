package org.grails.plugin.resource.mapper

import grails.spring.BeanBuilder
import org.grails.plugin.resources.artefacts.ResourceMapperClass
import java.lang.reflect.Modifier
import org.slf4j.LoggerFactory

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
                
                // We can't pass the specific config object for this mapper
                // because we don't know it's name until we have an instance.
                // The mapper extracts it's own config.
                "$name"(ResourceMapper, ref(instanceName), mappersConfig) 
                
                beanNames << name
            }
        }
        
        def ctx = bb.createApplicationContext()
        beanNames.collect { ctx.getBean(it) }.sort(PRIORITY_COMPARATOR)
    }
    
    static public final PRIORITY_COMPARATOR = [
        compare: { ResourceMapper lhs, ResourceMapper rhs -> 
            if (lhs == null || rhs == null) {
                throw new NullPointerException("compareTo() called with a null parameter")
            }
            
            lhs.priority <=> rhs.priority ?: lhs.name <=> rhs.name
        }
    ] as Comparator
}