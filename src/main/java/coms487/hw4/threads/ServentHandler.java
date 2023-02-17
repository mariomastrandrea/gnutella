package coms487.hw4.threads;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import coms487.hw4.model.FileMatch;
import coms487.hw4.model.FilesManager;
import coms487.hw4.model.GnutellaLog;
import coms487.hw4.model.GnutellaManager;
import coms487.hw4.model.Servent;
import coms487.hw4.model.packets.Announcement;
import coms487.hw4.model.packets.GnutellaPacket;
import coms487.hw4.model.packets.MessageId;
import coms487.hw4.model.packets.Ping;
import coms487.hw4.model.packets.Pong;
import coms487.hw4.model.packets.Query;
import coms487.hw4.model.packets.QueryHit;
import coms487.hw4.model.packets.GnutellaPacket.PacketType;
import javafx.application.Platform;
import coms487.hw4.model.packets.HitEntry;

import static coms487.hw4.model.Utilities.*;


public class ServentHandler extends Thread 
{	
	private final Servent servent;

	private final InetAddress localAddress;
	private final int localTcpPort;
	private final int fileSharingTcpPort;
	private final FilesManager filesManager;
	
	private final Map<String, Servent> neighbors;
	private final GnutellaLog gnutellaLog;
	
	private final Set<MessageId> seenPings;
	private final Map<MessageId, Servent> expectedPongReceivers;
	private final Set<MessageId> seenQueries;
	private final Map<MessageId, Servent> expectedQueryHitReceivers;

	private final MessageId requestedQuery;
	private final Collection<FileMatch> fileMatches;
	
	private Boolean announced;
	
	
	public ServentHandler(Servent servent, boolean announced, InetAddress localAddress, 
			int localTcpPort, int fileSharingTcpPort, FilesManager filesManager, 
			Map<String, Servent> neighbors, GnutellaLog gnutellaLog, Set<MessageId> seenPings, 
			Map<MessageId, Servent> expectedPongReceivers, Set<MessageId> seenQueries, 
			Map<MessageId, Servent> expectedQueryHitReceivers, MessageId requestedQuery,
			Collection<FileMatch> fileMatches) 
	{
		this.servent = servent;
		this.localAddress = localAddress;
		this.localTcpPort = localTcpPort;
		this.fileSharingTcpPort = fileSharingTcpPort;
		this.filesManager = filesManager;
		this.neighbors = neighbors;
		this.gnutellaLog = gnutellaLog;
		this.seenPings = seenPings;
		this.expectedPongReceivers = expectedPongReceivers;
		this.announced = announced;
		this.seenQueries = seenQueries;
		this.expectedQueryHitReceivers = expectedQueryHitReceivers;
		this.requestedQuery = requestedQuery;
		this.fileMatches = fileMatches;
	}
	
	@Override
	public void run() {
		while(true) {
			// * listen to new packets from this neighbor *
			GnutellaPacket receivedPacket = this.servent.receive();
			
			if(receivedPacket == null) {
				// network error or wrong packet format or neighbor turned off
				this.servent.disconnect();
				this.gnutellaLog.append("Disconnected from servent %s", this.servent);
				break;
			}
			
			// * received a new packet from this neighbor *
			
			if(receivedPacket.getPacketType() != PacketType.ANNOUNCEMENT)  
				this.gnutellaLog.append("> Received %s %s from %s", 
						receivedPacket.getPacketType(), receivedPacket.getMessageId(), this.servent);
			
			boolean continueToListen = true;
			
			switch (receivedPacket.getPacketType()) {
				case PING:
					continueToListen = this.manage((Ping)receivedPacket);
					if(!continueToListen) return;
					break;
					
				case PONG:
					continueToListen = this.manage((Pong)receivedPacket);
					if(!continueToListen) return;
					break;
					
				case QUERY:
					continueToListen = this.manage((Query)receivedPacket);
					if(!continueToListen) return;
					break;
					
				case QUERYHIT:
					continueToListen = this.manage((QueryHit)receivedPacket);
					if(!continueToListen) return;
					break;
					
				case ANNOUNCEMENT:
					Announcement announcement = (Announcement)receivedPacket;

					// (in case of announcement, print the payload info instead)
					this.gnutellaLog.append("> Received %s %s from %s", 
							receivedPacket.getPacketType(), receivedPacket.getMessageId(), 
							String.format("%s:%d", announcement.getNodeAddress().getHostAddress(), announcement.getNodePort()));
					
					continueToListen = this.manage(announcement);
					if(!continueToListen) return;
					break;
			}
		}
	}
	
