
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

environments {
    development {
        grails.serverURL = "http://localhost:8080/resources"
        
        grails.resources.test.excludes = ['/images/**']
        
        grails.resources.modules = {
            'jquery' { 
                resource url:'js/jquery/jquery-1.4.2.min.js', nominify:true, disposition:'head', bundle:'jq'
            }
            'jquery-ui' {
                dependsOn 'jquery'
                resource url:[dir:'js/jquery-ui', file:'jquery-ui-1.8.2.custom.min.js?someargument=value'], nominify:true, bundle:'jq'
                resource url:[dir:'js/jquery-ui', file:'jquery-ui-1.8.2.custom.css'], nominify:true, 
                    attrs:[media:'screen, projection'], bundle:'jq'
            }
            'blueprint' {
                resource url:[dir:'css/blueprint',file:'screen.css'], attrs:[media:'screen, projection'], bundle:'blueprint'
                resource url:[dir:'css/blueprint',file:'ie.css'], attrs:[media:'screen, projection'], bundle:'blueprint', 
                    wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
            }
            'app' {
                dependsOn 'blueprint'
                resource url:'css/main.css', bundle:'app'
                resource url:'js/application.js', bundle:'app'
                resource url:'images/grails_logo.png', attrs:[width:200, height:100], disposition:'inline'
            }
        } 
    }
}

