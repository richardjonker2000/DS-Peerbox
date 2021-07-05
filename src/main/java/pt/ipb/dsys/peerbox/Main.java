package pt.ipb.dsys.peerbox;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.TUNNEL;
import org.jgroups.stack.Protocol;


public class Main {
    public static Channel current = new Channel();

    public static void main(String[] args) {
        try {
            current.startChannel();
            Thread.sleep(5000);
            while (true){
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}