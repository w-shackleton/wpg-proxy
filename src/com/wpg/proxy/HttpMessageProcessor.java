package com.wpg.proxy;

/** Process requests before they are sent or responses once they are received */
public interface HttpMessageProcessor {
    /** Continue to run next processors? */
    public boolean doContinue(HttpMessage input);
    /** Send this message? */
    public boolean doSend(HttpMessage input);
    /** Process this given message, returns the message after modification */
    public HttpMessage process( HttpMessage input );
}
