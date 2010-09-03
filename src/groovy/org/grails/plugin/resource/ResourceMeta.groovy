package org.grails.plugin.resource

/**
 * Holder for info about a resource declaration at runtime
 */
class ResourceMeta {
    String sourceUrl
    String actualUrl
    String cachedTagText
    String sourceType // one of "img", "css" or "js"

    // For per-resource options like "minify", 'zip'
    Map attributes = [:]
    Closure prePostWrapper
    
    void setSourceUrl(String s) {
        sourceUrl = s
        actualUrl = s
    }

}