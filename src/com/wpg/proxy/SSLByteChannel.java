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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;

/**
 * Upgrade a ByteChannel for SSL.
 *
 * <p>
 * Change Log:
 * </p>
 * <ul>
 *  <li>v1.0.0 - First public release.</li>
 * </ul>
 *
 * <p>
 * This source code is given to the Public Domain. Do what you want with it.
 * This software comes with no guarantees or warranties.
 * Please visit <a href="http://perso.wanadoo.fr/reuse/sslbytechannel/">http://perso.wanadoo.fr/reuse/sslbytechannel/</a>
 * periodically to check for updates or to contribute improvements.
 * </p>
 * usage:
 * KeyStore ks = KeyStore.getInstance("JKS");
 * File kf = new File("keystore");
 * ks.load(new FileInputStream(kf), "storepassword".toCharArray());
 *
 * KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
 * kmf.init(ks, "keypassword".toCharArray());
 *
 * TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
 * tmf.init(ks);
 *
 * SSLContext sslContext = SSLContext.getInstance("TLS");
 * sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
 *
 * SSLEngine engine = sslContext.createSSLEngine();
 * engine.setUseClientMode(false);
 * engine.beginHandshake();
 *
 * SSLByteChannel sslByteChannel = new SSLByteChannel(channel, engine);
 *
 * @author David Crosson
 * @author david.crosson@wanadoo.fr
 * @version 1.0.0
 */
public class SSLByteChannel implements ByteChannel {
    private static Logger logger = Logger.getLogger(SSLByteChannel.class);
    private ByteChannel wrappedChannel;
    private boolean closed = false;
    private SSLEngine engine;
    
    private final ByteBuffer inAppData;
    private final ByteBuffer outAppData;
    
    private final ByteBuffer inNetData;
    private final ByteBuffer outNetData;
    
    
    /**
     * Creates a new instance of SSLByteChannel
     * @param wrappedChannel The byte channel on which this ssl channel is built.
     * This channel contains encrypted data.
     * @param engine A SSLEngine instance that will remember SSL current
     * context. Warning, such an instance CAN NOT be shared
     * between multiple SSLByteChannel.
     */
    public SSLByteChannel(ByteChannel wrappedChannel, SSLEngine engine) {
        this.wrappedChannel = wrappedChannel;
        this.engine = engine;
        
        SSLSession session = engine.getSession();
        inAppData  = ByteBuffer.allocate(session.getApplicationBufferSize());
        outAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        
        inNetData  = ByteBuffer.allocate(session.getPacketBufferSize());
        outNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }
    
    
    /**
     * Ends SSL operation and close the wrapped byte channel
     * @throws java.io.IOException May be raised by close operation on wrapped byte channel
     */
    public void close() throws java.io.IOException {
        if (!closed) {
            try {
                engine.closeOutbound();
                sslLoop(wrap());
                wrappedChannel.close();
            } finally {
                closed=true;
            }
        }
    }
    
    /**
     * Is the channel open ?
     * @return true if the channel is still open
     */
    public boolean isOpen() {
        return !closed;
    }
    
    /**
     * Fill the given buffer with some bytes and return the number of bytes
     * added in the buffer.<br>
     * This method may return immediately with nothing added in the buffer.
     * This method must be use exactly in the same way of ByteChannel read
     * operation, so be careful with buffer position, limit, ... Check
     * corresponding javadoc.
     * @param byteBuffer The buffer that will received read bytes
     * @throws java.io.IOException May be raised by ByteChannel read operation
     * @return The number of bytes read
     */
    public int read(java.nio.ByteBuffer byteBuffer) throws java.io.IOException {
        if (isOpen()) {
            try {
                SSLEngineResult r = sslLoop(unwrap());
            } catch(SSLException e) {
                logger.fatal("SSLException while reading: "+ e,e);
            } catch(ClosedChannelException e) {
                close();
            }
        }
        
        inAppData.flip();
        int posBefore = inAppData.position();
        byteBuffer.put(inAppData);
        int posAfter = inAppData.position();
        inAppData.compact();
        
        if (posAfter - posBefore > 0) return posAfter - posBefore ;
        if (isOpen())
            return 0;
        else
            return -1;
    }
    
    /**
     * Write remaining bytes of the given byte buffer.
     * This method may return immediately with nothing written.
     * This method must be use exactly in the same way of ByteChannel write
     * operation, so be careful with buffer position, limit, ... Check
     * corresponding javadoc.
     * @param byteBuffer buffer with remaining bytes to write
     * @throws java.io.IOException May be raised by ByteChannel write operation
     * @return The number of bytes written
     */
    public int write(java.nio.ByteBuffer byteBuffer) throws java.io.IOException {
        if (!isOpen()) return 0;
        int posBefore, posAfter;
        
        posBefore = byteBuffer.position();
        if (byteBuffer.remaining() < outAppData.remaining()) {
            outAppData.put(byteBuffer);  // throw a BufferOverflowException if byteBuffer.remaining() > outAppData.remaining()
        } else {
            while (byteBuffer.hasRemaining() && outAppData.hasRemaining()) {
                outAppData.put(byteBuffer.get());
            }
        }
        posAfter = byteBuffer.position();
        
        if (isOpen()) {
            try {
                while(true) {
                    SSLEngineResult r = sslLoop(wrap());
                    if (r.bytesConsumed() == 0 && r.bytesProduced()==0) break;
                };
            } catch(SSLException e) {
                logger.fatal("SSLException while reading: "+ e,e);
            } catch(ClosedChannelException e) {
                close();
            }
        }
        
        return posAfter - posBefore;
    }
    
    
    
    
    
    private SSLEngineResult unwrap() throws IOException, SSLException {
        while(wrappedChannel.read(inNetData) > 0) ;
        
        inNetData.flip();
        SSLEngineResult ser = engine.unwrap(inNetData, inAppData);
        inNetData.compact();
        
        return ser;
    }
    
    private SSLEngineResult wrap() throws IOException, SSLException {
        SSLEngineResult ser=null;
        
        outAppData.flip();
        ser = engine.wrap(outAppData,  outNetData);
        outAppData.compact();
        
        outNetData.flip();
        while(outNetData.hasRemaining()) wrappedChannel.write(outNetData); // TODO : To be enhanced (potential deadlock ?)
        outNetData.compact();
        
        return ser;
    }
    
    private SSLEngineResult sslLoop(SSLEngineResult ser) throws SSLException, IOException {
        if (ser==null) return ser;
        //log.finest(String.format("%s - %s\n", ser.getStatus().toString(), ser.getHandshakeStatus().toString()));
        logger.trace(ser.getStatus().toString() +" - "+ ser.getHandshakeStatus().toString());
        while(   ser.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED
                && ser.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch(ser.getHandshakeStatus()) {
                case NEED_TASK:
                    //Executor exec = Executors.newSingleThreadExecutor();
                    Runnable task;
                    while ((task=engine.getDelegatedTask()) != null) {
                        //exec.execute(task);
                        task.run();
                    }
                    // Must continue with wrap as data must be sent
                case NEED_WRAP:
                    ser = wrap();
                    break;
                case NEED_UNWRAP:
                    ser = unwrap();
                    break;
            }
        }
        switch(ser.getStatus()) {
            case CLOSED:
                logger.trace("SSLEngine operations finishes, closing the socket");
                try {
                    wrappedChannel.close();
                } finally {
                    closed=true;
                }
                break;
        }
        return ser;
    }
    
}
