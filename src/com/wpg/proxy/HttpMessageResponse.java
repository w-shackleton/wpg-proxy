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

import java.util.StringTokenizer;

/** HTTP Response Message Class, this is the message received from the remote host in response to a request */
public class HttpMessageResponse extends HttpMessage {
    private int statusCode;
    private String reasonPhrase;
    
    /** Get the HTTP Reason Phrase of this request */
    public String getReasonPhrase() { return reasonPhrase; }
    /** Set the HTTP Reason Phrase of this request*/
    public void setReasonPhrase( String s ) { reasonPhrase=s; }
    /** Get the HTTP Status Code of this request*/
    public int getStatusCode() { return statusCode; }
    /** Set the HTTP Status Code of this request*/
    public void setStatusCode( int i ) { statusCode=i; }
    /** Set the Start Line of the Message */
    public void setStartLine(String s) {
        if(s==null || s.length()==0)
            return;
        startLine=s;
        StringTokenizer st = new StringTokenizer( s, "/ "); //String: HTTP/1.0 200 OK
        setProtocol( st.nextToken() );
        setVersion( st.nextToken() );
        setStatusCode( new Integer( st.nextToken() ) );
        setReasonPhrase( st.nextToken() );
    }
}
