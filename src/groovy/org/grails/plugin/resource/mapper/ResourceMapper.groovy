package org.grails.plugin.resource.mapper

import org.codehaus.groovy.grails.*
import org.grails.plugin.resources.artefacts.ResourceMapperArtefactHandler
import org.grails.plugin.resource.ResourceMeta
import org.springframework.util.AntPathMatcher
import org.slf4j.LoggerFactory
import grails.util.GrailsNameUtils

class ResourceMapper {

    static public final DEFAULT_PRIORITY = 0
    static public final PATH_MATCHER = new AntPathMatcher()
    
    final artefact
    final config
    final log
    
    @Lazy priority = {
        try {
            artefact.priority
        } catch (MissingPropertyException e) {
            DEFAULT_PRIORITY
        }
    }()
    
    @Lazy name = {
        try {
            artefact.name
        } catch (MissingPropertyException e) {
            GrailsNameUtils.getLogicalName(artefact.class, ResourceMapperArtefactHandler.SUFFIX).toLowerCase()
        }
    }()
    
    @Lazy defaultExcludes = {
        try {
            toStringList(artefact.defaultExcludes)
        } catch (MissingPropertyException e) {
            []
        }
    }()

    @Lazy excludes = {
        if (config.excludes) {
            toStringList(config.excludes) + defaultExcludes
        } else {
            defaultExcludes
        }
    }()
    
    /**
     * @param artefact an instance of the resource mapper artefact
     * @param mappersConfig the config object that is the config for all mappers
     *                      this object is responsible for getting the specific
     *                      config object for this mapper
     */
    ResourceMapper(artefact, mappersConfig) {
        this.artefact = artefact
        this.config = mappersConfig[getName()]
        
        log = LoggerFactory.getLogger('org.grails.plugin.resource.mapper.' + getName())
        artefact.metaClass.getLog = { it }.curry(log)
    }

    boolean invokeIfNotExcluded(ResourceMeta resource) {
        def excludingPattern = getExcludingPattern(resource)
        if (excludingPattern) {
            if (log.debugEnabled) {
                log.debug "skipping ${resource.sourceUrl} due to excludes pattern ${excludes}"
            }
            
            false
        } else if (resource.excludesMapper(name)) {
            if (log.debugEnabled) {
                log.debug "skipping ${resource.sourceUrl} due definition excluding mapper"
            }
            
            false
        } else {
            invoke(resource)
            true
        }
    }
    
    private invoke(ResourceMeta resource) {
        if (log.debugEnabled) {
            log.debug "beginning mapping ${resource.dump()}"
        }
        
        try {
            artefact.map(resource, config)
        } catch (MissingMethodException e) {
            if (artefact.class == e.type && e.method == "map") {
                throw new Exception("the resource mapper '$name' does not implement the appropriate map method")
            }
        }
        
        if (log.debugEnabled) {
            log.debug "done mapping ${resource.dump()}"
        }
    }
    
    String getExcludingPattern(ResourceMeta resource) {
        excludes.find { PATH_MATCHER.match(it, resource.sourceUrl) }
    }
    
    private toStringList(value) {
        value instanceof Collection ? value*.toString() : value.toString()
    }
}