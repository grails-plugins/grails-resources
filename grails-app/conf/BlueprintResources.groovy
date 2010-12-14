modules = {
    'blueprint' {
        defaultBundle 'blueprint'
        
        resource url:[dir:'css/blueprint',file:'screen.css'], attrs:[media:'screen, projection'] 
        resource url:[dir:'css/blueprint',file:'ie.css'], attrs:[media:'screen, projection'],  
            wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
    }
}
