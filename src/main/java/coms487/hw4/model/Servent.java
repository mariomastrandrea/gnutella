package coms487.hw4.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import coms487.hw4.model.packets.Announcement;
import coms487.hw4.model.packets.GnutellaPacket;
import coms487.hw4.model.packets.MessageId;
import coms487.hw4.model.packets.GnutellaPacket.PacketType;
import coms487.hw4.model.packets.Ping;
import coms487.hw4.model.packets.Pong;
import coms487.hw4.model.packets.Query;
import coms487.hw4.model.packets.QueryHit;

import static coms487.hw4.model.Utilities.*;

public class Servent {
	private InetAddress address;
	private int tcpListeningPort;
	private Socket tcpSocket;
	private Long lastResponseTime; // in ms
	
	
	public Servent(InetAddress address, int udpPort) {
		this.address = address;
		this.tcpListeningPort = udpPort;
		this.tcpSocket = null;
		this.lastResponseTime = null;
	}
	
	public Servent(InetAddress address, int udpPort, Socket tcpSocket) {
		this.address = address;
		this.tcpListeningPort = udpPort;
		this.tcpSocket = tcpSocket;
		this.lastResponseTime = null;
	}
	
	public InetAddress getAddress() { return this.address; }
	public int getTcpPort() { return this.tcpListeningPort; }
	public Socket getTcpSocket() { return this.tcpSocket; }
	public Long getLastResponseTime() { return this.lastResponseTime; }
	
	public void setAddress(InetAddress address) { this.address = address; }
	public void setTcpPort(int tcpPort) { this.tcpListeningPort = tcpPort; }
	public void setLastResponseTime(long lastResponseTime) { this.lastResponseTime = lastResponseTime; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + tcpListeningPort;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Servent other = (Servent) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (tcpListeningPort != other.tcpListeningPort)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d", this.address.getHostAddress(), this.tcpListeningPort);
	}

	public boolean isConnected() {
		return this.tcpSocket != null && !this.tcpSocket.isClosed();
	}
	
	public boolean connect() {
		try {
			// 3-way handshake
			this.tcpSocket = new Socket(this.address, this.tcpListeningPort);
		} 
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean disconnect() {
		if(this.tcpSocket.isClosed()) return false;
		
		try {
			// 4-way teardown
			this.tcpSocket.close();
		} 
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean setTimeout(int seconds) {
		int milliseconds = 1000 * seconds;
		
		try {
			this.tcpSocket.setSoTimeout(milliseconds);
		} 
		catch (SocketException se) {
			System.err.println("Error setting timeout to join Gnutella network");
			se.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean unsetTimeout() {
		return setTimeout(0);
	}
	
	public boolean send(GnutellaPacket packet) {		
		try {
			OutputStream output = this.tcpSocket.getOutputStream();
			output.write(packet.toBytes());
		} 
		catch (IOException ioe) {
			// the socket is disconnected or error during I/O
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public GnutellaPacket receive() {
		byte[] headerBuffer = new byte[GnutellaPacket.HEADER_LENGTH];
		InputStream input;
		int receivedBytes;

		try {
			// * receive header *
			input = this.tcpSocket.getInputStream();
			receivedBytes = input.read(headerBuffer);
		} 
		catch (IOException ioe) {
			// the socket is disconnected or error during I/O
			ioe.printStackTrace();
			return null;
		}
		
		// check header size
		if(receivedBytes != headerBuffer.length) {
			System.err.println("Error: different header length than expected");
			return null;
		}
		
		// check and get *packet type*
		byte packetTypeByte = headerBuffer[MessageId.LENGTH];
		if(packetTypeByte < 0 || packetTypeByte >= PacketType.values().length) {
			System.err.println("Error: unrecognized packet type received");
			return null;
		}
		
		PacketType type = PacketType.values()[packetTypeByte];
		
		// get *message id*
		MessageId messageId = new MessageId(range(headerBuffer, 0, MessageId.LENGTH));
		
		// get *ttl*
		byte ttl = headerBuffer[MessageId.LENGTH+1];
		
		// get *payload length* from last 4 bytes
		int payloadLength = joinIntBigEndian(
				range(headerBuffer, GnutellaPacket.HEADER_LENGTH-4, GnutellaPacket.HEADER_LENGTH));
		
		byte[] payloadBuffer = new byte[payloadLength];
		
		try {
			// * receive payload *
			receivedBytes = input.read(payloadBuffer);
		} 
		catch (IOException ioe) {
			// socket disconnected or I/O error
			ioe.printStackTrace();
			return null;
		}
		
		// check payload size
		if (receivedBytes != payloadBuffer.length) {
			System.err.println("Error: different payload length than expected");
			return null;
		}
		
		// deserialize packet
		switch(type) {
			case PING:     return new Ping(messageId, ttl);
			case PONG:     return new Pong(messageId, ttl, payloadBuffer);
			case QUERY:    return new Query(messageId, ttl, payloadBuffer);				
			case QUERYHIT: return new QueryHit(messageId, ttl, payloadBuffer);
			case ANNOUNCEMENT: return new Announcement(messageId, ttl, payloadBuffer);
			default: 	  
				System.err.println("Braaaaiiiiiiinssss");
				return null;
		}
	}

	
}
