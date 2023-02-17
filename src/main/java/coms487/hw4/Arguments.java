package coms487.hw4;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


public class Arguments 
{
	private final List<String> args;
	private String errorMessage;
	
	private Integer serventTcpPort;			// TCP port where *this* Gnutella node will be listening to packets
	private Integer fileSharingTcpPort;		// TCP port where *this* Gnutella node will be listening to share files with other nodes
	private InetAddress gnutellaNodeIpAddress; 	// existing node's address
	private Integer gnutellaNodeTcpPort;		// existing node's TCP port where it is listening to
	

	public Arguments(List<String> args) {
		this.args = args;
		this.errorMessage = null;
		
		this.serventTcpPort = null;
		this.fileSharingTcpPort = null;
		this.gnutellaNodeIpAddress = null;
		this.gnutellaNodeTcpPort = null;
	}
	
	public boolean areValid() {
		// check args num
		if (this.args.size() != 2 && this.args.size() != 4) {
			this.errorMessage = String.format("%s\n%s", 
				"Usage: main <listening TCP port> <fileSharing TCP port> [<Gnutella node address> <Gnutella node TCP port>]",
				"\t*the 2 optional arguments comes together");
			
			return false;
		}
				
		// 1 - check listening port number
		try {
			this.serventTcpPort = Integer.parseInt(args.get(0));
			
			if (this.serventTcpPort < 0 || this.serventTcpPort > 65535)
				throw new NumberFormatException();
		}
		catch (NumberFormatException nfe) {
			this.errorMessage = "Error: *listening port* is not valid (must be an integer between 0 an 65535)";
			nfe.printStackTrace();
			return false;
		}
		
		// 2 - check file sharing port number
		try {
			this.fileSharingTcpPort = Integer.parseInt(args.get(1));
			
			if (this.fileSharingTcpPort < 0 || this.fileSharingTcpPort > 65535)
				throw new NumberFormatException();
		}
		catch (NumberFormatException nfe) {
			this.errorMessage = "Error: *fileSharing port* is not valid (must be an integer between 0 an 65535)";
			nfe.printStackTrace();
			return false;
		}
		
		if (this.args.size() == 2) return true;

		// 3 - check existing Gnutella servent's address
		try {
			this.gnutellaNodeIpAddress = InetAddress.getByName(args.get(2));
		} 
		catch (UnknownHostException uhe) {
			this.errorMessage = "Error: *existing Gnutella address* is not valid";
			uhe.printStackTrace();
			return false;
		}
		
		// 4 - check existing Gnutella servent's port number
		try {
			this.gnutellaNodeTcpPort = Integer.parseInt(args.get(3));
			
			if (this.serventTcpPort < 0 || this.serventTcpPort > 65535)
				throw new NumberFormatException();
		}
		catch (NumberFormatException nfe) {
			this.errorMessage = "Error: *existing Gnutella servent port* is not valid (must be an integer between 0 an 65535)";
			nfe.printStackTrace();
			return false;
		}
		
		// input ok
		return true;
	}
	
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
	public int getServentTcpPort() {
		return this.serventTcpPort;
	}
	
	public int getFileSharingTcpPort() {
		return this.fileSharingTcpPort;
	}
	
	public InetAddress getGnutellaNodeIpAddress() {
		return this.gnutellaNodeIpAddress;
	}
	
	public Integer getGnutellaNodeTcpPort() {
		return this.gnutellaNodeTcpPort;
	}
}

