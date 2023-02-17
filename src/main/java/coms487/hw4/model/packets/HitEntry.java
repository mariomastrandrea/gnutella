package coms487.hw4.model.packets;

import static coms487.hw4.model.Utilities.addNullTerminator;
import static coms487.hw4.model.Utilities.joinBytes;
import static coms487.hw4.model.Utilities.joinIntBigEndian;
import static coms487.hw4.model.Utilities.joinShortBigEndian;
import static coms487.hw4.model.Utilities.range;
import static coms487.hw4.model.Utilities.splitBigEndian;
import static coms487.hw4.model.Utilities.splitToShortBigEndian;

import java.nio.charset.StandardCharsets;

public class HitEntry {
	private byte[] fileIndex;	// 2 bytes
	private byte[] fileSize;	// 4 bytes
	private byte[] fileName;	// byte string terminated by null char (0x00)
	
	
	// to serialize
	public HitEntry(int fileIndex, int fileSize, String fileName) {
		this.fileIndex = splitToShortBigEndian(fileIndex);
		this.fileSize  = splitBigEndian(fileSize);
		this.fileName = addNullTerminator(fileName);
	}
	
	// to deserialize
	public HitEntry(byte[] hitPayload) {
		this.fileIndex = range(hitPayload, 0, 2); // [0, 2]
		this.fileSize  = range(hitPayload, 2, 6);  // [2, 6]
		this.fileName  = range(hitPayload, 6, hitPayload.length);  // [4, end]
	}
	
	public int getFileIndex() {
		return joinShortBigEndian(this.fileIndex);
	}
	
	public int getFileSize() {
		return joinIntBigEndian(this.fileSize);
	}
	
	public String getFileName() {
		return new String(this.fileName, 0, 
				this.fileName.length - 1, StandardCharsets.UTF_8);
	}
	
	public int getSize() {
		return this.fileIndex.length + this.fileSize.length + this.fileName.length;
	}
	
	public byte[] toBytes() {
		return joinBytes(this.fileIndex, this.fileSize, this.fileName);
	}
}
