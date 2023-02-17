package coms487.hw4.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import coms487.hw4.model.FilesManager;
import coms487.hw4.model.GnutellaLog;
import coms487.hw4.model.Utilities;

public class FileSharing extends Thread 
{
	private final InetAddress localAddress;
	private final int fileSharingTcpPort;
	private final FilesManager filesManager;
	private final GnutellaLog gnutellaLog;
	
	
	public FileSharing(InetAddress localAddress, int fileSharingTcpPort, 
			FilesManager filesManager, GnutellaLog gnutellaLog) {
		this.localAddress = localAddress;
		this.fileSharingTcpPort = fileSharingTcpPort;
		this.filesManager = filesManager;
		this.gnutellaLog = gnutellaLog;
	}
	
	@SuppressWarnings("resource")
	@Override
	public void run() {
		ServerSocket ss;
		
		try {
			ss = new ServerSocket(this.fileSharingTcpPort, 50, this.localAddress);
		} 
		catch (IOException ioe) {
			System.err.println("An error occurred starting file sharing tcp server");
			ioe.printStackTrace();
			return;
		}
		
		while(true) {
			Socket clientSocket;
			
			try {
				clientSocket = ss.accept();
			}
			catch (IOException ioe) {
				System.err.println("An error occurred accepting a connection for file sharing");
				ioe.printStackTrace();
				continue;
			}
			
			byte[] receiverBuffer = new byte[2];
			
			try {
				clientSocket.getInputStream().read(receiverBuffer);
			} 
			catch (IOException ioe) {
				System.err.println("An error occurred receiving request from client");
				ioe.printStackTrace();
				continue;
			}
			
			int requestedFileIndex = Utilities.joinShortBigEndian(receiverBuffer);
			String requestedFileName = this.filesManager.getFileName(requestedFileIndex);
			
			this.gnutellaLog.append("• Node %s:%d requested download file %s", 
					clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), requestedFileName);
			
			String fileContent = this.filesManager.getFileContent(requestedFileIndex);
		
			byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
			
			try {
				clientSocket.getOutputStream().write(fileBytes);
			} 
			catch (IOException ioe) {
				System.err.println("An error occurred sending file to client");
				ioe.printStackTrace();
				continue;
			}
			
			this.gnutellaLog.append("• Sent file %s to %s:%d", requestedFileName,
					clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
		}
	}
}
