package org.grails.plugin.resource

import org.apache.commons.io.FilenameUtils
import org.grails.plugin.resource.mapper.ResourceMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource

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

    static final String PROCESSED_BY_PREFIX = 'processed.by.'

    private Logger log = LoggerFactory.getLogger(getClass())

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

    Set excludedMappers

    // For per-resource options like "nominify", 'nozip'
    Map attributes = [:]

    // For per-resource tag resource attributes like "media", 'width', 'height' etc
    Map tagAttributes = [:]

    Closure prePostWrapper

    // ***** Below here is state we determine at runtime during processing *******

    /**
     * The delegate to actually use when linking, if any. Think bundling.
     */
    private ResourceMeta delegate

    Resource originalResource

    Long originalSize

    Long processedSize

    File processedFile

    long originalLastMod

    // A list of Closures taking request & response. Delegates to resourceMeta
    List requestProcessors = []

    private String _linkUrl

    private boolean processed

    private Boolean _resourceExists

    /**
     * The URI of the resource that resulted in the processing of this resource, or null
     * For resources ref'd in CSS or stuff loaded up by bundles for example
     */
    String declaringResource

    Integer contentLength

    Integer originalContentLength = 0

    void delegateTo(ResourceMeta target) {
        delegate = target

        // No more processing to be done on us
        processed = true
    }

    boolean isOriginalAbsolute() {
        sourceUrl.indexOf(':/') > 0
    }

    boolean isActualAbsolute() {
        actualUrl.indexOf(':/') > 0
    }

    boolean isDirty() {
        !originalResource || (originalResource.lastModified() != originalLastMod)
    }

    boolean needsProcessing() {
        processed
    }

    void updateContentLength() {
        if (processedFile) {
            contentLength = processedFile.size().toInteger()
        } else if (originalResource?.URL.protocol in ['jndi', 'file']) {
            contentLength = originalResource?.URL.openConnection().contentLength
        } else {
            contentLength = 0
        }
    }

    void setOriginalResource(Resource res) {
        originalResource = res
        updateExists()
        originalContentLength = originalResource?.URL.openConnection().contentLength
        updateContentLength()
    }

    void setProcessedFile(File f) {
        processedFile = f
        updateExists()
        updateContentLength()
    }

    void updateExists() {
        if (processedFile) {
            _resourceExists = processedFile.exists()
            if (!originalLastMod && _resourceExists) {
                originalLastMod = processedFile.lastModified()
            }
            if (originalSize == null) {
                originalSize = _resourceExists ? processedFile.length() : 0
            }
        } else if (originalResource) {
            _resourceExists = originalResource.exists()
            if (!originalLastMod && _resourceExists) {
                originalLastMod = originalResource.lastModified()
            }
        }
    }
    private void copyOriginalResourceToWorkArea() {
        def inputStream = originalResource.inputStream
        try {
            // Now copy in the resource from this app deployment into the cache, ready for mutation
            processedFile << inputStream
            _resourceExists = processedFile.exists()
        }
        finally {
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
    void beginPrepare(ResourceProcessor grailsResourceProcessor) {
        String uri = sourceUrl
        if (uri.contains('://')) {
			  setOriginalResource(new UrlResource(sourceUrl))
			  setActualUrl(sourceUrl)

			  log.warn "Skipping mappers for $actualUrl because its an absolute URL."
			  return
        }

        // Delete whatever file may already be there
        processedFile?.delete()

        String uriWithoutFragment = uri
        if (uri.contains('#')) {
            uriWithoutFragment = uri.substring(0, uri.indexOf('#'))
        }

        URL origResourceURL = grailsResourceProcessor.getOriginalResourceURLForURI(uriWithoutFragment)
        if (!origResourceURL) {
            if (declaringResource) {
                log.error "While processing ${declaringResource}, a resource was required but not found: ${uriWithoutFragment}"
            } else {
                log.error "Resource not found: ${uriWithoutFragment}"
            }
            throw new FileNotFoundException("Cannot locate resource [$uri]")
        }

        contentType = grailsResourceProcessor.getMimeType(uriWithoutFragment)
        if (log.debugEnabled) {
            log.debug "Resource [$uriWithoutFragment] ($origResourceURL) has content type [${this.contentType}]"
        }

        setOriginalResource(new UrlResource(origResourceURL))

        if (grailsResourceProcessor.processingEnabled) {
            setActualUrl(uriWithoutFragment)

            setProcessedFile(grailsResourceProcessor.makeFileForURI(uriWithoutFragment))
            // copy the file ready for mutation
            copyOriginalResourceToWorkArea()
        } else {
            setActualUrl(uriWithoutFragment)
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
        processed = true
    }

    boolean isDelegating() {
        delegate != null
    }

    boolean exists() {
        _resourceExists
    }

    String getLinkUrl() {
        delegate ? delegate.linkUrl : linkOverride ?: _linkUrl
    }

    String getActualUrl() {
        delegate ? delegate.actualUrl : actualUrl
    }

    void setActualUrl(String url) {
        actualUrl = url
        _linkUrl = sourceUrlParamsAndFragment ? actualUrl + sourceUrlParamsAndFragment : url
    }

    void setSourceUrl(String url) {
        if (originalUrl == null) {
            originalUrl = url // the full monty
        }

        int qidx = url.indexOf('?')
        int hidx = url.indexOf('#')

        int chopIdx = -1
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
        processedFile ? processedFile.path - workDir.path : null
    }

    String getActualUrlParent() {
        int lastSlash = actualUrl.lastIndexOf('/')
        lastSlash >= 0 ? actualUrl[0..lastSlash-1] : ''
    }

    String relativeToWithQueryParams(ResourceMeta base) {
        String url = relativeTo(base)
        return sourceUrlParamsAndFragment ? url + sourceUrlParamsAndFragment : url
    }

    ResourceMeta getDelegate() {
        delegate
    }

    /**
     * Reset the resource state to how it was after loading from the module definition
     * i.e. keep only declared info, nothing generated later during processing
     * // @todo should we delete the file in here?
     */
    void reset() {
        contentType = null
        actualUrl = null
        processedFile = null
        originalResource = null
        _resourceExists = false
        originalContentLength = 0
        _linkUrl = null
        delegate = null
        originalLastMod = 0
        contentLength = 0
        declaringResource = null
        requestProcessors.clear()
        processed = false
        attributes.entrySet().removeAll { it.key.startsWith(PROCESSED_BY_PREFIX) }
    }

    /**
     * Calculate the URI of this resource relative to the base resource.
     * All resource URLs must be app-relative with no ../ or ./
     */
    String relativeTo(ResourceMeta base) {
        if (actualAbsolute) {
            return actualUrl
        }
        String baseDirStr = base.actualUrlParent
        String thisDirStr = actualUrlParent
        boolean isChild = thisDirStr.startsWith(baseDirStr+'/')
        if (isChild) {
            // Truncate to the part that is after the base dir
            return actualUrl[baseDirStr.size()+1..-1]
        }

        def result = new StringBuilder()

        def commonStem = new StringBuilder()
        String baseUrl = base.actualUrl
        // Eliminate the common portion - the base to which we need to ".."
        def baseParts = baseUrl.tokenize('/')
        def thisParts = actualUrl.tokenize('/')
        int i = 0
        for (; i < baseParts.size(); i++) {
            if (thisParts[i] == baseParts[i]) {
                commonStem << baseParts[i]+'/'
            } else {
                break
            }
        }
        if (baseParts.size()-1 > i) {
            result << '../' * (baseParts.size()-1 - i)
        }
        result << actualUrl[commonStem.size()+1..-1]
        result.toString()
    }

    void updateActualUrlFromProcessedFile() {
        def p = workDirRelativePath?.replace('\\', '/')
        if (p != null) {
            // have to call the setter method
            setActualUrl(p)
        } else {
            setActualUrl(sourceUrl)
        }
    }

    boolean excludesMapperOrOperation(String mapperName, String operationName) {
        if (!excludedMappers) {
            return false
        }

        boolean exclude = excludedMappers.contains("*")
        if (!exclude) {
            exclude = excludedMappers.contains(mapperName)
        }
        if (!exclude && operationName) {
            exclude = excludedMappers.contains(operationName)
        }
        return exclude
    }

    void wasProcessedByMapper(ResourceMapper mapper, boolean processed = true) {
        attributes[PROCESSED_BY_PREFIX+mapper.name] = processed
    }

    String toString() {
        "ResourceMeta for URI $sourceUrl served by $actualUrl (delegate: ${delegating ? delegate : 'none'})"
    }
}
