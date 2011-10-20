package org.grails.plugin.resource

import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.apache.commons.io.FilenameUtils

import org.grails.plugin.resource.mapper.ResourceMapper

/**
 * Holder for info about a resource declaration at runtime
 *
 * This is actually non-trivial. A lot of data kept here. Be wary of what you think a "url" is. 
 * See the javadocs for each URL property.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
class ResourceMeta {

    def log = LoggerFactory.getLogger(ResourceMeta)

    /**
     * The optional module-unique id
     */
    String id
    
    /**
     * The owning module
     */
    ResourceModule module
    
    /**
     * Set on instantiation to be the dir that content is served from
     * 
     * @see ResourceProcessor#workDir
     */
    File workDir

    /**
     * The original Url provided in the mapping declaration, verbatim
     */
    String originalUrl

    /**
     * The app-relative url of the LOCAL source of this resource, minus query params
     */
    String sourceUrl

    /**
     * The original file extension of the resource
     */
    String sourceUrlExtension

    /**
     * The original sourceUrlParamsAndFragment of the resource, if any
     */
    String sourceUrlParamsAndFragment
    
    /**
     * The url of the local resource, after processing. (no query params)
     */
    String actualUrl
    
    /**
     * The url to use when rendering links - e.g. for absolute CDN overrides
     */
    String linkOverride
    
    String bundle
    
    /**
     * The original mime type
     */
    String contentType
    
    /**
     * Where do you want this resource? "defer", "head" etc
     */
    String disposition

    //@todo impl this later
    //String cachedTagText
    
    /**
     * The delegate to actually use when linking, if any. Think bundling.
     */
    ResourceMeta delegate
    
    void delegateTo(ResourceMeta target) {
        delegate = target
    }
    
    Resource originalResource
    
    File processedFile
    
    long originalLastMod
    
    // For per-resource options like "nominify", 'nozip'
    Map attributes = [:]
    
    // For per-resource tag resource attributes like "media", 'width', 'height' etc
    Map tagAttributes = [:]

    Closure prePostWrapper

    // A list of Closures taking request & response. Delegates to resourceMeta
    List requestProcessors = []
    
    private String _linkUrl
    
    private Boolean _resourceExists
    
    /**
     * The URI of the resource that resulted in the processing of this resource, or null
     * For resources ref'd in CSS or stuff loaded up by bundles for example
     */
    String declaringResource
    
    Integer contentLength
    
    Integer originalContentLength = 0

    Set excludedMappers
    
    boolean isOriginalAbsolute() {
        sourceUrl.indexOf(':/') > 0
    }
    
    void updateContentLength() {
        if (processedFile) {
            this.@contentLength = processedFile.size().toInteger()
        } else if (originalResource?.URL.protocol in ['jndi', 'file']) {
            this.@contentLength = originalResource?.URL.openConnection().contentLength        
        } else {
            this.@contentLength = 0
        }
    }

    void setOriginalResource(Resource res) {
        this.originalResource = res
        updateExists()
        this.originalContentLength = originalResource?.URL.openConnection().contentLength        
        updateContentLength()
    }
    
    void setProcessedFile(File f) {
        this.processedFile = f
        updateExists()
        updateContentLength()
    }

    void updateExists() {
        if (processedFile) {
            _resourceExists = processedFile.exists()
            if (!this.originalLastMod && _resourceExists) {
                this.originalLastMod = processedFile.lastModified()
            }
        } else if (originalResource) {
            _resourceExists = originalResource.exists()            
            if (!this.originalLastMod && _resourceExists) {
                this.originalLastMod = originalResource.lastModified()
            }
        }
    }
    private void copyOriginalResourceToWorkArea() {
        def inputStream = this.originalResource.inputStream
        try {
            // Now copy in the resource from this app deployment into the cache, ready for mutation
            this.processedFile << inputStream
            _resourceExists = this.processedFile.exists()
        } finally {
            inputStream?.close()                    
        }
    }

    /**
     * Return a new input stream for serving the resource - if processing is disabled 
     * the processedFile will be null and the original resource is used
     */
    InputStream newInputStream() {
        return processedFile ? processedFile.newInputStream() : originalResource.inputStream
    }
    
    // Hook for when preparation is starting
    void beginPrepare(grailsResourceProcessor) {
        def uri = this.sourceUrl
        if (!uri.contains('://')) {

            if (!exists()) {
        		def uriWithoutFragment = uri
        		if (uri.contains('#')) {
        			uriWithoutFragment = uri.substring(0, uri.indexOf('#'))
        		}

                def origResourceURL = grailsResourceProcessor.getOriginalResourceURLForURI(uriWithoutFragment)
                if (!origResourceURL) {
                    if (log.errorEnabled) {
                        if (this.declaringResource) {
                            log.error "While processing ${this.declaringResource}, a resource was required but not found: ${uriWithoutFragment}"
                        } else {
                            log.error "Resource not found: ${uriWithoutFragment}"
                        }
                    }
                    throw new FileNotFoundException("Cannot locate resource [$uri]")
                }

                this.contentType = grailsResourceProcessor.getMimeType(uriWithoutFragment)
                if (log.debugEnabled) {
                    log.debug "Resource [$uriWithoutFragment] ($origResourceURL) has content type [${this.contentType}]"
                }

                setOriginalResource(new UrlResource(origResourceURL))

                if (grailsResourceProcessor.processingEnabled) {
                    setActualUrl(uriWithoutFragment)

                    setProcessedFile(grailsResourceProcessor.makeFileForURI(uriWithoutFragment))
                    // copy the file ready for mutation
                    this.copyOriginalResourceToWorkArea()
                } else {
                    setActualUrl(uriWithoutFragment)
                }
            }

        } else {
            setOriginalResource(new UrlResource(this.sourceUrl))
            setActualUrl(this.sourceUrl)

            log.warn "Skipping mappers for ${this.actualUrl} because its an absolute URL."
        }
    }
    
    // Hook for when preparation is done
    void endPrepare(grailsResourceProcessor) {
        if (!delegating) {
            if (processedFile) {
                processedFile.setLastModified(originalLastMod ?: System.currentTimeMillis() )
            }
        }
        updateContentLength()
        updateExists()
    }
    
    boolean isDelegating() {
        delegate != null
    }
    
    boolean exists() {
        _resourceExists
    }
    
    String getLinkUrl() {
        if (!delegate) {
            return linkOverride ?: _linkUrl 
        } else {
            return delegate.linkUrl
        }
    }
    
    String getActualUrl() {
        if (!delegate) {
            return this.@actualUrl 
        } else {
            return delegate.actualUrl
        }
    }

    void setActualUrl(String url) {
        this.@actualUrl = url
        _linkUrl = sourceUrlParamsAndFragment ? actualUrl + sourceUrlParamsAndFragment : url
    }
    
    
    void setSourceUrl(String url) {
        if (this.@originalUrl == null) {
            this.@originalUrl = url // the full monty
        }
        
        def qidx = url.indexOf('?')
        def hidx = url.indexOf('#')

        def chopIdx = -1
        // if there's hash we chop there, it comes before query
        if (hidx >= 0 && qidx < 0) {
            chopIdx = hidx
        }
        // if query params, we chop there even if have hash. Hash is after query params
        if (qidx >= 0) {
            chopIdx = qidx
        }

        sourceUrl = chopIdx >= 0 ? url[0..chopIdx-1] : url

        // Strictly speaking this is query params plus fragment ...
        sourceUrlParamsAndFragment = chopIdx >= 0 ? url[chopIdx..-1] : null
        
        sourceUrlExtension = FilenameUtils.getExtension(sourceUrl) ?: null
    }

    /**
     * The file extension of the processedFile, or null if it has no extension.
     */
    String getProcessedFileExtension() {
        if (processedFile) {
            FilenameUtils.getExtension(processedFile.name) ?: null
        }
    }
    
    String getWorkDirRelativeParentPath() {
        workDirRelativePath - "$processedFile.name"
    }
    
    String getWorkDirRelativePath() {
        processedFile.path - workDir.path
    }
    
    String getActualUrlParent() {
        def lastSlash = actualUrl.lastIndexOf('/')
        if (lastSlash >= 0) {
            return actualUrl[0..lastSlash-1]
        } else {
            return ''
        }
    }
    
    String relativeToWithQueryParams(ResourceMeta base) {
        def url = relativeTo(base)
        return sourceUrlParamsAndFragment ? url + sourceUrlParamsAndFragment : url
    }
    
    String relativeTo(ResourceMeta base) {
        def baseDirStr = base.actualUrlParent
        def thisDirStr = this.actualUrlParent
        boolean isChild = thisDirStr.startsWith(baseDirStr)
        if (isChild) {
            // Truncate to the part that is after the base dir
            return this.actualUrl[baseDirStr.size()+1..-1]
        } else {
            def result = new StringBuilder()

            def commonStem = new StringBuilder()
            def baseUrl = base.actualUrl
            // Eliminate the common portion - the base to which we need to ".."
            def baseParts = baseUrl.tokenize('/')
            def thisParts = actualUrl.tokenize('/')
            int i = 0
            for (; i < baseParts.size(); i++) { 
                if (thisParts[i] == baseParts[i]) {
                    commonStem << baseParts[i]+'/'
                } else {
                    break;
                }
            }
            if (baseParts.size()-1 > i) {
                result << '../' * (baseParts.size()-1 - i)
            }
            result << actualUrl[commonStem.size()+1..-1]
            return result.toString()
        }
    }
    
    void updateActualUrlFromProcessedFile() {
        def p = workDirRelativePath.replace('\\', '/')
        // have to call the setter method
        setActualUrl(p)
    }
    
    boolean excludesMapperOrOperation(String mapperName, String operationName) {
        if (!excludedMappers) {
            return false
        }
        
        def exclude = excludedMappers.contains("*")
        if (!exclude) {
            exclude = excludedMappers.contains(mapperName)
        }
        if (!exclude && operationName) {
            exclude = excludedMappers.contains(operationName)
        }
        return exclude
    }
    
    void wasProcessedByMapper(ResourceMapper mapper, boolean processed = true) {
        attributes["processed.by.${mapper.name}".toString()] = processed
    }
    
    String toString() {
        "ResourceMeta for URI ${sourceUrl} served by ${actualUrl} (delegate: ${delegating ? delegate : 'none'})"
    }
}