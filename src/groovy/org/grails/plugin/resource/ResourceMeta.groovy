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
}