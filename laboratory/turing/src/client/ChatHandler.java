package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sharedClasses.ChatMessage;

//gestore della chat fra utenti che editano lo stesso documento
public class ChatHandler extends Thread {
    private String owner;
    private ArrayList<ChatMessage> history;
    private InetAddress group;
    private int port;
    private MulticastSocket ms;

    /**
     * @effects Costruttore del ChatHandler
     * @param owner username dell'utente client
     * @param port porta di comunicazione chat multicast
     */
    public ChatHandler(String owner, int port){
        this.port = port;
        this.owner = owner;
        this.history = new ArrayList<>();
        try {
            this.group = InetAddress.getByName("239.255.0.1");
            this.ms = new MulticastSocket(port);
            this.ms.joinGroup(group);  
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects: si mette in attesa di messaggi dal gruppo
     */
    @Override
    public void run() {
        byte[] buf = new byte [2200];
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket (buf, buf.length);
            try {
                ms.receive(packet);
                this.history.add(ChatMessage.fromBytesToMessage(packet.getData()));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @effects: interrompre la chat 
     */
    public void stopChat() {
        this.interrupt();
        try {
            this.ms.leaveGroup(this.group);
            this.ms.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @effects invia un messaggio
     * @param message messaggio da inviare
     */
    public void sendMessage(ChatMessage message){
        byte[] buf = ChatMessage.fromMessageToBytes(message);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
        try {
            this.ms.send(packet);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects: visualizza messaggi ricevuti
     */
    public void printReveivedMessage(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for(ChatMessage m: this.history){
            System.out.println(
                formatter.format(m.getDate()) +
                " " + m.getUsername() + 
                ": " + m.getMessageContent()
            );
        }
        this.history.clear();
    } 
}
