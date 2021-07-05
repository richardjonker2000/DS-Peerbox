package pt.ipb.dsys.peerbox;

//import jdk.swing.interop.SwingInterOpUtils;
//import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jgroups.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import static ch.qos.logback.core.joran.action.ActionConst.NULL;

public class FileManager {
    static List<FileStorageSytem> files = new LinkedList<>(); //files sent and received stored in memory
    static int fileIdTracker = 0; //tracks the unique id for files uploaded on the system
    static Address host = Main.current.channel.getAddress();//the address of this device

    //function to split and send the files into chunks
    public static void SplitFile(String filename, int copies) {
        File f = new File("Files/UploadedFiles");
        String[] pathnames = f.list();//get a list of uploaded files
        for (String pathname : pathnames) { //iterate through each of the files
            if (pathname.equals(filename)) { //if the file matches the file in the parameter
                int counter = 0; //chunk counter to determine the id of the chunk
                int sizeOfChunk = 1024 * 64; //the size of a chunk in bytes
                String name = host + "." + fileIdTracker;//temporary name for the new chunk fies
                byte[] bytes = new byte[0]; //bytes to be read from the file
                try {
                    bytes = Files.readAllBytes(Paths.get("Files/UploadedFiles" + '/' + pathname)); //read all bytes from the file
                    int index = 0; //index for the byted to be read from the file
                    int length = bytes.length; //get the length of the of the file in bytes
                    while (index < bytes.length) { //check that the index is less than the amount of bytes already read
                        String newFileName = "Files/SentFiles/" + name + "." + counter; //the files new path to be stored
                        File newFile = new File(newFileName);//create a new file to write the chunk
                        try {
                            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));


                            byte[] newArray; //new array to store the data of a chunk
                            if (length < sizeOfChunk) {//checks the length is less than size
                                newArray = Arrays.copyOfRange(bytes, index, index + length);//if there is less than a
                                // chunk of data copy that much data
                            } else {
                                newArray = Arrays.copyOfRange(bytes, index, index + sizeOfChunk); //otherwise copy a chunk
                                length -= sizeOfChunk;//reduce the length of data
                            }
                            index += sizeOfChunk;//increase the index by a chunk

                            CompressorOutputStream gzippedOut = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, out);
                            gzippedOut.write(newArray);
                            gzippedOut.close();
                            //out.write(newArray);//write the data
                            //out.close();
                        } catch (IOException | CompressorException e) {
                            e.printStackTrace();
                        }
                        for (int i = 0; i < copies; i++) {//send a copy to a random device on the network
                            int ran = (int) Math.round(Math.random() * (Main.current.members.size() - 1));
                            while (Main.current.members.get(ran).toString().equals(Main.current.admin.toString())) {
                                ran = (int) Math.round(Math.random() * (Main.current.members.size() - 1));
                            }
                            sendFile(newFileName, host, Main.current.members.get(ran), fileIdTracker, counter, i, "Files/UploadedFiles/" + filename, "empty");
                            //Thread.sleep(50);
                        }
                        counter++; //increase the amount of chunks
                    }
                    fileIdTracker++; //increase the file counter
                } catch (IOException  e) {
                    e.printStackTrace();
                }
            } else {
                Channel.logger.info("File Not Found");
            }
        }
    }

    //function to send a file
    public static void sendFile(String filePath, Address host, Address dest, int fileId, int chunk, int copyid, String originalFile, String tempFileName) {
        try {
            Channel.logger.info("sending File to: " + dest); //log that we are sending a file
            //FileInputStream in = new FileInputStream(filePath); //input stream to a the file
            byte[] data = Files.readAllBytes(Paths.get(filePath)); //retrieve the bytes from the files
            if (tempFileName.equals("empty")) //if the tempfile name is empty we define the file name to save the file as
                tempFileName = host + "." + fileId + "." + chunk + "." + copyid;
            //define the header of the file
            byte[] header = (tempFileName + "*" + host + "*" + fileId + "*" + chunk + "*" + copyid + "*" + originalFile).getBytes();
            int messagesize = 1 + header.length + data.length; //get the size of the message to be sent
            byte[] message = new byte[messagesize]; //define the message, which is a byte array
            message[0] = (byte) header.length; //set the first value as the size header for de-encoding the message
            System.arraycopy(header, 0, message, 1, header.length); //copy the header to the message
            System.arraycopy(data, 0, message, header.length + 1, data.length); //copy the data to the message
            ObjectMessage message1 = new ObjectMessage(dest, message); //create the message
            Main.current.channel.send(message1); //send the message
            //create and add the file to the filestoragesystem
            FileStorageSytem newfile = new FileStorageSytem(tempFileName, host, fileId, chunk, copyid, originalFile, dest);
            if (!files.contains(newfile))
                files.add(newfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //function to retrieve the file
    public static void retrieveFile(String fileName) {
        List<FileStorageSytem> retrievableFiles = new LinkedList<>(); //linked list for suitable for files to be retrieved
        for (int i = 0; i < files.size(); i++) { //loop through the files and add the ones which are relevant to the provided file
            FileStorageSytem file = files.get(i);
            if (("Files/UploadedFiles/" + fileName).equals(file.getFilepath())) {
                boolean temp = true;
                for (int j = 0; j < retrievableFiles.size(); j++) {
                    if (file.toString().equals(retrievableFiles.get(j).toString())) {
                        temp = false;
                    }
                }
                if (temp)
                    retrievableFiles.add(file);
            }
        }
        int chunkCounter = 0; //variable to store the amount of chunks
        int copies = 1; //variable to store the amount of copies
        if (retrievableFiles.size() == 0) { //if there are no files selected, it means the system has no information about the file
            Channel.logger.info(" File not found");
        } else {
            while (chunkCounter < retrievableFiles.size() / copies) { //iterate through each of the chunks
                for (int i = 0; i < retrievableFiles.size(); i++) { //check through each of the files
                    FileStorageSytem file = retrievableFiles.get(i);
                    if (file.getCopyNumber() + 1 > copies) { //identify how many copies the file has
                        copies = file.getCopyNumber() + 1;
                    }
                    if (file.getChunkID() == chunkCounter) { //if we find the chunk we need, we retrieve it
                        Channel.logger.info("Request file from " + file.getDest());
                        String tmp = "1" + file.getFilename();
                        ObjectMessage message = new ObjectMessage(file.getDest(), tmp); //send a message to the location of the
                        //file that is being stored, requesting it
                        try {
                            Main.current.channel.send(message); //send the message and wait so that the file can be received
                            Thread.sleep(100);
                            //check if the file was received
                            File f = new File("Files/ReceivedFiles");
                            String[] pathnames = f.list();
                            boolean temp = false;
                            for (String pathname : pathnames) {
                                if (pathname.equals(file.getFilename())) {
                                    temp = true;
                                    chunkCounter++;//increase the amount of chunks
                                    break;
                                }
                            }
                            if (temp) //if file was received we break
                                break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            reconstructFile(fileName, chunkCounter - 1, retrievableFiles); // we then reconstruct the file
        }
    }

    //reconstruct a file from the chunks
    public static void reconstructFile(String fileName, int chunks, List<FileStorageSytem> retrievableFiles) {
        File newFile = new File("Files/RetrievedFiles", fileName);  //create the file to write to
        int readChunks = 0; //amount of chunks read
        int counter = 0;
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));
            while (readChunks <= chunks && counter < retrievableFiles.size() * chunks) { //iterate until we have gone through all the chunks
                for (int i = 0; i < retrievableFiles.size(); i++) { //go through all the retrievable files
                    FileStorageSytem file = retrievableFiles.get(i);
                    if (file.getChunkID() == readChunks) { //look for the chunk
                        try {
                            //read the data from the file
                            CompressorInputStream gzippedIn = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, new BufferedInputStream(new FileInputStream("Files/ReceivedFiles/" + file.getFilename())));
                            byte[] bytes = gzippedIn.readAllBytes();
                            gzippedIn.close();

                            //byte[] bytes = Files.readAllBytes(Paths.get("Files/ReceivedFiles/" + file.getFilename()));
                            //write the data to the file
                            out.write(bytes);
                            readChunks++; //increase chunks read
                            break;

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    counter++;
                }

            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void deleteFile(String fileName) { //fuinction to delete a file
        List<FileStorageSytem> temp =  new LinkedList<>(files);

        for (int i = 0; i < temp.size(); i++) { //check through each of the files
            FileStorageSytem file = temp.get(i);
            System.out.println(file);
            if (file.getFilepath().equals("Files/UploadedFiles/" + fileName) && file.getHost().toString().equals(host.toString())) { //check if the filepath contains the file name
                System.out.println(file);
                ObjectMessage message = new ObjectMessage(file.getDest(), "2" + file.getFilename()); //send a message to the location of the file
                try {
                    Channel.logger.info("sending message to " + file.getDest() + " to delete file " + file.getFilename());
                    Main.current.channel.send(message); //send the message so that the file can be deleted
                    Thread.sleep(50);
                    files.remove(file);

                    File sent = new File("Files/SentFiles/" + file.getFilename().substring(0, file.getFilename().lastIndexOf('.')));
                    //we then deleted the sent file associated with that chunk
                    if (!sent.delete()) {
                        Channel.logger.error(sent + " failed to delete");
                    } else
                        Channel.logger.info(sent + " sent file Deleted");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deleteFileRec(String filePath) { //function to delete a file received
        Channel.logger.info("Del: " + filePath);
        File del = new File(filePath);
        if (!del.delete()) {
            Channel.logger.error(del + " failed to delete");
        } else
            Channel.logger.info(del + " file Deleted");
    }

    public static void disconnectHost(Address down) { //fucntion to handle the failure of a hose
        List<FileStorageSytem> temp = new LinkedList<>(files);
        for (FileStorageSytem file : temp) {
            if (file.getDest().toString().equals(down.toString()) && file.getFilename().contains(host.toString())) {
                //files sent to device
                int ran = (int) Math.round(Math.random() * (Main.current.members.size() - 1));
                while (Main.current.members.get(ran).toString().equals(Main.current.admin.toString())) {
                    ran = (int) Math.round(Math.random() * (Main.current.members.size() - 1));
                }
                sendFile("Files/SentFiles/" + file.getFilename().substring(0, file.getFilename().lastIndexOf('.')), host, Main.current.members.get(ran), file.getFileID(), file.getChunkID(), file.getCopyNumber(), file.getFilepath(), "empty");
                Channel.logger.info("Device down");
            } else if (file.getFilename().contains(down.toString())) {
                //files received from the host
                Channel.logger.info("Deleting File ");
                deleteFileRec("Files/ReceivedFiles/" + file.getFilename());
                files.remove(file);
            }
        }
    }


    public static String showFilesStored() {
        String ret = "Files that were sent to and from this device\n";
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            ret += files.get(i).formatOutput() + "\n";
            counter++;
        }
        ret += "There were " + counter + " files sent to and from this device.";
        return ret;
    }

    public static String listFiles() {
        String ret = "";
        for (int i = 0; i < files.size(); i++) {
            if (!ret.contains(files.get(i).getFilepath()) && files.get(i).getHost().toString().equals(host.toString())) {
                ret = ret + files.get(i).getFilepath() + "\n";
            }
        }
        return ret;
    }

    public static String metadata(String filePath) {
        String ret = "";
        ret = "All information related to " + filePath + "\n";
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getFilepath().equals("Files/UploadedFiles/" + filePath))
                ret += files.get(i).formatOutput() + "\n";
        }
        return ret;
    }

    public static String storageStats() {
        File file = new File("Files");
        String ret = "";
        ret += file.getAbsolutePath() + "\n";
        ret += String.format("Used space: %.2f MB", (double) folderSize(file) / 1048576) + "\n";
        ret += String.format("Free space: %.2f MB", (double) file.getFreeSpace() / 1048576) + "\n";
        return ret;
    }

    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

}
