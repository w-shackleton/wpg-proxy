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
