package coms487.hw4.model;

import java.net.InetAddress;

import coms487.hw4.model.packets.HitEntry;

public class FileMatch 
{
	private final InetAddress nodeAddress;
	private final int nodeFileSharingPort;
	private final int fileIndex;
	
	private final int fileSize;
	private final String fileName;
	
	
	public FileMatch(InetAddress nodeAddress, int nodeFileSharingPort, HitEntry fileHit) {
		this.nodeAddress = nodeAddress;
		this.nodeFileSharingPort = nodeFileSharingPort;
		this.fileIndex = fileHit.getFileIndex();
		this.fileSize = fileHit.getFileSize();
		this.fileName = fileHit.getFileName();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fileIndex;
		result = prime * result + ((nodeAddress == null) ? 0 : nodeAddress.hashCode());
		result = prime * result + nodeFileSharingPort;
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
		FileMatch other = (FileMatch) obj;
		if (fileIndex != other.fileIndex)
			return false;
		if (nodeAddress == null) {
			if (other.nodeAddress != null)
				return false;
		} else if (!nodeAddress.equals(other.nodeAddress))
			return false;
		if (nodeFileSharingPort != other.nodeFileSharingPort)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("%s (%d bytes) - from %s:%d", 
				this.fileName, this.fileSize, this.nodeAddress.getHostAddress(), this.nodeFileSharingPort);
	}
	
	public InetAddress getNodeAddress() { return this.nodeAddress; }
	public int getNodeFileSharingPort() { return this.nodeFileSharingPort; }
	
	public String getServentId() {
		return String.format("%s:%d", this.nodeAddress.getHostAddress(), this.nodeFileSharingPort);
	}
	
	public int getFileIndex() {
		return this.fileIndex;
	}

	public String getFileName() {
		return this.fileName;
	}

	public int getFileSize() {
		return this.fileSize;
	}
}
