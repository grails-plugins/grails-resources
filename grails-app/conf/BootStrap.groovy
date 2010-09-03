/**
 * Development-mode only bootstrap
 */

class BootStrap {
 
    def resourceService
    
    def init = { servletContext ->
        
        resourceService.with {
            module 'jquery', 'js/jquery/jquery-1.4.2-min.js'
            module 'jquery-ui', [
                [url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.js']],
                [url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.css']]
            ], ['jquery']

            module 'blueprint', [
                [url:[dir:'css/blueprint',file:'main.css']],
                [url:[dir:'css/blueprint',file:'ie.css'], wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }]
            ]

            module 'main', [
                'css/main.css',
                'js/application.js'
            ]
        }
    }
    
    def destroy = {
        
    }
}