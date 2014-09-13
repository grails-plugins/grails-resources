
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
    
    def 'fail if contains double encoded path traversal'() {
        when:
        def uri = normalizeUri('/test1/static/css/..%252fc..%252f..%252f c.txt')
        then:
        thrown IllegalArgumentException
    }
    
    def 'double url encoded should get normalized'() {
        expect:
        normalizeUri('/parentdir/%25%37%33%25%36%66%25%36%64%25%36%35%25%32%64%25%36%34%25%36%39%25%37%32/file.xml')  == '/parentdir/some-dir/file.xml'
    }

    def 'triple url encoded should get normalized'() {
        expect:
        normalizeUri('/parentdir/%25%32%35%25%33%37%25%33%33%25%32%35%25%33%36%25%36%36%25%32%35%25%33%36%25%36%34%25%32%35%25%33%36%25%33%35%25%32%35%25%33%32%25%36%34%25%32%35%25%33%36%25%33%34%25%32%35%25%33%36%25%33%39%25%32%35%25%33%37%25%33%32/file.xml') == '/parentdir/some-dir/file.xml'
    }

    def 'fail if normalization limit exceeds'() {
        when:
        def uri=normalizeUri('/parentdir/%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%37%25%32%35%25%33%33%25%33%33%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%36%25%32%35%25%33%36%25%33%36%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%36%25%32%35%25%33%36%25%33%34%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%36%25%32%35%25%33%33%25%33%35%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%32%25%32%35%25%33%36%25%33%34%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%36%25%32%35%25%33%33%25%33%34%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%36%25%32%35%25%33%33%25%33%39%25%32%35%25%33%32%25%33%35%25%32%35%25%33%33%25%33%37%25%32%35%25%33%33%25%33%32/file.xml')
        then:
        thrown IllegalArgumentException
    }
}
