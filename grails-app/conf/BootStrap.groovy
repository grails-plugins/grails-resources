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
        
        /*
        // App and plugins stash their module DSL definitions into Config.
        //
        // Possible syntaxes are:
        // '<module-name>' <resource-closure> 
        //
        // <module-name> is a string
        // <resource-closure> can invoke:
        //      resource <string-uri>
        //      resource <map-of-args>
        //      dependsOn <string-module-id>
        //      dependsOn <list-of-string-module-id>
        //      <flavour-name> grouping name for alternative sets of resources to be chosen at build/runtime (?)
        config.resources.modules << {
            'jquery' {
                resource 'js/jquery/jquery-1.4.2.js'
            }
            'jquery-ui' {
                dependsOn 'jquery'
                
                // Default resources
                resource id:'', url:jsurl, minified: true
                resource url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.css'], minified: true

                // Alternative resources to use if flavour desired = cdn
                // Set with resources.flavour = 'cdn' in Config
                cdn {
                    // Just string arg = a full url that will not be processed at all
                    resource inherit:true, linkOverride:"http://ajax.googleapis.com/ajax/libs/jqueryui/$jqver/themes/$theme/jquery-ui.css"
                    resource "http://ajax.googleapis.com/ajax/libs/jqueryui/$jqver/jquery-ui.${min ? 'min.js' : 'js'}"
                }
            }

            'blueprint' {
                resource url:[dir:'css/blueprint',file:'main.css']
                resource url:[dir:'css/blueprint',file:'ie.css'], 
                    wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
            }

            'main' {
                resource 'css/main.css'
                resource 'js/application.js'
            }
        }
        */
    }
    
    def destroy = {
        
    }
}