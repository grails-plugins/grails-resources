modules = {
    overrides {
        'blueprint' {
            defaultBundle 'monolith'
        }
        'jquery' {
            resource id:'jquery-js', bundle: 'monolith'
        }
        'jquery-ui' {
            resource id:'jquery-ui-js', bundle: 'monolith'
            resource id:'jquery-ui-css', bundle: 'monolith'
        }
    }
}