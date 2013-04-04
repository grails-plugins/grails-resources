package org.grails.plugin.resource

import grails.test.GrailsUnitTestCase

import org.junit.rules.TemporaryFolder

abstract class AbstractResourcePluginTests extends GrailsUnitTestCase {

	private TemporaryFolder temporaryFolder = new TemporaryFolder()
	protected File temporarySubfolder

	protected void setUp() {
		super.setUp()
		temporaryFolder.create()
		temporarySubfolder = temporaryFolder.newFolder('test-tmp')
	}
}
