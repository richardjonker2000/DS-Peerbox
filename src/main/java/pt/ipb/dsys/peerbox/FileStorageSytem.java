package pt.ipb.dsys.peerbox;

import org.jgroups.Address;

import java.util.LinkedList;
import java.util.List;

public class FileStorageSytem {

    private String filename;
    private Address host;
    private int fileID;
    private int chunkID;
    private int copyNumber;
    private String filepath;
    private Address dest;

    public FileStorageSytem(Address host, int fileID, int chunkID,  int copyNumber) {
        this.host = host;
        this.fileID = fileID;
        this.chunkID = chunkID;
        this.copyNumber = copyNumber;
    }
    public FileStorageSytem(Address host, int fileID, int chunkID,  int copyNumber, Address dest) {
        this.host = host;
        this.fileID = fileID;
        this.chunkID = chunkID;
        this.copyNumber = copyNumber;

        this.dest = dest;
    }
    public FileStorageSytem(String filename, Address host, int fileID, int chunkID,  int copyNumber, String filepath, Address dest) {
        this.filename = filename;
        this.host = host;
        this.fileID = fileID;
        this.chunkID = chunkID;
        this.copyNumber = copyNumber;
        this.filepath = filepath;
        this.dest = dest;
    }
    public void addDestinationAddress(Address a){
        dest=a;
    }

    public String formatOutput(){
        if(filename.contains(host.toString()))
            return "Copy "+copyNumber+" of Chunk "+chunkID+" of file "+ filepath+", was Sent From "+host +" to "+dest;
        else
            return "Copy "+copyNumber+" of Chunk "+chunkID+" of file "+ filepath+", was received From "+dest;
    }

    @Override
    public String toString() {
        return host +
                "*" + fileID +
                "*" + chunkID +
                "*" + copyNumber +
                "*" + dest+
                "*" + filepath
                ;
    }

    public String getFilepath() {
        return filepath;
    }

    public int getChunkID() {
        return chunkID;
    }

    public Address getHost() {
        return host;
    }

    public int getFileID() {
        return fileID;
    }

    public int getCopyNumber() {
        return copyNumber;
    }

    public Address getDest() {
        return dest;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object obj) {
        FileStorageSytem tmp = (FileStorageSytem)obj;
        return this.host == tmp.getHost() && this.filepath.equals(tmp.filepath) &&this.fileID == tmp.fileID && this.chunkID == tmp.chunkID
                && this.copyNumber == tmp.copyNumber;
    }


}
