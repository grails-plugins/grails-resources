package org.grails.plugin.resource.mapper

import grails.util.GrailsNameUtils

import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resources.artefacts.ResourceMapperArtefactHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

/**
 * The artefact facade used by the service to communicate with resource mapper artefacts.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceMapper {

    static final int DEFAULT_PRIORITY = 0
    static final AntPathMatcher PATH_MATCHER = new AntPathMatcher()

    final artefact
    final config
    private Logger log

    @Lazy phase = {
        try { artefact.phase }
        catch (MissingPropertyException e) {
            throw new IllegalArgumentException("Resource mapper $name must have a phase property defined")
        }
    }()

    @Lazy operation = {
        try { artefact.operation }
        catch (MissingPropertyException e) { null }
    }()

    @Lazy priority = {
        try { artefact.priority }
        catch (MissingPropertyException e) { DEFAULT_PRIORITY }
    }()

    @Lazy name = {
        try { artefact.name }
        catch (MissingPropertyException e) {
            GrailsNameUtils.getLogicalName(artefact.getClass(), ResourceMapperArtefactHandler.SUFFIX).toLowerCase()
        }
    }()

    @Lazy defaultExcludes = {
        try { toStringList(artefact.defaultExcludes) }
        catch (MissingPropertyException e) { [] }
    }()

    @Lazy defaultIncludes = {
        try { toStringList(artefact.defaultIncludes) }
        catch (MissingPropertyException e) { ['**/*'] }
    }()

    @Lazy excludes = {
        config?.excludes ? toStringList(config.excludes) : defaultExcludes
    }()

    @Lazy includes = {
        config?.includes ? toStringList(config.includes) : defaultIncludes
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

        // @todo why are we doing this, why isn't logging plugin doing it?
        // Even though we load after logging, it seems to not apply it to our artefacts
        log = LoggerFactory.getLogger('org.grails.plugin.resource.mapper.' + getName())
        artefact.metaClass.getLog = { it }.curry(log)
    }

    boolean invokeIfNotExcluded(ResourceMeta resource) {
        def includingPattern = getIncludingPattern(resource)
        def excludingPattern = getExcludingPattern(resource)
        if (!includingPattern) {
            if (log.debugEnabled) {
                log.debug "Skipping $resource.sourceUrl due to includes pattern $includes not including it"
            }
            return false
        }

        if (excludingPattern) {
            if (log.debugEnabled) {
                log.debug "Skipping $resource.sourceUrl due to excludes pattern $excludes"
            }
            return false
        }

        if (resource.excludesMapperOrOperation(name, operation)) {
            if (log.debugEnabled) {
                log.debug "Skipping $resource.sourceUrl due to definition excluding mapper"
            }
            return false
        }

        invoke(resource)
        true
    }

    private invoke(ResourceMeta resource) {
        if (log.debugEnabled) {
            log.debug "Beginning mapping ${resource.dump()}"
        }

        try {
            artefact.map(resource, config)
        } catch (MissingMethodException e) {
            if (artefact.getClass() == e.type && e.method == "map") {
                throw new Exception("The resource mapper '$name' does not implement the appropriate map method")
            }
            throw e
        }

        if (log.debugEnabled) {
            log.debug "Done mapping ${resource.dump()}"
        }
    }

    String stripLeadingSlash(String s) {
        s.startsWith("/") ? s.substring(1) : s
    }

    String getExcludingPattern(ResourceMeta resource) {
        // The path matcher won't match **/* against a path starting with /, so it makes sense to remove it.
        String sourceUrl = stripLeadingSlash(resource.sourceUrl)
        excludes.find { PATH_MATCHER.match(it, sourceUrl) }
    }

    String getIncludingPattern(ResourceMeta resource) {
        String sourceUrl = stripLeadingSlash(resource.sourceUrl)
        includes.find { PATH_MATCHER.match(it, sourceUrl) }
    }

    private toStringList(value) {
        value instanceof Collection ? value*.toString() : value.toString()
    }
}
