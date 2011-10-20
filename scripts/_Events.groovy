import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.XmlUtil

void sortFilterMappingNodes(dom, filterOrder) {
    def sortedMappingNodes = []
    def followingNode

    use (DOMCategory) {
        def mappingNodes = dom.'filter-mapping'
        if (mappingNodes.size()) {
            followingNode = mappingNodes[-1].nextSibling

            Set doneFilters = []
            filterOrder.each { f ->
                mappingNodes.each { n ->
                    def filterName = n.'filter-name'.text()
                    if (!(filterName in doneFilters)) {
                        if ((filterName == f) || ((f == '*') && !filterOrder.contains(filterName))) {
                            sortedMappingNodes << n
                            doneFilters << n
                        }
                    }
                }
            }

            mappingNodes.each { n ->
                dom.removeChild(n)
            }
        }
    }
    sortedMappingNodes.each { n ->
        dom.insertBefore(n, followingNode)
    }
}

/**
 * Re-write the web.xml to order the servlet filters the way we need
 */
eventWebXmlEnd = { filename ->

    def webXmlText = webXmlFile.text
    def reader = new StringReader(webXmlText)
    def doc = DOMBuilder.parse(reader)
    def wxml = doc.documentElement

    def filterOrder = [
        'DeclaredResourcesPluginFilter', 
        'AdHocResourcesPluginFilter', 
        '*', 
        'grailsWebRequest',
        'reloadFilter',
        'ResourcesDevModeFilter',
        'sitemesh',
        'urlMapping',
    ]
    sortFilterMappingNodes(wxml, filterOrder)

    webXmlFile.withWriter { w -> w << XmlUtil.serialize(wxml) }
}

