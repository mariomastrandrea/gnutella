package coms487.hw4.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import coms487.hw4.model.FileMatch;
import coms487.hw4.model.FilesManager;
import coms487.hw4.model.GnutellaLog;
import coms487.hw4.model.Servent;
import coms487.hw4.model.packets.MessageId;


public class GnutellaListener extends Thread
{
	private final ServerSocket tcpListenerSocket;
	private final int fileSharingTcpPort;
	private final FilesManager filesManager;
	private final Map<String, Servent> neighbors;
	private final GnutellaLog gnutellaLog;
	
	private final Set<MessageId> seenPings;
	private final Map<MessageId, Servent> expectedPongReceivers;
	private final Set<MessageId> seenQueries;
	private final Map<MessageId, Servent> expectedQueryHitReceivers; 
	
	private final MessageId requestedQuery;
	private Collection<FileMatch> fileMatches;

	
	public GnutellaListener(ServerSocket tcpSocket, int fileSharingTcpPort, FilesManager filesManager, 
			Map<String, Servent> neighbors, GnutellaLog gnutellaLog, 
			Set<MessageId> seenPings, Map<MessageId, Servent> expectedPongReceivers, 
			Set<MessageId> seenQueries, Map<MessageId, Servent> expectedQueryHitReceivers, 
			MessageId requestedQuery, Collection<FileMatch> fileMatches) 
	{
		this.tcpListenerSocket = tcpSocket;
		this.fileSharingTcpPort = fileSharingTcpPort;
		this.filesManager = filesManager;
		this.neighbors = neighbors;
		this.gnutellaLog = gnutellaLog;
		this.seenPings = seenPings;
		this.expectedPongReceivers = expectedPongReceivers;
		this.seenQueries = seenQueries;
		this.expectedQueryHitReceivers = expectedQueryHitReceivers;
		this.requestedQuery = requestedQuery;
		this.fileMatches = fileMatches;
	}
	
	@Override
	public void run() {	
		InetAddress localAddress = this.tcpListenerSocket.getInetAddress();
		int localPort = this.tcpListenerSocket.getLocalPort();
		this.gnutellaLog.append("Start Gnutella server at %s:%d...", localAddress.getHostAddress(), localPort);
		
		while(true) {
			Socket newServentSocket;
			
			// * start listening to new connections *
			try {
				newServentSocket = this.tcpListenerSocket.accept();
			} 
			catch (IOException ioe) {
				this.gnutellaLog.append("Error accepting a Gnutella connection");
				continue;
			}
			
			// * create the new neighbor * 
			Servent newNeighbor = new Servent(newServentSocket.getInetAddress(), 
										newServentSocket.getPort(), newServentSocket);
			
			// * spawn thread to manage the new neighbor *
			new ServentHandler(newNeighbor, false, this.tcpListenerSocket.getInetAddress(), 
					this.tcpListenerSocket.getLocalPort(), this.fileSharingTcpPort, this.filesManager, 
					this.neighbors, this.gnutellaLog, this.seenPings, this.expectedPongReceivers, 
					this.seenQueries, this.expectedQueryHitReceivers, 
					this.requestedQuery, this.fileMatches).start();
		}			
	}

}
