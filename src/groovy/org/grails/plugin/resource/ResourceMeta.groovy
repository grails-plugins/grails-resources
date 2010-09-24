package org.grails.plugin.resource

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
    String minifiedUrl
    String cdnUrl

    String actualUrl
    String contentType
    
    String disposition
    
    //@todo impl this later
    //String cachedTagText
    
    File processedFile
    
    // For per-resource options like "minify", 'zip'
    Map attributes = [:]
    
    // For per-resource tag resource attributes like "media", 'width', 'height' etc
    Map tagAttributes = [:]

    Closure prePostWrapper

    // A list of Closures taking request & response. Delegates to resourceMeta
    List requestProcessors = []
    
    boolean exists() {
        processedFile != null
    }
    
    /**
     * The file extension of the processedFile, or null if it has no extension.
     */
    String getProcessedFileExtension() {
        if (processedFile) {
            def extensionSeperatorIndex = processedFile.name.lastIndexOf('.')
            if (extensionSeperatorIndex == -1 || extensionSeperatorIndex == (processedFile.name.size() - 1)) {
                null
            } else {
                processedFile.name.substring(extensionSeperatorIndex + 1)
            }
        }
    }
    
    void updateActualUrlFromProcessedFile() {
        actualUrl = (processedFile.path - workDir.path).replace('\\', '/')
    }
}