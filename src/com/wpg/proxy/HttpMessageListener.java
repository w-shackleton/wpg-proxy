package com.wpg.proxy;

/** Interface for a user defined HTTP Message Listener */
public interface HttpMessageListener {
	/** Failed to get a request */
	public void failed( Exception exception );
	/** Failed to get a response for the given request */
	public void failed( HttpMessageRequest request, Exception exception );
	/** Failed to send the given response for the given request back to the user */
	public void failed( HttpMessageResponse response, HttpMessageRequest request, Exception exception );
	/** Just received a request, this is called before the request is sent out but after the request processors are executed */
	public void received( HttpMessageRequest request);
	/** Just received a response for the given request, but have not yet returned it to the user */
	public void received( HttpMessageResponse response, HttpMessageRequest request);
}