	private boolean manage(Announcement receivedPacket) {
		// check if this neighbor has already announced itself
		synchronized (this.announced) {
			if (this.announced) {
				this.gnutellaLog.append("Error: neighbor %s is trying to send another announcement", this.servent);
				return true; // ignore the packet
			}
			
			this.announced = true;
		}
		
		synchronized (this.neighbors) {
			// check if already exist this announced neighbor
			if(this.neighbors.containsKey(this.servent.toString())) {
				this.gnutellaLog.append("Neighbor %s has already been added", this.servent);
				this.servent.disconnect();
				return false;
			}
			
			// * override new neighbor info from announcement *
			// -> in this way, the neighbor is always identified by the (address, port) sent in its announcement, regardless of its socket
			this.servent.setAddress(receivedPacket.getNodeAddress());
			this.servent.setTcpPort(receivedPacket.getNodePort());
			
			// * reply with a Pong to the same neighbor *
			Pong pong = new Pong(receivedPacket.getMessageId(), 
					this.localAddress, this.localTcpPort, this.filesManager.getNumSharedFiles());
			
			// send Pong
			boolean sentPong = this.servent.send(pong);
			if(!sentPong) {
				// disconnect from this neighbor
				if(this.servent.disconnect())
					this.gnutellaLog.append("Disconnected from servent %s", this.servent);
				return false;
			}
			
			this.gnutellaLog.append("> Sent PONG %s back to %s", pong, this.servent);
			
			// register response time
			this.servent.setLastResponseTime(System.currentTimeMillis());
			
			// * add new neighbor *
			this.neighbors.put(this.servent.toString(), this.servent);
		}
		
		this.gnutellaLog.append("+ Added new neighbor %s", this.servent);
		return true;
	}

	private boolean manage(Ping receivedPing) {
		// * check that this servent is already announced *
		synchronized (this.announced) {
			if(!this.announced) {
				this.gnutellaLog.append("Error: node %s sent a PING without being announced", this.servent);
				
				this.servent.disconnect();
				return false;
			}
		}
		
		// * check if I have already seen this Ping: if so, discard it *
		if(hasBeenSeen(receivedPing, this.seenPings)) {
			this.gnutellaLog.append("Already seen Ping %s", receivedPing);
			return true; // discard packet 
		}
		
		// * reply with a Pong to the same neighbor *
		Pong pong = new Pong(receivedPing.getMessageId(), 
				this.localAddress, this.localTcpPort, this.filesManager.getNumSharedFiles());
		
		boolean sentPong = this.servent.send(pong);
		if(!sentPong) {
			this.gnutellaLog.append("Error sending PONG %s back to %s", pong, this.servent);
			
			// disconnect from this neighbor
			if(this.servent.disconnect())
				this.gnutellaLog.append("Disconnected from servent %s", this.servent);
			return false;
		}
		
		this.gnutellaLog.append("> Sent PONG %s back to %s", pong, this.servent);
		
		// decrement ttl
		receivedPing.decrementTtl();
		// discard the Ping if TTL is 0
		if(receivedPing.getTtl() == 0) return true; 
		
		// * save expected Pong *
		synchronized (this.expectedPongReceivers) {
			if (this.expectedPongReceivers.size() >= GnutellaManager.DEFAULT_CAPACITY) {
				// remove one random MessageId
				removeOneRandom(this.expectedPongReceivers, GnutellaManager.DEFAULT_CAPACITY);
			}
			
			this.expectedPongReceivers.put(receivedPing.getMessageId(), this.servent);
		}
		
		// * forward Ping to the other neighbors *
		synchronized (this.neighbors) {
			for(Servent neighbor : this.neighbors.values()) {
				// skip the Ping's sender and the disconnected ones 
				if(neighbor.equals(this.servent) || !neighbor.isConnected()) continue;
				
				boolean forwardedPing = neighbor.send(receivedPing);
				if (!forwardedPing) {
					this.gnutellaLog.append("Error forwarding PING %s to %s", receivedPing, neighbor);
					
					// disconnect from that neighbor
					if(neighbor.disconnect())
						this.gnutellaLog.append("Disconnected from neighbor %s", neighbor);
					return false;
				}
				
				this.gnutellaLog.append("> Forwarded PING %s to %s", receivedPing, neighbor);
			}
		}
		
		return true;
	}

