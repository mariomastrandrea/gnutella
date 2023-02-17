package coms487.hw4.model.packets;
import static coms487.hw4.model.Utilities.*;

import java.nio.ByteBuffer;

public abstract class GnutellaPacket {
	public static final int HEADER_LENGTH = 16; // bytes
	
	// header fields
	private final byte[] messageId;	  // 10 bytes
	private final byte packetType;    // 1 byte
	private byte ttl;			  	  // 1 byte - Time To Live
	private byte[] payloadLength;     // 4 bytes 
	
	
	public enum PacketType {
		PING((byte)0), PONG((byte)1), QUERY((byte)2), QUERYHIT((byte)3), ANNOUNCEMENT((byte)4);
		
		public final byte num;
		private PacketType(byte num) { this.num = num; }
	}
	
	public GnutellaPacket(MessageId messageId, PacketType packetType, byte ttl, int payloadLength) {
		this.messageId = messageId.bytes();
		this.packetType = packetType.num;
		this.ttl = ttl;
		this.payloadLength = splitBigEndian(payloadLength);
	}
	
	public MessageId getMessageId() { return new MessageId(this.messageId); }
	public PacketType getPacketType() { return PacketType.values()[this.packetType]; }
	public byte getTtl() { return this.ttl; }
	public int getPayloadLength() { return joinIntBigEndian(this.payloadLength); }
	
	public void decrementTtl() {
		this.ttl -= 1;
	}
	
	protected void setPayloadLength(int payloadLength) {
		this.payloadLength = splitBigEndian(payloadLength);
	}
	
	protected abstract byte[] payloadToBytes();
	
	public byte[] toBytes() {
		int headerLength = this.messageId.length + 
							1 +	 /* packet type */
							1 +  /* ttl */
							this.payloadLength.length;
		
		ByteBuffer buffer = ByteBuffer.allocate(headerLength + this.getPayloadLength());
		
		buffer.put(this.messageId)
			  .put(this.packetType)
			  .put(this.ttl)
			  .put(this.payloadLength)
			  .put(this.payloadToBytes());
		
		return buffer.array();
	}	
	
	@Override
	public String toString() { return this.getMessageId().toString(); }
}
