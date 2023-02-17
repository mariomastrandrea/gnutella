package coms487.hw4.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import coms487.hw4.model.packets.HitEntry;

public class FilesManager 
{
	private static final int NUM_SHARED_FILES = 6;	
	private static final int MAX_FILE_NUM = 20;
	private List<String> sharedFiles;
	private List<String> fileNames;
	
	public FilesManager() {
		this.sharedFiles = new ArrayList<>();
		this.fileNames = new ArrayList<>();
		this.initializeSharedFiles();
	}
	
	public int getNumSharedFiles() { return NUM_SHARED_FILES; }

	private void initializeSharedFiles() {
		HashSet<Integer> choosenRandomIntegers = new HashSet<>();
		
		// create fake string files
		for(int i=0; i<NUM_SHARED_FILES; i++) {
			int randomFileNumber;
			do {
				randomFileNumber = (int)(Math.random() * MAX_FILE_NUM);
			}
			while(choosenRandomIntegers.contains(randomFileNumber));
			
			choosenRandomIntegers.add(randomFileNumber);
			StringBuilder fileContent = new StringBuilder();
			
			int baseLines = 5;
			int additionalLines = (int)(Math.random() * 11);
			
			int totLines = baseLines + additionalLines;
			
			// add text lines
			for(int k=0; k<totLines; k++)
				fileContent.append("This is the content of the ").append(randomFileNumber).append(" file.\n");
			
			// save file
			String fileName = String.format("file%d.txt", randomFileNumber);
			this.sharedFiles.add(fileContent.toString());
			this.fileNames.add(fileName);
		}
	}
	
	public List<HitEntry> findMatchingFiles(String substring) {
		List<HitEntry> matchingFiles = new ArrayList<>();
		
		for(int i=0; i<this.fileNames.size(); i++) {
			String fileName = this.fileNames.get(i);
			String file = this.sharedFiles.get(i);
			
			if(fileName.toLowerCase().contains(substring.toLowerCase())) {
				HitEntry hit = new HitEntry(i, file.length(), fileName);
				matchingFiles.add(hit);
			}
		}
		
		return matchingFiles;
	}

	public String getFileContent(int requestedFileIndex) {
		return this.sharedFiles.get(requestedFileIndex);
	}

	public String getFileName(int requestedFileIndex) {
		return this.fileNames.get(requestedFileIndex);
	}
}
