grails.project.work.dir = 'target'

forkConfig = false
grails.project.fork = [
    test:    forkConfig, // configure settings for the test-app JVM
    run:     forkConfig, // configure settings for the run-app JVM
    war:     forkConfig, // configure settings for the run-war JVM
    console: forkConfig, // configure settings for the Swing console JVM
    compile: forkConfig  // configure settings for compilation
]

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
        mavenLocal()
        grailsCentral()
        mavenRepo "http://repo.grails.org/grails/core"
    }

    dependencies {
//        build 'org.codehaus.gpars:gpars:0.12'
    }
    plugins {        
        provided(":webxml:1.4.1") 
        build(":tomcat:7.0.50.1") {
            export = false
        }
        compile(':release:3.0.1', ':rest-client-builder:1.0.3') {
            export = false
        }
    }
}
