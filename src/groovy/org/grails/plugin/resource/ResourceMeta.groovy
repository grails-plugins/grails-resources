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
        println "setActualUrl: $url"
        this.@actualUrl = url
        println "updating linkUrl actual: $actualUrl"
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
    
    void updateActualUrlFromProcessedFile() {
        println "Updating from processed file: ${processedFile} / ${workDir}"
        setActualUrl((processedFile.path - workDir.path).replace('\\', '/'))
        println "Back from Updating from processed file: new actual url is ${actualUrl} - ${processedFile} / ${workDir}"
    }
}