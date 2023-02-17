package coms487.hw4.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Utilities {
	public static byte[] createRandom(int numBytes) {
		byte[] result = new byte[numBytes];
		
		for(int i=0; i<numBytes; i++) {
			byte randomByte = (byte)(Math.random() * 256);
			result[i] = randomByte;
		}
		
		return result;
	}
	
	public static byte[] splitBigEndian(int num) {		
		return new byte[] {
				(byte)(num >> 24), 
				(byte)(num >> 16), 
				(byte)(num >>  8), 
				(byte)(num >>  0)
		};
	}
	
	public static byte[] splitToShortBigEndian(int num) {		
		return new byte[] {
				(byte)(num >>  8), 
				(byte)(num >>  0)
		};
	}
	
	public static byte[] splitBigEndian(short num) {
		return new byte[] { 
				(byte)(num >> 8), 
				(byte)(num >> 0)
		};
	}
	
	public static int joinIntBigEndian(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}
	
	public static int joinShortBigEndian(byte[] bytes) {
		// add 2 more '0' bytes at the beginning 
		byte[] adjustedBytes = new byte[] {(byte)0, (byte)0, bytes[0], bytes[1]};
		return ByteBuffer.wrap(adjustedBytes).getInt();
	}
	
	public static byte[] range(byte[] bytes, int start, int end) {
		byte[] result = new byte[end-start];
		
		for(int i=0; i<(end-start); i++)
			result[i] = bytes[start+i];
		
		return result;
	}
	
	public static byte[] addNullTerminator(String string) {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		
		return ByteBuffer.allocate(bytes.length + 1)
						.put(bytes)
						.put((byte)0)
						.array();
	}
	
	public static byte[] joinBytes(byte[]... buffers) {
		int capacity = 0;
		
		for(byte[] buffer : buffers)
			capacity += buffer.length;
		
		ByteBuffer b = ByteBuffer.allocate(capacity);
		
		for(byte[] buffer : buffers)
			b.put(buffer);
		
		return b.array();
	}
	
	public static boolean areEquals(byte[] buf1, byte[] buf2) {
		if (buf1.length != buf2.length) return false;
		
		for(int i=0; i<buf1.length; i++)
			if(buf1[i] != buf2[i])
				return false;
		
		return true;
	}
	
	public static String hex(int i) {
		return String.format("0x%08X", i).toLowerCase();
	}
	
	public static String hex(short s) {
		return String.format("0x%04X", s).toLowerCase();
	}
	
	public static String hex(byte b) {
		return String.format("0x%02X", b).toLowerCase();
	}
	
	public static String hex(byte[] bytes) {
		StringBuilder hex = new StringBuilder("0x");
		
		for(byte b : bytes) 
			hex.append(String.format("%02x", b));
		
		return hex.toString().toLowerCase();
	}
	
	public static <T> void removeOneRandom(Collection<T> collection, int max) {
		int randomIndex = (int)(Math.random()*max);
		Iterator<T> iterator = collection.iterator();
		T elementToRemove = iterator.next();
		
		for(int i=0; i<randomIndex; i++)
			elementToRemove = iterator.next();
		
		collection.remove(elementToRemove);
	}
	
	public static <T> void removeOneRandom(Map<T, ?> map, int max) {
		int randomIndex = (int)(Math.random()*max);
		Iterator<T> iterator = map.keySet().iterator();
		T elementToRemove = iterator.next();
		
		for(int i=0; i<randomIndex; i++)
			elementToRemove = iterator.next();
		
		map.remove(elementToRemove);
	}
}
