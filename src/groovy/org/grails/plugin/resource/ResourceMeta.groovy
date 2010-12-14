package org.grails.plugin.resource

import org.apache.commons.io.FilenameUtils

import org.grails.plugin.resource.mapper.ResourceMapper

/**
 * Holder for info about a resource declaration at runtime
 *
 * This is actually non-trivial. A lot of data kept here. Be wary of what you think a "url" is. 
 * See the javadocs for each URL property.
 */
class ResourceMeta {

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
     * @see ResourceService#workDir
     */
    File workDir

    /**
     * The original Url provided in the mapping declaration, verbatim
     */
    final String originalUrl

    /**
     * The app-relative url of the LOCAL source of this resource, minus query params
     */
    String sourceUrl

    /**
     * The original file extension of the resource
     */
    String sourceUrlExtension

    /**
     * The original queryParams of the resource, if any
     */
    String queryParams
    
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
    
    // Hook for when preparation is starting
    void beginPrepare(resourceService) {
    }
    
    // Hook for when preparation is done
    void endPrepare(resourceService) {
        if (!delegating) {
            processedFile.setLastModified(originalLastMod ?: System.currentTimeMillis() )
        }
    }
    
    boolean isDelegating() {
        delegate != null
    }
    
    boolean exists() {
        processedFile != null
    }
    
    String getLinkUrl() {
        if (!delegate) {
            return linkOverride ?: _linkUrl 
        } else {
            return delegate.linkUrl
        }
    }
    
    String  getActualUrl() {
        if (!delegate) {
            return this.@actualUrl 
        } else {
            return delegate.actualUrl
        }
    }

    void setActualUrl(String url) {
        this.@actualUrl = url
        _linkUrl = queryParams ? "${actualUrl}?${queryParams}" : url
    }
    
    
    void setSourceUrl(String url) {
        def qidx = url.indexOf('?')

        this.@originalUrl = url // the full monty
        sourceUrl = qidx >= 0 ? url[0..qidx-1] : url
        queryParams = qidx >= 0 ? url[qidx+1..-1] : null
        
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
    
    boolean excludesMapper(String mapperName) {
        attributes["no$mapperName".toString()]
    }
    
    void wasProcessedByMapper(ResourceMapper mapper) {
        attributes["+${mapper.name}".toString()] = true
    }
}