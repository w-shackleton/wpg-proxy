/*
 Java HTTP Proxy Library (wpg-proxy), 
    more info at http://wpg-proxy.sourceforge.net/

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public

 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.wpg.proxy;

import java.net.URI;
import java.net.URISyntaxException;

/** HTTP Request Message Class, this is the message received from the client for processing */
public class HttpMessageRequest extends HttpMessage {
    private final String[] methodStrings = {"CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT", "TRACE"};
    private int method;
    private URI uri;
    
    /** Get the HTTP Method of this request */
    public String getMethod() {
        return methodStrings[method];
    }
    /** Set the HTTP Method of this request, with a String */
    public void setMethod( String s ) {
        for(int i=0; i< methodStrings.length; i++) {
            if( s.equals(methodStrings[i]) )
                method=i;
        }
    }
    
    /** Get the Request Start Line */
    public String getStartLine() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(getMethod() + " ");
            uri = getUri();
            sb.append(uri.getPath());
            if( uri.getQuery() != null )
                sb.append(uri.getQuery());
            if( uri.getFragment() != null )
                sb.append(uri.getFragment());
            sb.append(" "+ getProtocol() + getVersion() );
        } catch( Exception e ) {
            logger.warn("Exception caught while building the Start Line for this request: "+ e,e);
        }
        return sb.toString();
    }
    /** Set the Start Line of the Message */
    public void setStartLine(String s) {
        startLine=s;
        setVersion( s.substring( s.lastIndexOf('/')+1 ) ); }
    
    /** Get the URI of this request */
    public URI getUri() {
        try {
            uri = new URI( uri.getScheme(), uri.getUserInfo(), getToHost(), getToPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()
                    );
        } catch( Exception e ) {
            logger.warn("Exception caught while building the URI for this request: "+e, e);
        }
        return uri;
    }
    /** Set the URI of this request */
    public void setUri( URI u ) throws java.net.URISyntaxException {
        uri=u;
        setToHost( u.getHost());
        setToPort( u.getPort());
        //setProtocol( u.toURL().getProtocol() );
    }
    /** Set the URI of this request, from a String */
    public void setUri( String s ) throws java.net.URISyntaxException {
        setUri(new URI(s));
    }
}
