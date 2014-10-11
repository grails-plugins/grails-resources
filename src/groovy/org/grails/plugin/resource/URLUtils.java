package org.grails.plugin.resource;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class URLUtils {
    
    public static Pattern externalURLPattern = Pattern.compile("^((https?:?)?//).*");
    private static Pattern invalidUriPartsPattern = Pattern.compile("\\\\|/\\./|/\\.\\.|\\.\\./|//");
    
    private static final int MAX_NORMALIZE_ITERATIONS = 3;

    /**
     * Take a base URI and a target URI and resolve target against the base
     * using the normal rules e.g. "../x", "./x" "x" results in a link relative to the base's folder
     * and / is app-absolute, and anything with a protocol // is absolute
     *
     * Please note, I take full responsibility for the nastiness of this code. I could not 
     * find a nice way to do this, and I wanted to find an existing lib to do it. Its
     * certainly not my finest moment. Sorry. Rely on the MenuTagTests.
     *
     * It's quite ugly in there.
     * @throws URISyntaxException 
     */
    public static String relativeURI(String base, String target) throws URISyntaxException {
        return new URI(base).resolve(new URI(target)).normalize().toString();
    }
    
    /**
     * Works out if url is relative, such that it would need to be corrected if
     * the file containing the url is moved
     */
    public static Boolean isRelativeURL(String url) {
        return !url.startsWith("data:") &&
        !url.startsWith("#") && 
        !(url.indexOf("//") >= 0);
    }

    public static Boolean isExternalURL(String url){
        if(url == null) return false;
        return externalURLPattern.matcher(url).matches();
    }
    
    /**
     * Normalizes and decodes uri once.
     * Check if result contains \ , /../ , /./ or // after decoding and throws IllegalArgumentException in that case
     * 
     * @param uri
     * @return
     */
    public static String normalizeUri(String uri) {
        String currentUri = uri;
        int counter=0;
        boolean processOnceMore = true;
        // handle double-encoding
        while (processOnceMore) {
            if(currentUri == null) return null;
            
            if (counter++ > MAX_NORMALIZE_ITERATIONS) {
                throw new IllegalArgumentException("unable to normalize input uri " + uri);
            }
            
            String normalized = doNormalizeUri(currentUri);
            String decoded = doDecodeUri(normalized, uri);
            processOnceMore = normalized != decoded; // object reference comparison is ok here
            currentUri = decoded;
        }
        return currentUri;
    }

    private static String doNormalizeUri(String uri) {
        if (uri == null) return null;

        String normalized = RequestUtil.normalize(uri);
        if (normalized == null) {
            throw new IllegalArgumentException("illegal uri " + uri);
        }

        return normalized;
    }

    private static String doDecodeUri(String uri, String originalUri) {
        String decoded;
        try {
            decoded = URLDecoder.decode(uri, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if(invalidUriPartsPattern.matcher(decoded).find()) {
            throw new IllegalArgumentException("illegal uri " + originalUri);
        }
        return decoded;
    }
}