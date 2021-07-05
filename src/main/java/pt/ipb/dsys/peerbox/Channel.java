package pt.ipb.dsys.peerbox;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.Protocol;
//import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
//import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

//class to handle the channel
public class Channel {
    public static final Logger logger = LoggerFactory.getLogger(Main.class); //define the logger
    private static final String CLUSTER_NAME = "peerbox_group"; //define the cluster to connect to
    List<Address> members = new LinkedList(); //crate a list of addresses to represent the memebers that are connected to the channel
    Address admin = null;
    boolean imAdmin = false;
    JChannel channel; //the channel


    //function to start the channel and receiver
    public void startChannel() throws Exception {

        channel = new JChannel(gossipRouter());// create the channel with the gossip router
        channel.connect(CLUSTER_NAME); //connect to the cluster

        //overload the receiver
        channel.receiver(new Receiver() {
                             public void receive(Message msg) {

                                 //we split messages recceived into 2 main categories, those which are strings, and those which are not
                                 try {
                                     //we first try to cast the msg to a string
                                     String data = (String) msg.getObject();
                                     if (data.equals("Admin")) { //if the message is an admin message
                                         admin = msg.getSrc(); //define the admin, based on the source
                                         members = channel.getView().getMembers(); //update the memebers list
                                         Channel.logger.info(admin + " is the group admin");
                                     } else {
                                         switch (data.charAt(0)) { //we switch the first character to use as a protocol
                                             case '1', '2': //if it is 1 or 2, then it is a file to be sent or deleted
                                                 String name = data.substring(1);
                                                 //the data is the name of the file
                                                 for (int i = 0; i < FileManager.files.size(); i++) { //we iterate through all the files we have
                                                     //supposedly received
                                                     FileStorageSytem file = FileManager.files.get(i);
                                                     if (file.getFilename().equals(name)) { //we check that we have the file stored
                                                         switch (data.charAt(0)) {
                                                             case '1':
                                                                 //we then send the file to the device that requested it
                                                                 FileManager.sendFile("Files/ReceivedFiles/" + name, msg.dest(), msg.src(), file.getFileID(), file.getChunkID(), file.getCopyNumber(), file.getFilepath(), name);
                                                                 //i = FileManager.files.size();
                                                                 break;
                                                             case '2':
                                                                 //we delete the file
                                                                 FileManager.deleteFileRec("Files/ReceivedFiles/" + name);
                                                                 FileManager.files.remove(i);
                                                                 break;
                                                         }
                                                         break;
                                                     }
                                                 }
                                                 break;
//                                             case '3': //delete
//                                                 break;
                                             //the rest of the cases are called when sent from the gui device
                                             case '4'://list the files
                                                 String listText = data.substring(1);
                                                 if (imAdmin) { //if we are the gui we set the text of the gui element
                                                     GuiMain.listText.setText(listText);
                                                 } else {//if we are a node, we simply send a message to the admin with the text
                                                     channel.send(msg.getSrc(), "4" + FileManager.showFilesStored());
                                                 }
                                                 break;
                                             case '5'://retrieve a file with a name
                                                 String retFileName = data.substring(1);
                                                 FileManager.retrieveFile(retFileName);
                                                 break;
                                             case '6'://delete a file with a name
                                                 String delFileName = data.substring(1);
                                                 FileManager.deleteFile(delFileName);
                                                 break;
                                             case '7'://split file with a name and copies
                                                 String splitFileName = data.substring(2);
                                                 int copies = Integer.parseInt(String.valueOf(data.charAt(1)));
                                                 FileManager.SplitFile(splitFileName, copies);
                                                 break;
                                             case '8'://metadata of a file, same concept as 4
                                                 String metaData = data.substring(1);
                                                 if (imAdmin) {
                                                     GuiMain.metaText.setText(metaData);
                                                 } else {
                                                     String metaText = FileManager.metadata(metaData);
                                                     channel.send(msg.getSrc(), "8" + metaText);
                                                 }
                                                 break;
                                             case '9'://shows the storage data of the device, same concept as 4
                                                 if (imAdmin) {
                                                     GuiMain.storageText.setText(data.substring(2));
                                                 } else
                                                     channel.send(msg.getSrc(), "9" + FileManager.storageStats());
                                                 break;
                                         }
                                     }
                                 } catch (Exception e) {
                                     if (!msg.getSrc().toString().equals(admin.toString())) { //if message was not received from admin, it is a chunk form a host
                                         // if the message is not a string, it is a file
                                         byte[] message = msg.getObject(); //we cast the message to a byte array
                                         int hsize = (int) message[0]; //we extract the header size which is stored in the first element of the array
                                         byte[] recHeader = new byte[hsize]; //we define an array for storing the header
                                         System.arraycopy(message, 1, recHeader, 0, (hsize)); //we copy the header into the array
                                         String filename = new String(recHeader, StandardCharsets.UTF_8); //we cast the header to a string
                                         Channel.logger.info("received file: " + filename); //log the file we received
                                         byte[] recMessage = new byte[message.length - (hsize + 1)];  //array to store the file bytes
                                         System.arraycopy(message, hsize + 1, recMessage, 0, message.length - (hsize + 1)); //copy the file to the array
                                         String[] temp = filename.split("[\\*]"); //split the header to extract information
                                         //we then write the file to the file defined in the header
                                         try  {
                                             OutputStream out = new BufferedOutputStream(new FileOutputStream("Files/ReceivedFiles/" + temp[0]));



                                           //  ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream("Files/ReceivedFiles/" + temp[0]));
                                             //zipOut.putNextEntry(zipEntry);
                                             //ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                                             //zipOut.putNextEntry(zipEntry);
                                             out.write(recMessage);
                                             out.close();
                                             //store the files metadata in memory
                                             FileStorageSytem newfile = new FileStorageSytem(temp[0], msg.getDest(), Integer.parseInt(temp[2]), Integer.parseInt(temp[3]), Integer.parseInt(temp[4]), temp[5], msg.getSrc());
                                             if (!FileManager.files.contains(newfile))
                                                 FileManager.files.add(newfile);



                                         } catch (Exception fileNotFoundException) {
                                             fileNotFoundException.printStackTrace();
                                         }
                                     } else { //the message is received from admin, it is an uploaded file, that we received
                                         byte[] message = msg.getObject(); //we cast the message to a byte array
                                         int hsize = (int) message[0]; //we extract the header size which is stored in the first element of the array
                                         byte[] recHeader = new byte[hsize]; //we define an array for storing the header
                                         System.arraycopy(message, 1, recHeader, 0, (hsize)); //we copy the header into the array
                                         String filename = new String(recHeader, StandardCharsets.UTF_8); //we cast the filename to a string
                                         Channel.logger.info("received file: " + filename); //log the file we received
                                         byte[] recMessage = new byte[message.length - (hsize + 1)];  //array to store the file bytes
                                         System.arraycopy(message, hsize + 1, recMessage, 0, message.length - (hsize + 1)); //copy the file to the array
                                         try  { // write the file to uploaded files

                                             OutputStream out = new BufferedOutputStream(new FileOutputStream("Files/UploadedFiles/" + filename));
                                             out.write(recMessage);
                                             out.close();
                                         } catch (Exception fileNotFoundException) {
                                             fileNotFoundException.printStackTrace();
                                         }
                                     }
                                 }
                             }

                             public void viewAccepted(View view) {  // when a change has been made to the members of a channel
                                 List<Address> old = members; //copy the old members
                                 members = channel.getView().getMembers(); //get the new members
                                 for (int i = 0; i < old.size(); i++) {
                                     if (!members.contains(old.get(i))) {//check if each old member is in the new member list
                                         //if they are not
                                         Channel.logger.info(old.get(i) + " has left the group"); //log the device that left
                                         if (!imAdmin)
                                             FileManager.disconnectHost(old.get(i)); //disconnect the host
                                     }
                                 }
                                 Channel.logger.info("The members have been updated: " + members);
                             }
                         }
        );
    }

