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

import org.apache.log4j.Logger;

/** ProxyStatistics object is used to track statistics collected during the proxy execution to report to the user via an HTTP request directed to the proxy itself, rather than meant for a remote host or port */
public class ProxyStatistics {
    private static Logger logger = Logger.getLogger(ProxyStatistics.class);
	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;
	public static final int STOPPED = 2;
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
		stdDevDuration=-1;
	}

	public double getDurationCnt() { return cntDuration; }
	public double getDurationMin() { return minDuration; }
	public double getDurationMax() { return maxDuration; }
	public double getDurationAvg() { return sumDuration/cntDuration; }
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
}
