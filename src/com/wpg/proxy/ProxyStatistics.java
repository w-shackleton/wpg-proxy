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
import java.text.NumberFormat;

import org.apache.log4j.Logger;

/** ProxyStatistics object is used to track statistics collected during the proxy execution to report to the user via an HTTP request directed to the proxy itself, rather than meant for a remote host or port */
public class ProxyStatistics {
    private static Logger logger = Logger.getLogger(ProxyStatistics.class);
    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int STOPPED = 2;
    
    private ProxyStatistics(){};
    private final static ProxyStatistics singleton = new ProxyStatistics();
    public static ProxyStatistics getSingleton() {
        return singleton;
    }
    
    /*number of successful and failed transactions processed by this proxy since init or reset*/
    private long successCount = 0;
    private long failureCount = 0;
    private long stoppedCount = 0;
    private double minDuration = -1;
    private double maxDuration = 0.0;
    private double sumDuration = 0.0;
    private long cntDuration = 0;
    private Vector<Double> durations = new Vector<Double>();
    private double stdDevDuration = -1; //this is never the case because stddev is the sqrt of variance, thus it tells us to init this
    private Proxy proxy = null;
    private String title = "WPG Proxy Statistics";
    
    /**reset all counters to init values*/
    public void reset() {
        successCount = 0;
        failureCount = 0;
        stoppedCount = 0;
        minDuration = -1;
        maxDuration = 0.0;
        sumDuration = 0.0;
        cntDuration = 0;
        durations.clear();
        stdDevDuration = -1;
    }
    
    /**set title of web pages returned*/
    public void setTitle( String t ) { title=t; }
    /**get title of web pages returned*/
    public String getTitle() { return title; }
    
    /**get successful transaction count*/
    public long getSuccessTransactions() { return successCount; }
    /**get failed transaction count*/
    public long getFailureTransactions() { return failureCount; }
    /**get stopped transaction count, this is a stop due to one of the HttpMessageProcessor rules denying the continuation of the exchange*/
    public long getStoppedTransactions() { return stoppedCount; }
    
    /**increment transaction count*/
    public synchronized void incrementTransactionCount( int status ) throws IllegalArgumentException {
        switch(status) {
            case SUCCESS:	successCount++; break;
            case FAILURE:	failureCount++; break;
            case STOPPED:	stoppedCount++; break;
            default:	throw new IllegalArgumentException("Transaction Status of: "+ status +" Unrecognized!");
        }
    }
    
    /**get transaction count*/
    public long getTransactionCount() { return successCount + failureCount + stoppedCount; }
    
    /**add a duration measurement to the collection of statistics*/
    public synchronized void addDuration( double duration ) {
        if( minDuration == -1 || duration < minDuration ) minDuration = duration;
        if( duration > maxDuration ) maxDuration = duration;
        sumDuration += duration;
        cntDuration++;
        durations.addElement(duration);
        stdDevDuration=-1; //we set this to -1 in order to figure out we need to recalculate this, because a sqrt is never negative ;)
    }
    
    public double getDurationCnt() { return cntDuration; }
    public double getDurationMin() { return minDuration; }
    public double getDurationMax() { return maxDuration; }
    public double getDurationAvg() { return sumDuration/cntDuration; }
    /**
     * Calculate Standard Deviation as the SQRT of variance based on the formula from:<br>
     * <a href="http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Algorithm_II">WIKI Variance Calculation Page: Algorithm II</a><p>
     * <pre>
     * double mean = getDurationAvg();
     * double sum = 0.0;
     * for( int i=0; i< durations.size(); i++ ) {
     * double duration = durations.elementAt(i);
     * sum += Math.pow(duration - mean, 2);
     * }
     * stdDevDuration = Math.sqrt( sum / (durations.size() -1) );
     * </pre><p>
     * note: Future calculations of this may change based on the whims of man<br>
     */
    public double getDurationStdDev() {
        if( stdDevDuration < 0 ) {
            double mean = getDurationAvg();
            double sum = 0.0;
            for( int i=0; i< durations.size(); i++ ) {
                double duration = durations.elementAt(i);
                sum += Math.pow(duration - mean, 2);
            }
            stdDevDuration = Math.sqrt( sum / (durations.size() -1) );
        }
        return stdDevDuration;
    }
    
    /**Set Proxy object to query*/
    public void setProxy( Proxy p ) { proxy = p; }
    /**Get Proxy for this statistic collector*/
    public Proxy getProxy() { return proxy; }
    
    /**Get the html page for this request*/
    public String getHTMLPage() {
        StringBuffer sb = new StringBuffer("<html><head><title>"+ getTitle() +"</title></head><body>\r\n");
        sb.append("<H1>"+ getTitle() +":</H1>\r\n");
        sb.append("Number of Transactions Processed Total: <b>"+ getTransactionCount() +"</b><br>\r\n");
        sb.append("<ul>\r\n");
        sb.append("<li>Completed Successfully: <b>"+ getSuccessTransactions() +"</b></li>\r\n");
        sb.append("<li>Stopped Due to Processor: <b>"+ getStoppedTransactions() +"</b></li>\r\n");
        sb.append("<li>Failed Due To Errors: <b>"+ getFailureTransactions() +"</b></li>\r\n");
        sb.append("</ul><br>\r\n");
        sb.append("Transaction Statistics:\r\n");
        sb.append("<ul>\r\n");
        NumberFormat form = NumberFormat.getInstance();
        form.setMaximumFractionDigits(3);
        form.setMinimumFractionDigits(3);
        sb.append("<li>Minimum Transaction Time: <b>"+ form.format(getDurationMin()) +"</b></li>\r\n");
        sb.append("<li>Average Transaction Time: <b>"+ form.format(getDurationAvg()) +"</b></li>\r\n");
        sb.append("<li>Maximum Transaction Time: <b>"+ form.format(getDurationMax()) +"</b></li>\r\n");
        sb.append("<li>Transaction StdDev: <b>"+ form.format(getDurationStdDev()) +"</b></li>\r\n");
        sb.append("</ul><br>\r\n");
        sb.append("Registered Processors and Handlers:\r\n");
        sb.append("<ul>\r\n");
        sb.append("<li>Request Processors Registered: <b>"+ ProxyRegistry.getRequestProcessors().size() +"</b></li>\r\n");
        sb.append("<li>Message Handlers Registered: <b>"+ ProxyRegistry.getHandlers().size() +"</b></li>\r\n");
        sb.append("<li>Response Processors Registered: <b>"+ ProxyRegistry.getResponseProcessors().size() +"</b></li>\r\n");
        sb.append("</ul><br>\r\n");
        sb.append("</body></html>\r\n");
        return sb.toString();
    }
}
