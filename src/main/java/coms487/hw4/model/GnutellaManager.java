package coms487.hw4.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import coms487.hw4.model.packets.Announcement;
import coms487.hw4.model.packets.GnutellaPacket;
import coms487.hw4.model.packets.MessageId;
import coms487.hw4.model.packets.Pong;
import coms487.hw4.model.packets.Query;
import coms487.hw4.threads.FileSharing;
import coms487.hw4.threads.GnutellaListener;
import coms487.hw4.threads.NeighborsMonitor;
import coms487.hw4.threads.PeriodicalPingSender;
import coms487.hw4.threads.ServentHandler;
import coms487.hw4.model.packets.GnutellaPacket.PacketType;
import javafx.beans.value.WritableStringValue;
import static coms487.hw4.model.Utilities.*;


public class GnutellaManager 
{
	public static final int DEFAULT_CAPACITY = 1024;
	public static final int TIMEOUT_TO_JOIN = 20 * 1000; // 20 seconds
	
	private InetAddress localAddress;	// IPv4 address of this Gnutella node: (address, port) is the unique ID in the Gnutella Network
	private final int localTcpPort;		// TCP port where this Gnutella node will be listening to for receiving packets 
	private final int fileSharingTcpPort;	// TCP port where other Gnutella nodes can download files from 
	
	private FilesManager filesManager;
	private final Map<String, Servent> neighbors;	// neighbors nodes by ID (x.x.x.x:y)
	private GnutellaLog gnutellaLog;				// log object able to print logs on GUI
	
	private final Set<MessageId> seenPings;		// set of Pings' IDs already seen by this node
	private final Map<MessageId, Servent> expectedPongsReceivers;	// servents who expect to receive the Pong
	private final Set<MessageId> seenQueries;	// set of Queries' IDs already seen by this node
	private final Map<MessageId, Servent> expectedQueryHitReceivers; // servents who may expect to receive the QueriHit
	
