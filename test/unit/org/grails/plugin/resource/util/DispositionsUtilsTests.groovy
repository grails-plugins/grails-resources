package org.grails.plugin.resource.util

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin


@TestMixin(GrailsUnitTestMixin)
class DispositionsUtilsTests {

  void testAddingDispositionToRequest() {
    def request = [:]
    assertTrue DispositionsUtils.getRequestDispositionsRemaining(request).empty

    DispositionsUtils.addDispositionToRequest(request, 'head', 'dummy')
    assertTrue((['head'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))

    // Let's just make sure its a set
    DispositionsUtils.addDispositionToRequest(request, 'head', 'dummy')
    assertTrue((['head'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))

    DispositionsUtils.addDispositionToRequest(request, 'defer', 'dummy')
    assertTrue((['head', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))

    DispositionsUtils.addDispositionToRequest(request, 'image', 'dummy')
    assertTrue((['head', 'image', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))
  }

  void testRemovingDispositionFromRequest() {
    def request = [(DispositionsUtils.REQ_ATTR_DISPOSITIONS_REMAINING):(['head', 'image', 'defer'] as Set)]

    assertTrue((['head', 'image', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))

    DispositionsUtils.removeDispositionFromRequest(request, 'head')
    assertTrue((['defer', 'image'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))

    DispositionsUtils.removeDispositionFromRequest(request, 'defer')
    assertTrue((['image'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request))
  }

}
