package coms487.hw4;

import coms487.hw4.model.GnutellaManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class EntryPoint extends Application 
{	
    @Override
    public void start(Stage stage) throws Exception 
    { 	
    	String serventAddress = "127.0.0.1";
    	
    	// * check input args first *
    	Arguments programArgs = new Arguments(getParameters().getUnnamed());
    	
    	if (!programArgs.areValid()) {
    		System.err.println(programArgs.getErrorMessage());
    		Platform.exit();
    		return;
    	}    	
    	
    	// * load the GUI from FXML file *
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Scene_HW4.fxml")); // to export in JAR
    	//FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Scene_HW4.fxml"));
    	
        Parent root = loader.load();
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add("/resources/styles/Styles.css");   // to export in JAR
        //scene.getStylesheets().add("/styles/Styles.css");
        
        // * create the model class *
        GnutellaManager model = new GnutellaManager(
        	serventAddress,						/* IPv4 address where this node will be listening to */
        	programArgs.getServentTcpPort(), 	/* TCP port where this node will be listening to */ 
        	programArgs.getFileSharingTcpPort() /* TCP port where this node will be available for file sharing */
        );
        
        // * set the controller's model *
        FXMLController controller = loader.getController();
        controller.setModel(model);
        
        // * show the GUI *
        stage.setTitle("HW4 - Gnutella user panel 127.0.0.1:" + programArgs.getServentTcpPort());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        
        // * startup the Gnutella node *
        boolean started = model.startup(
        	programArgs.getGnutellaNodeIpAddress(),  /* existing node's ip address */
        	programArgs.getGnutellaNodeTcpPort()	 /* existing node's listening TCP port */
        );
        
        if (!started) {
        	Platform.exit();
        	return;
        }
        
       
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        	@Override
            public void handle(WindowEvent e) {
               Platform.exit();
               System.exit(0);
            }
        });
    }

    /**
     * @param args the command line arguments:
     * 		<servent IP> <Gnutella TCP port> [Gnutella node IP address]
     */
    public static void main(String[] args) {
        launch(args);
    }
}
