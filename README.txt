
Features:


* Creates appropriate <link>/<script> content in <head> section for resources

* Allows definition of resource modules, and single tag to render the links to
  all the resources required, even modules they depend on

* Using the tags supplied, a resource cannot be included more than once in a
  page (saves html, reduces errors - include a file in GSP that layout already has, no problem)

* Provides hooks for other plugins to process resources (even change their URI)
  at runtime - caching, minifying, compressing etc.

* Provides r:resource tag to provide 1:1 mapping over g:resource but with
  support for mutated content in any grails environment including development

* Automatically processes all your existing resources even if you don't use
  any of the new tags this plugin supplies, so legacy apps can make use of it. The one caveat:
  other plugins that use this plugin to provide long-term caching in the browser will not work
  as a redirect is involved as the resources have no unique name and hence cannot be eternally 
  cached in the client. Suitable for setups where you have a front-side proxy/cache and don't
  want to change any code. Instant win!

* Supports flags for resources that other resource processing plugins can use during the processing chain,

* Using flags, can supply pre-processed gzipped or minified files and still use the same tags etc.

* Caches the processed files on server filesystem - process chain executed only once per resource

* Provides a uniform resource API that other plugins can use to prevent duplication of resources in pages

* Support for rendering external JS resources at end of page instead of inline

* DSL to define resources

* Detect ?debugResources var in request params of *Referer* and use clean non-processed resources (sourceUrl) for that request


Will also soon:

* Change default extensions mechanism to grails.resources.filter.includes and allow any servlet matcher pattern there. Gives finer control

* Add linkOverride mechanism

* Support automatic bundling of files into less files prior to processing, and
  give application ability to override/redefine the bundling of resources
  declared by other plugins (e.g. rebundle plugin files into different bundles)

* Support "flavours" of resources e.g. source, CDN, minified etc

* Support for defining which mappers are to be assigned to each file type -
  e.g. allow cachability of images but not gzipping of them

* Support for parameterized resources eg plugins or themes, by passing a map to the linking tags, which will result
	in new resources being created from the parameterized template:
	
    resource id:'jq-ui-css', url:[dir:'js/jquery-ui/themes/$theme', file:'jquery-ui-1.8-custom-min.css'], minified: true

  Invoking <r:dependsOn name="jquery-ui" args="[theme:'cupertino']"/> would put "cupertino" into the resource with $theme substituted.	

* Application-specific resource dependency overrides (e.g. force a plugin you use to use a newer version of jquery)

* Cache the HTML needed to include the JS and CSS resources, so including these becomes very efficient

* Allow app author to control which URIs are subject to filtering, not just types. E.g. a CMS may not want all its images and CSS processed.

* Add option to CachedResources plugin to flatten the directory structure for shorter uris

* Make CachedResources use base62 encoding to shorten hashed url links

* Make SmartImageResources plugin that adds an <r:img> tag that uses
  <r:resource> to locate the resource and renders out width and height that
  are pre-calculated by parsing the image

* Support for apps to update the resource definitions of plugins e.g. to force no-defer on jquery


DSL FULL FEATURE (NOT ALL IMPL'D) EXAMPLE


// App and plugins stash their module DSL definitions into Config.
//
// Possible syntaxes are:
// '<module-name>' <resource-closure> 
//
// <module-name> is a string
// <resource-closure> can invoke:
//      resource <string-uri>
//      resource <map-of-args>
//      dependsOn <string-module-id>
//      dependsOn <list-of-string-module-id>
//      <flavour-name> grouping name for alternative sets of resources to be chosen at build/runtime (?)
grails.resources.defer.default = 'true'
grails.resources.modules << {
    'jquery' {
        resource url:'js/jquery/jquery-1.4.2.js', bundle:'jq'
    }
    'jquery-ui' {
        dependsOn 'jquery'
        
        // Default resources with ids so they can be inherited/reused
        resource id:'jq-ui-js', url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.css'], 
			attrs:[media:'screen'], nominify: true, bundle:'jq'
        resource id:'jq-ui-css', url:[dir:'js/jquery-ui', file:'jquery-ui-1.8-min.css'], 
			nominify: true, bundle:'jq'

        // Alternative resources to use if flavour desired = cdn
        // Set with resources.flavour = 'cdn' in Config
        cdn {
            // Just string arg = a full url that will not be processed at all
            resource inherit:'jq-ui-js', linkOverride:"http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/themes/$theme/jquery-ui.css"
            resource inherit:'jq-ui-css', linkOverride:"http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.${min ? 'min.js' : 'js'}"
        }
    }

    'blueprint' {
        resource url:[dir:'css/blueprint',file:'main.css'], 
			attrs:[media:'screen']
        resource url:[dir:'css/blueprint',file:'print.css'], 
			attrs:[media:'print']
        resource url:[dir:'css/blueprint',file:'ie.css'], 
			attrs:[media:'screen'],
            wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
    }

	// Example of build-time already optimized resources
	'prepared-statics' {
		resource uri:'pdfs/2010-catalogue.pdf', actualUri:'pdfs/2010-catalogue.pdf.gz', nozip: true
	}
	
    'main' {
		// Pass-through attributes rendered by r:resourceLink
		resource url:"images/logo.png", attrs:[width:100, height:48]
		
        resource url:'css/main.css'
        resource url:'js/application.js'
        resource url:'js/appcode.js', defer: false // make sure it loads in head
		// A generated resource that resource plugins can see should be cached per-user
		resource url:'generator/user-specific.css', userSpecific: true, nominify:true
    }
}
