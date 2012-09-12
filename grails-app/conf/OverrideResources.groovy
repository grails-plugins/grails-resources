modules = {
    overrides {
        'blueprint' {
            defaultBundle 'monolith'
            // Make sure we report bad dependsOn syntax
            dependsOn ['a', 'b', 'c']
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