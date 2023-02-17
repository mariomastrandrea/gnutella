package coms487.hw4.threads;

import static coms487.hw4.model.Utilities.removeOneRandom;

import java.util.Map;
import java.util.Set;

import coms487.hw4.model.GnutellaLog;
import coms487.hw4.model.GnutellaManager;
import coms487.hw4.model.Servent;
import coms487.hw4.model.packets.MessageId;
import coms487.hw4.model.packets.Ping;

public class PeriodicalPingSender extends Thread 
{
	private final static int SETUP_WAIT = 2; // seconds
	private final static int PING_AVG_PERIOD = 30; // seconds
	private final static int TOLERANCE = 5;	 // seconds
	public static final int RANDOM_PERIOD = computeRandomPeriod(PING_AVG_PERIOD, TOLERANCE);	// in seconds
	
	private final Map<String, Servent> neighbors;
	private final Set<MessageId> seenPings;
	private final GnutellaLog gnutellaLog;
	
	
	public PeriodicalPingSender(Map<String, Servent> neighbors, Set<MessageId> seenPings,
			GnutellaLog gnutellaLog) {
		this.neighbors = neighbors;
		this.seenPings = seenPings;
		this.gnutellaLog = gnutellaLog;
	}
		
	@Override
	public void run() {
		try {
			Thread.sleep(SETUP_WAIT);
		} 
		catch (InterruptedException ie) {
			System.err.println("Error occurred during first thread sleep in PeriodicalPingSender");
			ie.printStackTrace();
		}
		
		while(true) {
			// create a Ping at each loop
			Ping ping = new Ping();
			boolean sent = false;
			
			synchronized (this.neighbors) {
				// send the Ping to each (active) neighbor
				for(Servent neighbor : this.neighbors.values()) {
					// skip disconnected neighbors
					if(!neighbor.isConnected()) continue;
					
					if(!sent)
						this.gnutellaLog.append(""); // append a new line at the beginning of the Pings

					// * send Ping to this neighbor *
					boolean pingSent = neighbor.send(ping);
					
					if(!pingSent) {
						this.gnutellaLog.append("Error sending PING %s to %s", ping, neighbor);
						
						if(neighbor.disconnect())
							this.gnutellaLog.append("Disconnected from %s", neighbor);
						
						continue;
					}
					
					// * Ping sent *
					this.gnutellaLog.append("> Sent PING %s to %s", ping, neighbor);
					sent = true;
				}
			}
			
			if(sent) {				
				// Ping has been sent to at least one neighbor: add it to seen Pings set
				synchronized (this.seenPings) {
					if(this.seenPings.size() >= GnutellaManager.DEFAULT_CAPACITY) {
						// maximum capacity reached remove a random MessageId
						removeOneRandom(this.seenPings, GnutellaManager.DEFAULT_CAPACITY);
					}
					
					this.seenPings.add(ping.getMessageId());
				}
			}
			
			// wait 20 seconds
			try {
				Thread.sleep(RANDOM_PERIOD * 1000);	
			} 
			catch (InterruptedException ie) {
				System.err.println("An exception occurred during PeriodicalPingSender thread sleep");
				ie.printStackTrace();
			}
		}
	}
	
	private static int computeRandomPeriod(int avg, int tolerance) {
		double randomDeviation = 2.0 * tolerance * (Math.random() - 0.5);
		return (int)((double)avg + randomDeviation);
	}
}
