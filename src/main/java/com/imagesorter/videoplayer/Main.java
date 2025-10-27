package com.imagesorter.videoplayer;

import java.io.File;
import java.net.MalformedURLException;

import com.imagesorter.videoplayer.Player;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

// launches the application
public class Main extends Application {
    Player player;
    FileChooser fileChooser;
    public void start(final Stage primaryStage) throws Exception
    {
        // setting up the stages
        MenuItem open = new MenuItem("Open");
        Menu fileMenu = new Menu("File");
        MenuBar menu = new MenuBar();

        // Connecting the above three
        fileMenu.getItems().add(open); // it would connect open with file
        menu.getMenus().add(fileMenu);

        // Adding functionality to switch to different videos
        fileChooser = new FileChooser();
        open.setOnAction((e)->{
            // Pausing the video while switching
            Main.this.player.player.pause();
            File file = fileChooser.showOpenDialog(primaryStage);

            // Choosing the file to play
            if (file != null) {
                try {
//                    player = new Player(file.toURI().toURL().toExternalForm());
                    player =  null;
                    Scene scene = new Scene(player, 720, 535, Color.BLACK);
                    primaryStage.setScene(scene);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        // here you can choose any video
        player =null;

        // Setting the menu at the top
        player.setTop(menu);

        // Adding player to the Scene
        Scene scene = new Scene(player, 720, 535, Color.BLACK);

        // height and width of the video player
        // background color set to Black
        primaryStage.setScene(scene); // Setting the scene to stage
        primaryStage.show(); // Showing the stage
    }

    // Main function to launch the application
    public static void main(String[] args){
        launch(args);
    }
}