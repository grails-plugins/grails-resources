package org.grails.plugin.resource

import javax.servlet.Filter

import org.springframework.context.ApplicationContext

class FilterBeanTests extends GroovyTestCase {

	def grailsApplication
	def grailsResourceProcessor

	void testFilterBeans() {

		ApplicationContext ctx = grailsApplication.mainContext
		def filterBeans = ctx.getBeansOfType(Filter)
		assert 2 == filterBeans.size()

		assert ['AdHocResourcesPluginFilter', 'DeclaredResourcesPluginFilter'] == filterBeans.keySet().sort()
	}

	void testProperties() {

		def ctx = grailsApplication.mainContext

		def AdHocResourcesPluginFilter = ctx.AdHocResourcesPluginFilter
		assert AdHocResourcesPluginFilter
		assert AdHocResourcesPluginFilter instanceof ProcessingFilter
		assert AdHocResourcesPluginFilter.grailsResourceProcessor
		assert AdHocResourcesPluginFilter.grailsResourceProcessor == grailsResourceProcessor
		assert AdHocResourcesPluginFilter.adhoc
		
		def DeclaredResourcesPluginFilter = ctx.DeclaredResourcesPluginFilter
		assert DeclaredResourcesPluginFilter
		assert DeclaredResourcesPluginFilter instanceof ProcessingFilter
		assert DeclaredResourcesPluginFilter.grailsResourceProcessor
		assert DeclaredResourcesPluginFilter.grailsResourceProcessor == grailsResourceProcessor
		assert !DeclaredResourcesPluginFilter.adhoc
	}
}
