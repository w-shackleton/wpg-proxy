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

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

import java.util.*;
import java.security.*;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.security.cert.*;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509TrustManager;
//import com.sun.net.ssl.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Main Proxy class which has a test main method and is the base class for all proxy handling
 */
public class Proxy extends Thread {
    private final static Logger logger = Logger.getLogger(Proxy.class);
    private ProxyStatistics stats = ProxyStatistics.getSingleton();
    private InetAddress inetAddr = null;
    private int port = 8080;
    private int securePort = 14111;
    private int backlog = 50;
    private int socketTimeout = 30000;
    private ServerSocketChannel ssChannel;
    private Selector selector;
    private Selector secureSelector;
    private Charset utf8 = Charset.forName("UTF-8");

    private boolean running=false;
    /** is the server running? */
    public boolean isRunning() { return running; }
    /** stop the proxy server */
    public void shutdown() { running=false; }

    
   
    
    
    
    /** Creates a new Proxy with ssl support */
    public Proxy( InetAddress inetAddr, int port, int backlog, String keyfile, char[] spass, char[] kpass ) {
        setInetAddress( inetAddr );
        setPort( port );
        setBacklog( backlog );
        ProxyRegistry.setKeystoreFilename( keyfile );
        ProxyRegistry.setKeystorePassword( spass );
        ProxyRegistry.setKeystoreKeysPassword( kpass );
        stats.setProxy(this);
    }
    
    /** Creates a new Proxy without ssl support */
    public Proxy( InetAddress inetAddr, int port, int backlog ) {
        setInetAddress( inetAddr );
        setPort( port );
        setBacklog( backlog );
        stats.setProxy(this);
    }
    
    /** Set the address to listen for new requests on */
    public void setInetAddress( InetAddress ia ) { inetAddr=ia; }
    /** Set the port to listen for new requests on */
    public void setPort( int i ) { port=i; }
    /** Set the backlog, or number of awaiting requests to queue */
    public void setBacklog( int i ) { backlog=i; }
    /** Get the address to listen for new requests on */
    public InetAddress getInetAddress() { return inetAddr; }
    /** Get the port to listen for new requests on */
    public int getPort() { return port; }
    /** Get the backlog, or number of awaiting requests to queue */
    public int getBacklog() { return backlog; }

    
    
    public void run(){
        logger.info("Proxy started on: "+ inetAddr.toString() +":"+ port);
        running=true;
        try {
            ssChannel = ServerSocketChannel.open();
            ssChannel.configureBlocking(false);
            ssChannel.socket().bind( new InetSocketAddress(inetAddr,port), backlog );
            selector = Selector.open();
            secureSelector = Selector.open();
            ssChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch( IOException e ) {
            logger.error("Error while listening for requests: "+ e, e);
            logger.fatal("Shutting down due to previous error!");
            shutdown();
        }
        new Thread(new Runnable() {
            public void run() {
                while(running){
                    try{
                        selector.select();
                    }catch(IOException e) {
                        logger.error("Error selecting next available connection: "+ e,e);
                        shutdown();
                        break;
                    }
                    
                    Set readyKeys = selector.selectedKeys();
                    Iterator it = readyKeys.iterator();
                    while(it.hasNext()) {
                        SelectionKey key = (SelectionKey) it.next();
                        it.remove();
                        try {
                            new ProxyProcessor(false,inetAddr,key);
                        } catch( IOException e ) {
                            logger.error("Error processing client: "+ key +" Exception: "+e,e);
                            key.cancel();
                            try { key.channel().close(); } catch(IOException ioe) {}
                        }
                    }
                }
            }
        },"Unsecure cons thread").start();
        
        new Thread(new Runnable() {
            public void run() {
                while(running){
                    try{
                        secureSelector.select();
                    }catch(IOException e) {
                        logger.error("Error selecting next available connection: "+ e,e);
                        shutdown();
                        break;
                    }
                    Set readyKeys = secureSelector.selectedKeys();
                    Iterator it = readyKeys.iterator();
                    while(it.hasNext()) {
                        SelectionKey key = (SelectionKey) it.next();
                        it.remove();
                        try {
                            new ProxyProcessor(true,inetAddr,key);
                        } catch( IOException e ) {
                            logger.error("Error processing client: "+ key +" Exception: "+e,e);
                            key.cancel();
                            try { key.channel().close(); } catch(IOException ioe) {}
                        }
                    }
                    
                }
            }
        },"Secure conns thread").start();
    }
    

    
    
    
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger","DEBUG, stdout");
        props.setProperty("log4j.appender.stdout","org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.stdout.layout","org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.stdout.layout.ConversionPattern","%5p [%t] (%F:%L) - %m%n");
        PropertyConfigurator.configure(props);
        
        try {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
            File keyFile = null;
            try {
                keyFile = File.createTempFile("proxy","keystore");
                keyFile.deleteOnExit();
                DataInputStream keyIS = new DataInputStream(logger.getClass().getResourceAsStream("/com/wpg/exproxy-keystore") );
                FileOutputStream fo = new FileOutputStream( keyFile );
                byte[] b = new byte[1];
                while( keyIS.read(b,0,1) != -1 )
                    fo.write(b,0,1);
                keyIS.close();
                fo.close();
                logger.debug("proxy key extracted for ssl support");
            } catch( Exception ex ) {
                logger.warn("Error creating a temporary file for proxy keystore ssl use: Exception: "+ ex);
            }
            Proxy proxy = new Proxy( java.net.InetAddress.getByName("127.0.0.1"), 8081, 50, keyFile.getPath(), "spassword".toCharArray(), "kpassword".toCharArray() );
            //Proxy proxy = new Proxy( java.net.InetAddress.getByName("192.168.0.11"), 8080, 50, keyFile.getPath(), "spassword".toCharArray(), "kpassword".toCharArray() );
            ProxyRegistry.enableStatusBrowser(true);
            proxy.start();
        } catch( java.net.UnknownHostException e ) {
            logger.fatal("Error resolving host: "+ e,e);
        } catch( Exception e ) {
            logger.fatal("Error: "+ e,e);
        }
    }
}