	private MessageId requestedQuery;		// Query's ID requested by *this* node
	private Collection<FileMatch> fileMatches;	// list of file matches collected from the Gnutella Network
	
	
	public GnutellaManager(String serventAddress, int serventTcpPort, int fileSharingTcpPort) {
		try {
			this.localAddress = InetAddress.getByName(serventAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.localTcpPort = serventTcpPort;
		this.fileSharingTcpPort = fileSharingTcpPort;
		this.filesManager = new FilesManager();
		this.neighbors = new HashMap<>();
		this.gnutellaLog = null;
		this.seenPings = new HashSet<>(DEFAULT_CAPACITY);
		this.expectedPongsReceivers = new HashMap<>(DEFAULT_CAPACITY);
		this.seenQueries = new HashSet<>(DEFAULT_CAPACITY);
		this.expectedQueryHitReceivers = new HashMap<>(DEFAULT_CAPACITY);
		this.requestedQuery = new MessageId();	// random initial id
		this.fileMatches = null;
	}

	public void initGnutellaLog(WritableStringValue logString, Collection<FileMatch> fileMatches) {
		this.gnutellaLog = new GnutellaLog(logString);
		this.fileMatches = fileMatches;
	}

	public boolean startup(InetAddress existingNodeAddress, Integer existingNodePort) {
		// check that the log object has been initialized
		if (this.gnutellaLog == null) {
			System.err.println("Error: gnutella Log has not been initialized");
			return false;
		}
		
		// * if existing node is specified, 
		// join the Gnutella network sending the first Ping to a known node *
		if (existingNodeAddress != null && existingNodePort != null) {
			Servent existingNode = new Servent(existingNodeAddress, existingNodePort);
			boolean joined = this.joinGnutellaNetwork(existingNode);
			
			if (!joined) return false;
			
			// * joined Gnutella network! *
		}
		
		// * spawn the Gnutella Listener thread to accept new Gnutella connections *
		boolean listenerStarted = this.startGnutellaListener();
		if (!listenerStarted) return false;
		
		// * setup tcp socket and spawn thread to upload requested files *
		new FileSharing(this.localAddress, this.fileSharingTcpPort, this.filesManager, this.gnutellaLog).start();
		
		// * spawn thread that periodically send Ping to neighbors *
		new PeriodicalPingSender(this.neighbors, this.seenPings, this.gnutellaLog).start();
		
		// * spawn thread to periodically check if neighbors are still responsive *
		// (if the corresponding Pong is not arrived after a sufficient amount of time, discard the neighbor)
		new NeighborsMonitor(this.neighbors, this.gnutellaLog).start();
				
		return true;
	}
	

	private boolean joinGnutellaNetwork(Servent existingNode){
		// open TCP connection with the existing node
		if (!existingNode.connect()) {
			System.out.println("An error occurred connecting to the existing node");
			return false;
		}
		this.gnutellaLog.append(String.format("Connected to first node %s", existingNode));
				
		Announcement firstAnnouncement = new Announcement(
				this.localAddress, this.localTcpPort, this.filesManager.getNumSharedFiles());
		
		// * send first Announcement *
		if(!sendFirstAnnouncement(firstAnnouncement, existingNode)) return false;
		
		// set timeout of 20 sec
		if(!existingNode.setTimeout(TIMEOUT_TO_JOIN)) return false;
		
		// * receive Pong *
		if(!receiveFirstPong(firstAnnouncement.getMessageId(), existingNode)) return false;
		
		// unset timeout
		if(!existingNode.unsetTimeout()) {
			System.err.println("Error removing timeout after joining Gnutella network");
			return false;
		}
	
		// * register first neighbor *
		synchronized (this.neighbors) {	
			this.neighbors.put(existingNode.toString(), existingNode);
		}
		this.gnutellaLog.append("+ Added new neighbor %s", existingNode);
		
		// Ping-Pong ok
		this.gnutellaLog.append("* Joined successfully Gnutella network! *");
	
		// * handle new neighbor * 
		new ServentHandler(existingNode, true, this.localAddress, this.localTcpPort, 
				this.fileSharingTcpPort, this.filesManager, this.neighbors, this.gnutellaLog, 
				this.seenPings, this.expectedPongsReceivers, this.seenQueries, 
				this.expectedQueryHitReceivers, this.requestedQuery, this.fileMatches).start();
		
		return true;
	}
	
	private boolean sendFirstAnnouncement(Announcement announcement, Servent existingNode) {
		// * send first Ping *
		boolean sent = existingNode.send(announcement);
	
		if(!sent) {
			System.err.println("Error sending first ANNOUNCEMENT to join the Gnutella Network");
			return false;
		}
		
		this.gnutellaLog.append("> Sent first ANNOUNCEMENT %s to %s", announcement.getMessageId(), existingNode);
		return true;
	}
	
	private boolean receiveFirstPong(MessageId expectedId, Servent existingNode) {
		GnutellaPacket packet = existingNode.receive();

		if (packet == null) {
			System.err.println("Error receiving first PONG to join the Gnutella Network");
			return false;
		}
		
		// * check Pong *
		if (packet.getPacketType() != PacketType.PONG) {
			System.err.println("Error: received a different Packet rather than PONG to join Gnutella network");
			return false;
		}
		
		Pong firstPong = (Pong)packet;
		
		// check Pong message id 
		if (!Objects.equals(firstPong.getMessageId(), expectedId)) {
			System.err.println("Error: received a PONG with a different ID than first Annnouncement");
			return false;
		}
		
		// check Pong payload 
		if(!Objects.equals(firstPong.getNodeAddress(), existingNode.getAddress()) ||
			firstPong.getNodePort() != existingNode.getTcpPort()) {
			System.err.println("Error: received a PONG describing a different node than expected");
			return false;
		}
		
		this.gnutellaLog.append("> Received first PONG %s from %s", firstPong.getMessageId(), existingNode);
		return true; 	// first Pong successfully received
	}
	
	private boolean startGnutellaListener() {
		ServerSocket listenerSocket;
		
		// create the TCP socket to listen to new Gnutella connections
		try {
			listenerSocket = new ServerSocket(this.localTcpPort, 50, this.localAddress);
		} 
		catch (IOException ioe) {
			System.err.println("An error occurred creating listener TCP socket");
			ioe.printStackTrace();
			return false;
		}
		
		// spawn the Gnutella listener thread
		new GnutellaListener(listenerSocket, this.fileSharingTcpPort, this.filesManager, this.neighbors,
				this.gnutellaLog, this.seenPings, this.expectedPongsReceivers, this.seenQueries, 
				this.expectedQueryHitReceivers, this.requestedQuery, this.fileMatches).start();
		
		return true;
	}

	public void executeGnutellaQuery(String searchString) {
		Query query = new Query(searchString);
		
		synchronized (this.fileMatches) {
			this.requestedQuery.set(query.getMessageId());
		}
		
		// * send Query to all the active neighbors *
		synchronized (this.neighbors) {
			for(Servent neighbor : this.neighbors.values()) {
				// skip disconnected neighbors
				if(!neighbor.isConnected()) continue;
				
				boolean querySent = neighbor.send(query);
				
				if(!querySent) {
					this.gnutellaLog.append("Error sending QUERY %s ('%s') to %s", query, searchString, neighbor);
					
					if(neighbor.disconnect())
						this.gnutellaLog.append("Disconnected from %s", neighbor);
					
					continue;
				}
				
				// * Query sent *
				this.gnutellaLog.append("> Sent QUERY %s ('%s') to %s", query, searchString, neighbor);
			}
		}
	}

	public String requestFile(FileMatch file) {
		Socket socket;
		
		try {
			socket = new Socket(file.getNodeAddress(), file.getNodeFileSharingPort());
		} 
		catch (IOException ioe) {
			ioe.printStackTrace();
			this.gnutellaLog.append("Error occurred connecting to %s to request the file %s", 
					file.getServentId(), file.getFileName());
			return "";
		}
		
		short fileIndex = (short)file.getFileIndex();
		byte[] senderBuffer = splitBigEndian(fileIndex);
		
		try {
			socket.getOutputStream().write(senderBuffer);
		} 
		catch (IOException ioe) {
			this.gnutellaLog.append("Error occurred sending file %s request to %s", file.getFileName(),
					file.getServentId());
			ioe.printStackTrace();
			return "";
		}
		
		this.gnutellaLog.append("* Requested file %s to %s", file.getFileName(), file.getServentId());
		
		byte[] receiverBuffer = new byte[file.getFileSize()];
		
		try {
			socket.getInputStream().read(receiverBuffer);
		} 
		catch (IOException ioe) {
			this.gnutellaLog.append("Error occurred receiving file %s to %s", 
					file.getFileName(), file.getServentId());

			ioe.printStackTrace();
			return "";
		}
		
		this.gnutellaLog.append("* Received file %s to %s", file.getFileName(), file.getServentId());
		
		String fileContent = new String(receiverBuffer, StandardCharsets.UTF_8);
		return fileContent;
	}
}
