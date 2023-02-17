package coms487.hw4.model.packets;


public class Ping extends GnutellaPacket
{
	public static final PacketType packetType = PacketType.PING;
	private static final byte defaultTtl = 2;
	
	// to serialize
	public Ping() {
		super(new MessageId(), packetType, defaultTtl, 0);
	}
	
	// to deserialize (and forwarding)
	public Ping(MessageId messageId, byte ttl) {
		super(messageId, packetType, ttl, 0);
	}

	@Override
	protected byte[] payloadToBytes() {
		return new byte[]{};	// no payload
	}
}
