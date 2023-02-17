package coms487.hw4.model.packets;

import static coms487.hw4.model.Utilities.*;

import java.util.Arrays;

public class MessageId {
	public static final int LENGTH = 10;
	private byte[] id;
	
	
	public MessageId() {
		this.id = createRandom(LENGTH);
	}
	
	public MessageId(byte[] id) {
		this.id = id;
	}
	
	public byte[] bytes() { return this.id; }
	
	@Override
	public String toString() {
		return hex(this.id);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(id);
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
		MessageId other = (MessageId) obj;
		if (!Arrays.equals(id, other.id))
			return false;
		return true;
	}

	public void set(MessageId messageId) {
		this.id = messageId.id;
	}
}
