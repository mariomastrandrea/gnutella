package coms487.hw4;

import coms487.hw4.model.FileMatch;
import coms487.hw4.model.GnutellaManager;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class FXMLController 
{    
    private GnutellaManager model;
    
    @FXML
    private TextArea fileContentTextArea;

    @FXML
    private TextArea logTextArea;

    @FXML
    private TextField queryStringTextField;

    @FXML
    private Button searchFilesButton;

    @FXML
    private ComboBox<FileMatch> selectFileComboBox;
    private FileMatch selectedFileMatch;
   


    @FXML
    void handleSearchFilesButton(ActionEvent event) {
    	String searchString = this.queryStringTextField.getText();
    	
    	// clear previous query content
    	this.selectFileComboBox.getItems().clear();
    	this.fileContentTextArea.clear();
    	
    	if(searchString == null || searchString.isBlank()) return;
    	
    	searchString = searchString.trim().toLowerCase();
    	this.model.executeGnutellaQuery(searchString);
    }

    @FXML
    void handleSelectFileComboBox(ActionEvent event) {
    	// clear text area first
    	this.fileContentTextArea.clear();
    	
    	// retrieve the selected file
    	FileMatch selectedOption = this.selectFileComboBox.getValue();
    	
    	if(selectedOption != null)
    		this.selectedFileMatch = selectedOption;
    	
    	String fileContent = this.model.requestFile(this.selectedFileMatch);
    	this.fileContentTextArea.setText(fileContent);
    }
    
	public void setModel(GnutellaManager model) {
		this.model = model;
		
		// create a binding to the log textarea 
		StringProperty logOutput = new SimpleStringProperty("");
		this.logTextArea.textProperty().bind(logOutput);
		
		// scroll to the bottom every time the text is changed
		logOutput.addListener(new ChangeListener<Object>() {
			  @Override
			  public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
				  logTextArea.selectPositionCaret(logTextArea.getLength());
				  logTextArea.deselect();  // this will scroll to the bottom
			  }
		});
		
		// create a binding to the file matches combo box
		ListProperty<FileMatch> fileMatches = 
				new SimpleListProperty<FileMatch>(FXCollections.observableArrayList());
		this.selectFileComboBox.itemsProperty().bindBidirectional(fileMatches);
		
		
		fileMatches.addListener(new ChangeListener<ObservableList<FileMatch>>() {
			@Override
			public void changed(ObservableValue<? extends ObservableList<FileMatch>> observable,
					ObservableList<FileMatch> oldValue, ObservableList<FileMatch> newValue) {
				if(newValue.isEmpty()) {
					selectFileComboBox.setDisable(true);
				}
				else {
					selectFileComboBox.setDisable(false);
				}
			}
		});
		

		this.model.initGnutellaLog(logOutput, fileMatches);
	}    
}
