
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
    }
}

//grails.resources.defer.default=false
grails.resources.modules = {
    'jquery' { 
        resource url:'js/jquery/jquery-1.4.2-min.js', nominify:true, disposition:'head'
    }
    'jquery-ui' {
        dependsOn 'jquery'
        resource url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.js'], nominify:true
        resource url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.css'], nominify:true
    }
    'blueprint' {
        resource url:[dir:'css/blueprint',file:'main.css']
        resource url:[dir:'css/blueprint',file:'ie.css'], wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
    }
    'app' {
        resource 'css/main.css'
        resource 'js/application.js'
        resource url:'images/grails_logo.png', attrs:[width:200, height:100], disposition:'inline'
    }
} 