	private boolean manage(Pong receivedPong) {
		// * check that this servent is already announced *
		synchronized (this.announced) {
			if(!this.announced) {
				this.gnutellaLog.append("Error: node %s sent a PONG without being announced", this.servent);
				
				this.servent.disconnect();
				return false;
			}
		}
		
		long responseTime = System.currentTimeMillis();
		
		// * retrieve Servent *
		Servent pongServent = new Servent(receivedPong.getNodeAddress(), receivedPong.getNodePort());
		
		// * save new neighbor or update the existing one *
		synchronized (this.neighbors) {
			if(this.neighbors.containsKey(pongServent.toString())) {
				// just update last pong time
				this.neighbors.get(pongServent.toString()).setLastResponseTime(responseTime);
			}
			// new neighbor: save it if there is enough space 
			else if (this.neighbors.size() < GnutellaManager.DEFAULT_CAPACITY) {
				pongServent.setLastResponseTime(responseTime);
				
				// open TCP connection with the new node
				if (!pongServent.connect()) {
					this.gnutellaLog.append("An error occurred connecting to %s", pongServent);
				}
				else {
					this.gnutellaLog.append("Connected to node %s", pongServent);
				
					// * join new neighbor and handle it *
					boolean joined = this.joinNewServent(pongServent);
					
					if(!joined) {
						// disconnect from that node
						if(pongServent.disconnect())
							this.gnutellaLog.append("Disconnected from node %s", pongServent);
					}
				}
			}
		}
		
		Servent pongReceiver;
		
		// * decide if forwarding or not *
		synchronized (this.expectedPongReceivers) {
			if(!this.expectedPongReceivers.containsKey(receivedPong.getMessageId())) {
				// it was directed to me (or error Pong) -> don't forward
				return true;
			}
			
			// decrement ttl
			receivedPong.decrementTtl();
			if(receivedPong.getTtl() == 0) return true;
			
			pongReceiver = this.expectedPongReceivers.get(receivedPong.getMessageId());
		}
		
		// * forward Pong *
		if(pongReceiver.isConnected()) {
			boolean pongForwarded = pongReceiver.send(receivedPong);
			
			if(!pongForwarded) {
				this.gnutellaLog.append("Error forwarding PONG %s to %s", receivedPong, pongReceiver);
				
				if(pongReceiver.disconnect())
					this.gnutellaLog.append("Disconnected from neighbor %s", pongReceiver);
			}
			
			this.gnutellaLog.append("> Forwarded PONG %s to %s", receivedPong, pongReceiver);
		}
		else 
			this.gnutellaLog.append("Error forwarding PONG %s (%s disconnected)", receivedPong, pongReceiver);
		
		return true;
	}
	
