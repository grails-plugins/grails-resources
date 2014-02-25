package org.grails.plugin.resource

import spock.lang.Specification
import static URLUtils.normalizeUri

class URLUtilsSpec extends Specification {
    def './ should get normalized'() {
        expect:
        normalizeUri('/parentdir/./some-dir/file.xml') == '/parentdir/some-dir/file.xml'
    }
    
    def '../ should get normalized'() {
        expect:
        normalizeUri('/parentdir/something/../some-dir/file.xml') == '/parentdir/some-dir/file.xml'
    }
    
    def 'fail if ../ goes beyond root'() {
        when:
        normalizeUri('../../test')
        then:
        thrown IllegalArgumentException
    }
    
    def 'allow spaces in path'() {
        expect:
        normalizeUri('/parentdir/a b c.xml') == '/parentdir/a b c.xml'
    }
}
