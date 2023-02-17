package coms487.hw4.model.packets;

import static coms487.hw4.model.Utilities.joinBytes;
import static coms487.hw4.model.Utilities.joinShortBigEndian;
import static coms487.hw4.model.Utilities.range;
import static coms487.hw4.model.Utilities.splitToShortBigEndian;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class Announcement extends GnutellaPacket {

	public static final PacketType packetType = PacketType.ANNOUNCEMENT;
	private static final byte defaultTtl = 2;
	public static final int payloadLength = 8; // bytes
	
	// payload fields
	private byte[] nodeAddress;	// 4 bytes
	private byte[] nodePort;	// 2 bytes
	private byte[] numSharedFiles; // 2 bytes
	
	
	// to serialize
	public Announcement(InetAddress nodeAddress, int nodePort, int numSharedFiles) {
		super(new MessageId(), packetType, defaultTtl, payloadLength);
		
		this.nodeAddress = nodeAddress.getAddress(); // big-endian
		this.nodePort = splitToShortBigEndian(nodePort);	
		this.numSharedFiles = splitToShortBigEndian(numSharedFiles);
	}
	
	// to deserialize
	public Announcement(MessageId pingId, byte ttl, byte[] payload) {
		super(pingId, packetType, ttl, payloadLength);

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
