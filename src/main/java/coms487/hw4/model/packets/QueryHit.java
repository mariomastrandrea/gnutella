package coms487.hw4.model.packets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static coms487.hw4.model.Utilities.*;

public class QueryHit extends GnutellaPacket 
{
	public static final PacketType packetType = PacketType.QUERYHIT;
	private final static byte defaultTtl = 64;

	// payload fields
	private byte[] numHits;			// 2 bytes
	private byte[] hitNodeAddress;  // 4 bytes
	private byte[] hitNodeTcpPort;	// 2 bytes - Tcp port used to download the hit file
	private Collection<HitEntry> hits;
	
	
	// to serialize
	public QueryHit(MessageId queryId, InetAddress hitNodeAddress, int hitNodeTcpPort, 
			Collection<HitEntry> hits) {
		super(queryId, packetType, defaultTtl, 0);
	
		this.numHits = splitToShortBigEndian(hits.size());
		this.hitNodeAddress = hitNodeAddress.getAddress();   // big-endian
		this.hitNodeTcpPort = splitToShortBigEndian(hitNodeTcpPort);
		this.hits = hits;
		
		// compute payload length considering all the query hits
		int payloadLength = this.numHits.length + this.hitNodeAddress.length + this.hitNodeTcpPort.length;
		
		for (HitEntry hit : this.hits)
			payloadLength += hit.getSize();
		
		this.setPayloadLength(payloadLength);
	}
	
	// to deserialize
	public QueryHit(MessageId queryId, byte ttl, byte[] payload) {
		super(queryId, packetType, ttl, payload.length);
		
		this.numHits = range(payload, 0, 2);
		this.hitNodeAddress = range(payload, 2, 6);
		this.hitNodeTcpPort = range(payload, 6, 8);
		this.hits = new ArrayList<>();
		
		// * deserialize query hits *
		int i = 8;
		
		while(i < payload.length) {
			int start = i;
			i += 6; // skip file index and file size
			
			// detect null char terminator
			while (payload[i] != 0 && i < payload.length)
				i++;
			
			if (i < payload.length)
				i++; // move over null terminator
			
			HitEntry hit = new HitEntry(range(payload, start, i));
			this.hits.add(hit);
		}
	}
	
	public int getNumHits() {
		return joinShortBigEndian(this.numHits);
	}
	
	public InetAddress getHitNodeAddress() {
		try {
			return InetAddress.getByAddress(this.hitNodeAddress);
		} 
		catch (UnknownHostException e) {
			System.err.println("Braaaaaaiiiiiinnsssss");
			e.printStackTrace();
			return null;
		}
	}
	
	public int getNodeTcpPort() {
		return joinShortBigEndian(this.hitNodeTcpPort);
	}
	
	public Collection<HitEntry> getHits() {
		return this.hits;
	}

	@Override
	protected byte[] payloadToBytes() {	
		ByteBuffer buffer = ByteBuffer.allocate(this.getPayloadLength())
									  .put(this.numHits)
									  .put(this.hitNodeAddress)
									  .put(this.hitNodeTcpPort);
		
		// now put the hits entries
		Iterator<byte[]> hitsBuffers = this.getHits().stream()
				   							.map(hit -> hit.toBytes()).iterator();
		
		while(hitsBuffers.hasNext())
			buffer.put(hitsBuffers.next());
			
		return buffer.array();
	}
}


