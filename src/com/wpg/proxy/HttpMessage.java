package com.wpg.proxy;

import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Iterator;

import org.apache.log4j.Logger;

/** 
	Class to manage the  HTTP message headers and body content, which could be anything at this point 
	This class should be extended by HttpMessageRequest and HttpMessageResponse classes and never used directly
*/
public abstract class HttpMessage {
	public static final String HEADER_ACCEPT = "accept";
	public static final String HEADER_ACCEPT_CHARSET = "accept-charset";
	public static final String HEADER_ACCEPT_ENCODING = "accept-encoding";
	public static final String HEADER_ACCEPT_LANGUAGE = "accep-language";
	public static final String HEADER_CONNECTION = "connection";
	public static final String HEADER_CONTENT_LENGTH = "content-length";
	public static final String HEADER_CONTENT_TYPE = "content-type";
	public static final String HEADER_COOKIE = "cookie";
	public static final String HEADER_DATE = "date";
	public static final String HEADER_KEEP_ALIVE = "keep-alive";
	public static final String HEADER_PROXY_CONNECTION = "proxy-connection";
	public static final String HEADER_REFERER = "referer";
	public static final String HEADER_SERVER = "server";
	public static final String HEADER_SET_COOKIE = "set-cookie";
	public static final String HEADER_TRANSFER_ENCODING = "transfer-encoding";
	public static final String HEADER_USER_AGENT = "user-agent";

	protected Logger logger = Logger.getLogger(HttpMessage.class);

	protected byte[] body;
	protected ByteBuffer data;
	protected String fromHost;
	protected int fromPort;
	protected String toHost;
	protected int toPort = 80;
	protected String startLine;
	protected String protocol = "HTTP";
	protected String protocolVersion = "1.0";
	protected boolean isSecure = false;
	protected Map<String,List<String>> headers = null;
	protected Vector headerOrder = new Vector();

	/** Set body content to input byte[] */
	public void setBodyContent( byte[] b ) { body=b; updateContentLength(); }
	/** Add byte[] to body content */
	public void addToBody( byte[] b, int s ) { 
		if( body != null ) {
			byte[] tmp = new byte[ body.length + s ];
			System.arraycopy(body, 0, tmp, 0, body.length);
			System.arraycopy(b, 0, tmp, body.length, s);
			body=tmp;
		} else {
			body = new byte[s];
			System.arraycopy(b, 0, body, 0, s);
		}
		updateContentLength();
	}
	/** Update the content length header to the size of the new body */
	protected void updateContentLength() {
		Vector v = new Vector();
		v.addElement(body.length);
		//setHeader(HEADER_CONTENT_LENGTH, v);
	}
	/** Get raw content as an Array of Bytes */
	public byte[] getBodyContent(){ return body; }
	/** Get the content as a Stream of Bytes */
	public java.io.ByteArrayInputStream getBodyContentStream() { return new java.io.ByteArrayInputStream(body); }

	/** Get entire message as a ByteBuffer containing the header and body if any */
	public ByteBuffer getData() { 
		//TODO process headers then produce the results into a ByteBuffer
		return data;
	}

	/** Get From Host, or the host initiating this request */
	public String getFromHost() { return fromHost; }
	/** Set From Host, or the host initiating this request */
	public void setFromHost( String h ) { fromHost=h; }
	/** Get From Port, or the port on the host initiating this request */
	public int getFromPort() { return fromPort; }
	/** Set From Port, or the port on the host initiating this request */
	public void setFromPort( int p ) { fromPort=p; }
	/** Get To Host, or the host which is the target of this request */
	public String getToHost() { return toHost; }
	/** Set To Host, or the host which is the target of this request */
	public void setToHost( String h ) { toHost=h; }
	/** Get To Port, or the port on the host which is the target of this request */
	public int getToPort() { return toPort; }
	/** Set To Port, or the port on the host which is the target of this request */
	public void setToPort( int p ) { toPort=p; }

