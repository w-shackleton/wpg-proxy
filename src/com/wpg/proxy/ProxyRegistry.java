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

import java.util.Vector;

/**
 * A static data container that keeps references to every HttpMessageHandler/Processor that
 * must be called by the Proxy for each message. It also containes the SSL key information.
 */
public class ProxyRegistry {
    private static boolean statusBrowser=false;
    private static String keyfile = null;
    private static char[] keystorePass = null;
    private static char[] keystoreKeysPass = null;
    private final static Vector<HttpMessageProcessor> requestProcessors = new Vector<HttpMessageProcessor>();
    private final static Vector<HttpMessageHandler> handlers = new Vector<HttpMessageHandler>();
    private final static Vector<HttpMessageProcessor> responseProcessors = new Vector<HttpMessageProcessor>();
        
    /** is the status browser capability enabled? */
    public static boolean isStatusBrowserEnabled() { return statusBrowser; }
    /** enable or dissable the status browser capability */
    public static void enableStatusBrowser( boolean enable ) { statusBrowser=enable; }
    
    /** Add a new handler to receive incomming http responses, as well as the request */
    public static void addHandler( HttpMessageHandler hml ) {
        handlers.addElement( hml );
    }
    /** Removes a response handler*/
    public static boolean removeHandler( HttpMessageHandler hml ) {
        return handlers.remove( hml );
    }
    /** Get the list of request handlers */
    protected static Vector<HttpMessageHandler> getHandlers(){
        return handlers;
    }
    
    /** Add a new request processor */
    public static void addRequestProcessor( HttpMessageProcessor hmp ) {
        requestProcessors.addElement( hmp );
    }
    /** Removes a response processor*/
    public static boolean removeRequestProcessor( HttpMessageProcessor hmp ) {
        return requestProcessors.remove( hmp );
    }
    /** Get the list of request processors */
    protected static Vector<HttpMessageProcessor> getRequestProcessors(){
        return requestProcessors;
    }
    
    /** Add a new response processor */
    public static void addResponseProcessor( HttpMessageProcessor hmp ) {
        responseProcessors.addElement( hmp );
    }
    /** Removes a response processor*/
    public static boolean removeResponseProcessor( HttpMessageProcessor hmp ) {
        return responseProcessors.remove( hmp );
    }
    /** Get the list of response processors */
    protected static Vector<HttpMessageProcessor> getResponseProcessors(){
        return responseProcessors;
    }
    
    /** Set Keystore File Name */
    protected static void setKeystoreFilename( String s ) {
        keyfile=s;
    }
    /** Set Keystore password */
    protected static void setKeystorePassword( char[] c ) {
        keystorePass=c;
    }
    /** Set Keystore keys password */
    protected static void setKeystoreKeysPassword( char[] c ) {
        keystoreKeysPass=c;
    }
    /** Get Keystore File Name */
    protected static String getKeystoreFilename() {
        return keyfile;
    }
    /** Get Keystore password */
    protected static char[] getKeystorePassword() {
        return keystorePass;
    }
    /** Get Keystore keys password */
    protected static char[] getKeystoreKeysPassword() {
        return keystoreKeysPass;
    }
    
    /** Creates a new instance of ProxyRegistry */
    private ProxyRegistry() {
    }
    
    
}