    //function to say Admin on the channel
    public void sayAdmin() throws Exception {
        imAdmin = true; //define that this device is admin
        //create a message with a string and send it on the channel
        ObjectMessage objectMessage = new ObjectMessage(null, String.format("Admin"));
        admin = channel.getAddress();
        channel.send((Message) objectMessage);
        Thread.sleep(2000L); //sleep so that the channel members can be established
    }

    public static List<Protocol> gossipRouter() throws UnknownHostException {
        List<Protocol> protocols = new ArrayList<>();
        TUNNEL tunnel = new TUNNEL();
        try {
            InetAddress grAddress = InetAddress.getByName("gossip-router");
            logger.info("Found gossip router at {} (using it)", grAddress);
            tunnel.setGossipRouterHosts("gossip-router[12001]");
        } catch (UnknownHostException e) {
            System.setProperty("jgroups.bind_addr", "127.0.0.1");
            tunnel.setGossipRouterHosts("127.0.0.1[12001]");
        }
        protocols.add(tunnel);
        protocols.add(new PING());
        protocols.add(new MERGE3());
        protocols.add(new FD_ALL3());
        protocols.add(new VERIFY_SUSPECT());
        protocols.add(new NAKACK2().useMcastXmit(false));
        protocols.add(new UNICAST3());
        protocols.add(new STABLE());
        protocols.add(new GMS());
        protocols.add(new UFC());
        protocols.add(new MFC());
        protocols.add(new FRAG2());
        protocols.add(new STATE_TRANSFER());
        return protocols;
    }
}