	/** Get the Start Line of the Message */
	public String getStartLine() { return startLine; }
	/** Set the Start Line of the Message */
	public abstract void setStartLine(String s);
	/** Get the protocol of this request as a string */
	public String getProtocol() { return protocol; }
	/** Set the protocol of this request */
	public void setProtocol( String s ) { protocol=s; }
	/** Get the protocol version of this request as a string */
	public String getVersion() { return protocolVersion; }
	/** Set the protocol version of this request */
	public void setVersion( String s ) { protocolVersion=s; }

	/** Get All Headers as a Map */
	public Map<String,List<String>> getHeaders() { return headers; }
	/** Set All Headers from a Map*/
	public void setHeaders( Map m ) { 
		headers = new Hashtable();
		headerOrder = new Vector();
		Iterator it = m.keySet().iterator();
		while( it.hasNext() ) {
			String key = ((String) it.next()).toLowerCase();
			List roList = (List) m.get(key);
			Vector items = new Vector(roList.size());
			items.addAll(roList);
			logger.trace("Adding Header: "+ key +"["+ items +"]");
			if( key != null) setHeader(key, items);
		}
	}
	/** Get a specific Header as a List */
	public List<String> getHeaderValues( String header ) { return headers.get(header); }
	/** Set a specific Header from a List */
	public void setHeader( String s, Vector l ) { 
		s = s.toLowerCase();
		if(headers == null)
			headers = new Hashtable();
		headers.put(s,l); 
		if( ! headerOrder.contains(s) ) headerOrder.addElement(s);
	}
	/** Add to a specific Header from a String */
	public void addHeader( String s, String item ) { 
		s = s.toLowerCase();
		if(headers == null)
			headers = new Hashtable();
		if( headers.get(s) == null ) {
			headers.put(s,new Vector());
			headerOrder.addElement(s);
		}
		((Vector)headers.get(s)).addElement(item); 
		logger.trace("Adding Header: "+ s +"["+ item +"]");
		if(s.toLowerCase().equals("host")) {
			if( item.indexOf(':') == -1 ) {
				setToHost(item);
			} else {
				StringTokenizer st = new StringTokenizer( item, ":");
				setToHost( st.nextToken() );
				setToPort( new Integer(st.nextToken()) );
			}
		}
	}
	/** Add Headers from a Vector of raw lines */
	public void setHeaders( Vector v ) {
		for(int i=0; i< v.size(); i++) {
			String line = (String)v.elementAt(i);
			StringTokenizer st = new StringTokenizer( line, ":");
			String flag= st.nextToken().toLowerCase();
			addHeader(flag, line.substring(flag.length()+2) );
			/*
			st = new StringTokenizer( line.substring(flag.length()+2), ", ");
			while( st.hasMoreTokens() ) {
				addHeader(flag, st.nextToken() );
			}*/
		}
	}
	/** Return a String with the headers printed in their RFC compliant final version */
	public String getHeadersAsString() {
		StringBuffer sb = new StringBuffer();
		for( int j=0; j< headerOrder.size(); j++) {
			String key = (String) headerOrder.elementAt(j);
			sb.append(key+": ");
			Vector items = (Vector) headers.get(key);
			for(int i=0; i< items.size(); i++) {
				Object item = (Object) items.elementAt(i);
				sb.append(item.toString() +((i+1)<items.size()?", ":"\r\n") );
			}
		}
		//sb.append("\r\n");
		logger.trace("Header: \""+ sb.toString() +"\"");
		return sb.toString();
	}
	/** Return true|false if the content length header is set */
	public boolean isContentLengthSet() {
		return headers.containsKey(HEADER_CONTENT_LENGTH);
	}
	/** Get the size of the stated content length */
	public int getContentLength() {
		if( !isContentLengthSet() )
			return -1;
		return new Integer((String) ( (Vector) headers.get(HEADER_CONTENT_LENGTH)).elementAt(0));
	}


}
