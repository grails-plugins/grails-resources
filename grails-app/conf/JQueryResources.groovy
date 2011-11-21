modules = {
    'jquery' { 
        resource id:'jquery-js', url:'js/jquery/jquery-1.4.2.min.js', exclude:'minify', disposition:'head', bundle:'jq'
    }
    'jquery-ui' {
        dependsOn 'jquery'
        
        resource id:'jquery-ui-js', url:[dir:'js/jquery-ui', file:'jquery-ui-1.8.5.custom.min.js?someargument=value'], 
            exclude:'minify', bundle:'jq'
        resource id:'jquery-ui-css', url:[dir:'js/jquery-ui/themes/custom-theme', file:'jquery-ui-1.8.5.custom.css'], 
            exclude:'minify', attrs:[media:'screen, projection'], bundle:'jq'
    }
}
