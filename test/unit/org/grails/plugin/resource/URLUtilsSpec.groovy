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
        normalizeUri('/parentdir/a%20b%20c.xml') == '/parentdir/a b c.xml'
    }
    
    def 'fail if contains .. path traversal after decoding'() {
        when:
        normalizeUri('/some/path/%2e%2e/some-dir/file.xml')
        then:
        thrown IllegalArgumentException
    }
    
    def 'fail if contains backslash after decoding'() {
        when:
        normalizeUri('/some/path/%2e%2e%5c%2e%2e/some-dir/file.xml')
        then:
        thrown IllegalArgumentException
    }

    def 'fail if contains . path traversal after decoding'() {
        when:
        normalizeUri('/some/path/%2e/some-dir/file.xml')
        then:
        thrown IllegalArgumentException
    }
}
