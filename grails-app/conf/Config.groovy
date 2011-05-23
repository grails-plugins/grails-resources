
// The following properties have been added by the Upgrade process...
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"


log4j = {
    root {
        info 'stdout'
    }
    
    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
	       'org.codehaus.groovy.grails.web.pages' //  GSP
    debug   'grails.app',
            'org.grails.plugin.resource'
    
}

//grails.resources.processing.enabled = false

environments {
    development {
        grails.serverURL = "http://localhost:8080/resources"
        
        grails.resources.test.excludes = ['/images/**']
        
        grails.resources.adhoc.excludes = ['**/js/core.js']
        
        grails.resources.modules = {
            app {
                dependsOn 'blueprint'
                resource 'css/main.css'
                resource url:'js/application.js', exclude:["bundle"]
                resource url:'images/grails_logo.png', attrs:[width:200, height:100], disposition:'inline'
            }
            jqueryGoogle { 
                defaultBundle false 
                resource 'https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js'
            }
        } 
    }
}