	private boolean manage(Query receivedQuery) {
		// * check that this servent is already announced *
		synchronized (this.announced) {
			if(!this.announced) {
				this.gnutellaLog.append("Error: node %s sent a QUERY without being announced", this.servent);
				
				this.servent.disconnect();
				return false;
			}
		}
		
		// * check if I have already seen this Query: if so, discard it *
		if(hasBeenSeen(receivedQuery, this.seenQueries)) {
			this.gnutellaLog.append("Already seen Query %s", receivedQuery);
			return true; // discard packet 
		}
		
		// * check if the query matches any file *
		String queryString = receivedQuery.getQueryString();
		List<HitEntry> matchingFiles = this.filesManager.findMatchingFiles(queryString);
		
		if(!matchingFiles.isEmpty()) {
			// * reply with a QueryHit *
			QueryHit queryHit = new QueryHit(receivedQuery.getMessageId(), 
					this.localAddress, this.fileSharingTcpPort, matchingFiles);
			
			boolean queryHitSent = this.servent.send(queryHit);
			
			if(!queryHitSent) {
				this.gnutellaLog.append("Error sending QUERYHIT %s back to %s", queryHit, this.servent);
				
				// disconnect from this neighbor
				if(this.servent.disconnect())
					this.gnutellaLog.append("Disconnected from servent %s", this.servent);
				return false;
			}
			
			this.gnutellaLog.append("> Sent QUERYHIT %s back to %s", queryHit, this.servent);
		}
		
		// decrement ttl
		receivedQuery.decrementTtl();
		// discard the query if TTL is 0
		if(receivedQuery.getTtl() == 0) return true; 
				
		// * save expected QueryHit *
		synchronized (this.expectedQueryHitReceivers) {
			if (this.expectedQueryHitReceivers.size() >= GnutellaManager.DEFAULT_CAPACITY) {
				// remove one random MessageId
				removeOneRandom(this.expectedQueryHitReceivers, GnutellaManager.DEFAULT_CAPACITY);
			}
			
			this.expectedQueryHitReceivers.put(receivedQuery.getMessageId(), this.servent);
		}
				
		// * forward Query to other neighbors *
		synchronized (this.neighbors) {
			for(Servent neighbor : this.neighbors.values()) {
				// skip the Query's sender and the disconnected ones 
				if(neighbor.equals(this.servent) || !neighbor.isConnected()) continue;
						
				boolean forwardedQuery = neighbor.send(receivedQuery);
				if (!forwardedQuery) {
					this.gnutellaLog.append("Error forwarding QUERY %s to %s", receivedQuery, neighbor);
							
					// disconnect from that neighbor
					if(neighbor.disconnect())
						this.gnutellaLog.append("Disconnected from neighbor %s", neighbor);
					return false;
				}
						
				this.gnutellaLog.append("> Forwarded QUERY %s to %s", receivedQuery, neighbor);
			}
		}
				
		return true;
	}
	
	
	private boolean manage(QueryHit receivedQueryHit) {
		// * check that this servent is already announced *
		synchronized (this.announced) {
			if(!this.announced) {
				this.gnutellaLog.append("Error: node %s sent a QUERYHIT without being announced", this.servent);
				
				this.servent.disconnect();
				return false;
			}
		}
		
		Servent queryHitReceiver;
				
		// * check if this query has to be forwarded *
		synchronized (this.expectedQueryHitReceivers) {
			if(this.expectedQueryHitReceivers.containsKey(receivedQueryHit.getMessageId())) {
				// It has to be forwarded (it was not a query requested by me)
				receivedQueryHit.decrementTtl();
				if(receivedQueryHit.getTtl() == 0) return true;
				
				queryHitReceiver = this.expectedQueryHitReceivers.get(receivedQueryHit.getMessageId());
				
				// * forward QueryHit *
				if(queryHitReceiver.isConnected()) {
					boolean queryHitForwarded = queryHitReceiver.send(receivedQueryHit);
					
					if(!queryHitForwarded) {
						this.gnutellaLog.append("Error forwarding QUERYHIT %s to %s", receivedQueryHit, queryHitReceiver);
						
						if(queryHitReceiver.disconnect())
							this.gnutellaLog.append("Disconnected from neighbor %s", queryHitReceiver);
					}
					
					this.gnutellaLog.append("> Forwarded QUERYHIT %s to %s", receivedQueryHit, queryHitReceiver);
				}
				else 
					this.gnutellaLog.append("Error forwarding QUERYHIT %s (%s disconnected)", 
							receivedQueryHit, queryHitReceiver);
				
				return true;
			}
		}
				
		// * check if this query hit was directed to me or not: if so, save received data *
		synchronized (this.fileMatches) {
			if(!Objects.equals(this.requestedQuery, receivedQueryHit.getMessageId())) {	
				return true;	// it is not the queryHit I was searching for
			}
			
			// * this query hit is the one directed to me! *
			
			// create file matches from HitEntry(s)
			InetAddress nodeAddress = receivedQueryHit.getHitNodeAddress();
			int sharingFileTcpPort = receivedQueryHit.getNodeTcpPort();
			
			Collection<FileMatch> filesHits = new ArrayList<>();
			
			receivedQueryHit.getHits().forEach(hit -> {
				FileMatch match = new FileMatch(nodeAddress, sharingFileTcpPort, hit);
				filesHits.add(match);
			});
														
			// and now add them to the GUI list
			Platform.runLater(() -> {
				this.fileMatches.addAll(filesHits);
			});
		}
		
		return true;
	}
	
