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

/** Interface for a user defined HTTP Message Handler */
public interface HttpMessageHandler {
    /** Failed to get a request */
    public void failed( Exception exception );
    /** Failed to get a response for the given request */
    public void failedRequest( HttpMessageRequest request, Exception exception );
    /** Failed to send the given response for the given request back to the user */
    public void failedResponse( HttpMessageResponse response, HttpMessageRequest request, Exception exception );
    /** Just received a request, this is called before the request is sent out but after the request processors are executed */
    public void receivedRequest( HttpMessageRequest request);
    /** Just received a response for the given request, but have not yet returned it to the user */
    public void receivedResponse( HttpMessageResponse response, HttpMessageRequest request);
}
