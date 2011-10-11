import grails.util.Environment

class DevResourcesFilters {
    def resourceService
    
    def filters = {
        if ( Environment.current == Environment.DEVELOPMENT) {
            devSnafuCatcher(controller:'*', action:'*') {
                afterView = {
                    def dispositionsLeftOver = resourceService.getRequestDispositionsRemaining(request)
                    if (dispositionsLeftOver) {
                        throw new RuntimeException("It looks like you are missing some calls to tag r:layoutResources. "+
                            "After rendering your view dispositions ${dispositionsLeftOver} are still pending.")
                    }
                }
            }
        }
    }
}