package coms487.hw4.model;

import javafx.application.Platform;
import javafx.beans.value.WritableStringValue;

public class GnutellaLog 
{
	private WritableStringValue logString;
	
	
	public GnutellaLog(WritableStringValue logString) {
		this.logString = logString;
	}
	
	public synchronized void append(String newLog, Object... args) {
		Platform.runLater(() -> {
			String oldLogOutput = this.logString.get();
			
			this.logString.set(oldLogOutput + String.format(newLog, args) + "\n");
		});
	}
}
