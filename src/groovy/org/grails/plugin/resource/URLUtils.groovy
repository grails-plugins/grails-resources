package org.grails.plugin.resource

class URLUtils {
    
    /**
     * Take a base URI and a target URI and resolve target against the base
     * using the normal rules e.g. "../x", "./x" "x" results in a link relative to the base's folder
     * and / is app-absolute, and anything with a protocol :// is absolute
     *
     * Please note, I take full responsibility for the nastiness of this code. I could not 
     * find a nice way to do this, and I wanted to find an existing lib to do it. Its
     * certainly not my finest moment. Sorry. Rely on the MenuTagTests.
     */
     static resolveURI(base, target) {
        if (target.startsWith('/') || (target.indexOf('://') >= 0)) {
            return target
        } else {
            def relbase = base
            def wasAbs = base.startsWith('/')
            if (base != '/') {
                if (wasAbs) {
                    base = base[1..-1]
                }
                def lastSlash = base.lastIndexOf('/')
                if (lastSlash < 0) {
                    relbase = ''
                } else {
                    if (base.endsWith('/')) {
                        lastSlash = base[0..lastSlash-1].lastIndexOf('/')
                    }

                    relbase = base[0..(lastSlash >= 0 ? lastSlash-1 : -1)]                
                }
            }
            def relURI

            if (target.startsWith('../')) {
                // go "up" a dir in the base
                def lastSlash = relbase.lastIndexOf('/')
                if (lastSlash > 0) {
                    relbase = relbase[0..lastSlash-1]
                } else {
                    relbase = ''
                }
                relURI = target[3..-1]
                if (relbase.endsWith('/')) {
                    if (relbase.size() > 1) {
                        relbase = relbase[0..-2]
                    } else {
                        relbase = ''
                    }
                }
            } else if (target.startsWith('./')) {
                relURI = target[2..-1]
            } else if (target.startsWith('/')) {
                return target
            } else {
                relURI = target
            }

            if (relbase) {
                return wasAbs ? "/${relbase}/${relURI}" : "${relbase}/${relURI}"
            } else {
                return wasAbs ? '/' + relURI : relURI
            }
        }
    }
}