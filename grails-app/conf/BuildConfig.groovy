grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
	}

	plugins {
		provided(":webxml:1.4.1")
		test(":spock:0.6"){
			export = false
		}
	}
}
