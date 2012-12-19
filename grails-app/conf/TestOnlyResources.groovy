environments {
    test {
        modules = {
            'jquery' { 
                resource url:'js/jquery/jquery-1.4.2-b.min.js', disposition:'head'
            }
			testurl {
				resource url: 'http://fonts.googleapis.com/css?family=PT+Sans:400,700&subset=latin,cyrillic', 
				          disposition: 'head', 
						  attrs: [type: 'css']
			}
			'google-maps' {
				resource url: 'http://maps.googleapis.com/maps/api/js?libraries=places&sensor=false', attrs: [type: 'js']
			}
			  
        }
    }
}