class BootStrap {
    def resourceService
    
    def init = {
        resourceService.addResourceMapper "test", { r ->
            def f = new File(r.processedFile.parentFile, '_'+r.processedFile.name)
            println "new file: $f"
            assert r.processedFile.renameTo(f)
            r.processedFile = f
            r.updateActualUrlFromProcessedFile()
        }
    }
    
    def destroy = {
        
    }
}