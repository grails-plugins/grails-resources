environments {
    development {
        modules = {
            'blueprint' {
                resource url:[dir:'css/blueprint',file:'screen.css'], attrs:[media:'screen, projection'], bundle:'blueprint'
                resource url:[dir:'css/blueprint',file:'ie.css'], attrs:[media:'screen, projection'], bundle:'blueprint', 
                    wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
            }
        }
    }
}