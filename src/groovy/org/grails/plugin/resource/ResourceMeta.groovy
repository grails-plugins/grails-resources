package org.grails.plugin.resource

import org.apache.commons.io.FilenameUtils

/**
 * Holder for info about a resource declaration at runtime
 */
class ResourceMeta {

    /**
     * Set on instantiation to be the dir that content is served from
     * 
     * @see ResourceService#workDir
     */
    File workDir

    String sourceUrl
    String queryParams
    /* This will be handled by flavours
    String minifiedUrl
    String cdnUrl
    */
    
    String actualUrl
    String linkOverride
    String contentType
    
    String disposition
    
    //@todo impl this later
    //String cachedTagText
    
    File processedFile
    
    // For per-resource options like "nominify", 'nozip'
    Map attributes = [:]
    
    // For per-resource tag resource attributes like "media", 'width', 'height' etc
    Map tagAttributes = [:]

    Closure prePostWrapper

    // A list of Closures taking request & response. Delegates to resourceMeta
    List requestProcessors = []
    
    private String _linkUrl
    
    boolean exists() {
        processedFile != null
    }
    
    String getLinkUrl() {
        linkOverride ?: _linkUrl 
    }
    
    void setActualUrl(String url) {
        this.@actualUrl = url
        _linkUrl = queryParams ? "${actualUrl}?${queryParams}" : url
    }
    
    void setSourceUrl(String url) {
        def qidx = url.indexOf('?')

        sourceUrl = qidx >= 0 ? url[0..qidx-1] : url
        queryParams = qidx >= 0 ? url[qidx+1..-1] : null
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
        // @todo I still don't trust this. surely a URL with duplication in would cause odd effects?
        workDirRelativePath - "/$processedFile.name"
    }
    
    String getWorkDirRelativePath() {
        processedFile.path - workDir.path
    }
    
    void updateActualUrlFromProcessedFile() {
        def p = workDirRelativePath.replace('\\', '/')
        // Strip leading / off, we want relative paths
        if (p.startsWith('/')) {
            p = p[1..-1]
        }
        // have to call the setter method
        setActualUrl(p)
    }
}