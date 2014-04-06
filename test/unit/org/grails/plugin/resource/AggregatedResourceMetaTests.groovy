package org.grails.plugin.resource
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@TestMixin(GrailsUnitTestMixin)
class AggregatedResourceMetaTests {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder()
    File temporarySubfolder
    
    def mockResSvc
    def module 
    
    @org.junit.Before
    void setupTest() {
        temporarySubfolder = temporaryFolder.newFolder('test-tmp')

        module = new ResourceModule()
        module.name = 'aggmodule'
        
        mockResSvc = [
            config : [ ],
            updateDependencyOrder: { -> },
            modulesInDependencyOrder: [module.name],
            getMimeType: { String str -> 'text/plain' },
            makeFileForURI: { uri -> new File(temporarySubfolder, uri)}
        ]
        
    }

    protected ResourceMeta makeRes(String reluri, String contents) {
        def base = new File('./test-tmp/')
        base.mkdirs()
        
        def r = new ResourceMeta(sourceUrl:'/'+reluri)
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.disposition = 'head'
        r.contentType = "text/css"
        r.processedFile = new File(base, reluri)
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()
        r.module = module
        
        r.processedFile << new ByteArrayInputStream(contents.bytes)
        return r
    }
    
    /**
     * Ensure that bundle mapper updates content length and exists()
     */
    void testUpdatesMetadata() {
        def r = new AggregatedResourceMeta()
        
        def r1 = makeRes('/aggtest/file1.css', "/* file 1 */")
        def r2 = makeRes('/aggtest/file2.css', "/* file 2 */")
        
        r.add(r1)
        r.add(r2)

        r.sourceUrl = '/aggtest1.css'
        assertFalse r.exists()
        
        r.beginPrepare(mockResSvc)
        
        r.endPrepare(mockResSvc)
        
        assertTrue r.exists()
        assertTrue r.contentLength >= r1.contentLength + r2.contentLength
    }
}