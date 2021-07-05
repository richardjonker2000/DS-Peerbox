package pt.ipb.dsys.peerbox;


import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.jgroups.Address;
import org.jgroups.ObjectMessage;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


public class GuiMain extends Application {

    public static Channel current = new Channel();

    public static void main(String[] args) {
        try {
            current.startChannel();
            Thread thread = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(2000);
                            current.sayAdmin();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }


    static ComboBox members = new ComboBox();
    static String selected = "";
    static String uploadFilePath = "";
    static Text listText = new Text();
    static Text metaText = new Text();
    static Text storageText = new Text();

    @Override
    public void start(Stage stage) throws Exception {

        updateMembers(current.members);
        stage.setTitle("PeerBox");
        //---------------------ROW 0---------------------
        Text memText = new Text();
        memText.setText("Members:");
        memText.setTextAlignment(TextAlignment.CENTER);

        members.setOnAction((event) -> {
            selected = "" + members.getValue();
        });

        Button refresh = new Button("Refresh");
        refresh.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                updateMembers(current.members);
            }
        });

        //---------------------ROW 1---------------------
        Button selectFile = new Button("Select File");
        selectFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showOpenDialog(fileChooser);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    uploadFilePath = selectedFile.getAbsolutePath();
                }
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    byte[] data = Files.readAllBytes(Paths.get(uploadFilePath));
                    File temp = new File(uploadFilePath);
                    byte[] header = (temp.getName()).getBytes();
                    int messagesize = 1 + header.length + data.length;
                    byte[] message = new byte[messagesize];
                    message[0] = (byte) header.length;
                    System.arraycopy(header, 0, message, 1, header.length);
                    System.arraycopy(data, 0, message, header.length + 1, data.length);
                    ObjectMessage message1 = new ObjectMessage(dest, message);
                    current.channel.send(message1);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        TextField copies = new TextField();
        copies.setPromptText("Copies");

        Button uploadButton = new Button("Upload File");
        uploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    File temp = new File(uploadFilePath);
                    String fileName = temp.getName();
                    int copy = Integer.parseInt(copies.getText());
                    if (copy < 10)
                        current.channel.send(dest, "7" + copy + "" + fileName);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 2---------------------

        TextField delFile = new TextField();
        delFile.setPromptText("File name");

        Button deleteButton = new Button("Delete File");
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    current.channel.send(dest, "6" + delFile.getText());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 3---------------------

        Button listButton = new Button("List Files");
        listButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    current.channel.send(dest, "4");
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 4---------------------

        listText.setText("");
        ScrollPane spList = new ScrollPane();
        spList.setContent(listText);
        spList.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spList.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        spList.setPrefSize(690, 180);

        //---------------------ROW 5---------------------
        TextField retrieveFile = new TextField();
        retrieveFile.setPromptText("File Name");

        Button retrieveButton = new Button("Retrieve File");
        retrieveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {

                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    current.channel.send(dest, "5" + retrieveFile.getText());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 6---------------------


        TextField metaFile = new TextField();
        metaFile.setPromptText("File Name");

        Button metadataButton = new Button("File Metadata");
        metadataButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    current.channel.send(dest, "8" + metaFile.getText());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 7---------------------

        metaText.setText("");
        ScrollPane spMeta = new ScrollPane();
        spMeta.setContent(metaText);
        spMeta.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spMeta.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        spMeta.setPrefSize(690, 180);

        //---------------------ROW 8---------------------

        Button storageButton = new Button("Show Storage");
        storageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Address dest = null;
                for (int i = 0; i < current.members.size(); i++) {
                    if (current.members.get(i).toString().equals(selected)) {
                        dest = current.members.get(i);
                        break;
                    }
                }
                try {
                    current.channel.send(dest, "9");
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        //---------------------ROW 9---------------------

        storageText.setText("");

        //-----------------------------------------------

        GridPane gridPane = new GridPane();

        //---------------------ROW 0---------------------
        gridPane.add(memText, 0, 0, 1, 1);
        gridPane.add(members, 1, 0, 1, 1);
        gridPane.add(refresh, 2, 0, 1, 1);
        //---------------------ROW 1---------------------
        gridPane.add(selectFile, 0, 1, 1, 1);
        gridPane.add(copies, 1, 1, 1, 1);
        gridPane.add(uploadButton, 2, 1, 1, 1);
        //---------------------ROW 2---------------------
        gridPane.add(delFile, 0, 2, 2, 1);
        gridPane.add(deleteButton, 2, 2, 1, 1);
        //---------------------ROW 3---------------------
        gridPane.add(listButton, 0, 3, 3, 1);
        //---------------------ROW 4---------------------
        gridPane.add(spList, 0, 4, 3, 1);
        //---------------------ROW 5---------------------
        gridPane.add(retrieveFile, 0, 5, 2, 1);
        gridPane.add(retrieveButton, 2, 5, 1, 1);
        //---------------------ROW 6---------------------
        gridPane.add(metaFile, 0, 6, 2, 1);
        gridPane.add(metadataButton, 2, 6, 3, 1);
        //---------------------ROW 7---------------------
        gridPane.add(spMeta, 0, 7, 3, 1);
        //---------------------ROW 8---------------------
        gridPane.add(storageButton, 0, 8, 3, 1);
        //---------------------ROW 9---------------------
        gridPane.add(storageText, 0, 9, 3, 1);

        gridPane.setAlignment(Pos.CENTER);

        Scene scene = new Scene(gridPane, 1000, 800);
        stage.setScene(scene);
        stage.show();
    }

    public static void updateMembers(List<Address> mem) {
        members.getItems().removeAll(members.getItems());
        for (int i = 0; i < mem.size(); i++) {
            if (!mem.get(i).toString().equals(current.admin.toString()))
                members.getItems().add(mem.get(i).toString());
        }
    }


}







