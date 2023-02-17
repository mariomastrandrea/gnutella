package coms487.hw4.threads;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import coms487.hw4.model.GnutellaLog;
import coms487.hw4.model.Servent;

public class NeighborsMonitor extends Thread 
{	
	private static final int MONITOR_PERIOD = 60; // seconds
	
	private final Map<String,Servent> neighbors;
	private final GnutellaLog gnutellaLog;
	
	
	public NeighborsMonitor(Map<String, Servent> neighbors, GnutellaLog gnutellaLog) {
		this.neighbors = neighbors;
		this.gnutellaLog = gnutellaLog;
	}
	
	@Override
	public void run() {
		// periodically check for unresponsive neighbors 
		while(true) {
			// sleep for 1 min
			try {
				Thread.sleep(MONITOR_PERIOD * 1000);
			} 
			catch (InterruptedException ie) {
				System.err.println("An exception occurred during thread sleep in NeighborsMonitor");
				ie.printStackTrace();
				continue;
			}
			
			this.checkUnresponsiveNeighbors();
		}
	}

	private void checkUnresponsiveNeighbors() {
		synchronized (this.neighbors) {
			long currentTime = System.currentTimeMillis();
			Collection<String> neighborsToDiscard = new HashSet<>();
			
			// collect neighbors to discard
			for(Servent neighbor : this.neighbors.values()) {
				// discard disconnected neighbors
				if (!neighbor.isConnected()) {
					neighborsToDiscard.add(neighbor.toString());
					continue;
				}
				
				long lastResponseTime = neighbor.getLastResponseTime();
				long elapsedSeconds = (currentTime-lastResponseTime)/1000;
				
				// discard neighbors that are unresponsive for more than 3 times the ping period
				if(elapsedSeconds > 3 * PeriodicalPingSender.RANDOM_PERIOD) {
					neighborsToDiscard.add(neighbor.toString());
				}
			}
			
			// * delete neighbors *
			for(String neighborId : neighborsToDiscard) {
				this.neighbors.remove(neighborId);
				this.gnutellaLog.append("- Deleted unresponsive neighbor %s", neighborId);
			}
		}
	}
}
