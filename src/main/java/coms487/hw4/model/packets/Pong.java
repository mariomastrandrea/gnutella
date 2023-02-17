package coms487.hw4.model.packets;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static coms487.hw4.model.Utilities.*;

public class Pong extends GnutellaPacket {

	public static final PacketType packetType = PacketType.PONG;
	private static final byte defaultTtl = 2;
	public static final int payloadLength = 8; // bytes
	
	// payload fields
	private byte[] nodeAddress;	// 4 bytes
	private byte[] nodePort;	// 2 bytes
	private byte[] numSharedFiles; // 2 bytes
	
	
	// to serialize
	public Pong(MessageId pingId, InetAddress nodeAddress, int nodePort, int numSharedFiles) {
		super(pingId, packetType, defaultTtl, payloadLength);
		
		this.nodeAddress = nodeAddress.getAddress(); // big-endian
		this.nodePort = splitToShortBigEndian(nodePort);	
		this.numSharedFiles = splitToShortBigEndian(numSharedFiles);
	}
	
	// to deserialize
	public Pong(MessageId messageId, byte ttl, byte[] payload) {
		super(messageId, packetType, ttl, payloadLength);

		this.nodeAddress = range(payload, 0, 4);
		this.nodePort = range(payload, 4, 6);
		this.numSharedFiles = range(payload, 6, 8);
	}
	
	public InetAddress getNodeAddress() {
		try {
			return InetAddress.getByAddress(this.nodeAddress);
		} 
		catch (UnknownHostException e) {
			System.err.println("Braaaaaaiiiiiinnsssss");
			e.printStackTrace();
			return null;
		}
	}
	
	public int getNodePort() {
		return joinShortBigEndian(this.nodePort);
	}
	
	public int getNumSharedFiles() {
		return joinShortBigEndian(this.numSharedFiles);
	}

	@Override
	protected byte[] payloadToBytes() {
		return joinBytes(this.nodeAddress, this.nodePort, this.numSharedFiles);
	}
}
