class BootStrap {
    def resourceService
    
    def init = {
        resourceService.addResourceMapper "test", { r ->
            def f = new File(r.processedFile.parentFile, '_'+r.processedFile.name)
            assert r.processedFile.renameTo(f)
            r.processedFile = f
            r.updateActualUrlFromProcessedFile()
        }, 10
    }
    
    def destroy = {
        
    }
}