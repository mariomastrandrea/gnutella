package coms487.hw4.model.packets;

import java.nio.charset.StandardCharsets;

public class Query extends GnutellaPacket 
{
	public static final PacketType packetType = PacketType.QUERY;
	private static final byte defaultTtl = 64;
	
	// payload fields
	private byte[] queryString;
	
	
	// to serialize
	public Query(String queryString) {
		super(new MessageId(), packetType, defaultTtl, 
				queryString.getBytes(StandardCharsets.UTF_8).length);
		
		this.queryString = queryString.getBytes(StandardCharsets.UTF_8);
	}
	
	// to deserialize
	public Query(MessageId messageId, byte ttl, byte[] payload) {
		super(messageId, packetType, ttl, payload.length);
		
		this.queryString = payload;
	}
	
	public String getQueryString() {
		return new String(this.queryString, StandardCharsets.UTF_8);
	}

	@Override
	protected byte[] payloadToBytes() {
		return this.queryString;
	}
}