	private boolean hasBeenSeen(GnutellaPacket packet, Set<MessageId> seenPackets) {
		synchronized (seenPackets) {
			boolean seen = seenPackets.contains(packet.getMessageId());
			
			if(!seen) {
				if(seenPackets.size() >= GnutellaManager.DEFAULT_CAPACITY) {
					// remove a random MessageId
					removeOneRandom(seenPackets, GnutellaManager.DEFAULT_CAPACITY);
				}
				
				seenPackets.add(packet.getMessageId());
			}
			return seen;
		}
	}

	private boolean joinNewServent(Servent servent) {		
		Announcement announcement = new Announcement(
				this.localAddress, this.localTcpPort, this.filesManager.getNumSharedFiles());
		
		// * send announcement *
		boolean sent = servent.send(announcement);
			
		if(!sent) {
			this.gnutellaLog.append("Error sending Announcement %s to %s", announcement, servent);
			return false;
		}
				
		this.gnutellaLog.append("> Sent Announcement %s to %s", announcement, servent);
				
		// set timeout of 20 sec
		if(!servent.setTimeout(GnutellaManager.TIMEOUT_TO_JOIN)) {
			this.gnutellaLog.append("Error setting timeout to connect to node %s", servent);
			return false;
		}
		
		// * receive (hopefully) Pong *
		GnutellaPacket packet = servent.receive();

		if (packet == null) {
			this.gnutellaLog.append("Error receiving first Pong from %s", servent);
			return false;
		}
		
		// * check Pong *
		if (packet.getPacketType() != PacketType.PONG) {
			this.gnutellaLog.append("Error: received a different Packet rather than Pong to join %s", servent);
			return false;
		}
		
		Pong firstPong = (Pong)packet;
		
		// check Pong message id 
		if (!Objects.equals(firstPong.getMessageId(), announcement.getMessageId())) {
			this.gnutellaLog.append("Error: received a Pong with a different ID than Announcement");
			return false;
		}
		
		// check Pong payload 
		if(!Objects.equals(firstPong.getNodeAddress(), servent.getAddress()) ||
			firstPong.getNodePort() != servent.getTcpPort()) {
			this.gnutellaLog.append("Error: received a Pong from a different node than expected");
			return false;
		}
		
		// * Pong successfully received from new neighbor *
		this.gnutellaLog.append("> Received first Pong %s from %s", firstPong, servent);
		
		// unset timeout
		if(!servent.unsetTimeout()) {
			this.gnutellaLog.append("Error removing timeout after joining %s", servent);
			return false;
		}
	
		// * register NEW neighbor * (already synchronizing to neighbors)
		this.neighbors.put(servent.toString(), servent);
		
		this.gnutellaLog.append("+ Added new neighbor %s", servent);	
		
		// * handle new neighbor * 
		new ServentHandler(servent, true, this.localAddress, this.localTcpPort, 
				this.fileSharingTcpPort, this.filesManager, this.neighbors, this.gnutellaLog, 
				this.seenPings, this.expectedPongReceivers, this.seenQueries, 
				this.expectedQueryHitReceivers, this.requestedQuery, this.fileMatches).start();
		
		return true;
	}
}
