environments {
    test {
        modules = {
            'jquery' { 
                resource url:'js/jquery/jquery-1.4.2-b.min.js', disposition:'head'
            }
			
			'testAppAccess' {
				resource id:'testAppAccess', 
				         url: "css/main.css",
						 wrapper: {"<!--${grailsApplication.ENV_DEVELOPMENT}-->"}
			}
        }
    }
}