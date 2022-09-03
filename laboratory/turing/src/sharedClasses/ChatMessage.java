package sharedClasses;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

//classe che rappresnta il messaggio che si inviano gli utenti nella chat multicast
public class ChatMessage implements Serializable{
    private String messageContent;
    private String username;
    private Date date;

    public ChatMessage(String messageContent, String username) {
        this.messageContent = messageContent;
        this.username = username;
        this.date = new Date();
    }

    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    
    
    public static byte[] fromMessageToBytes(ChatMessage m){
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream;
        try {
            objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(m);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return byteStream.toByteArray();
    }
    
    public static ChatMessage fromBytesToMessage(byte[] bytes){
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream;
        try {
            objStream = new ObjectInputStream(byteStream);
            return (ChatMessage) objStream.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null; //non lo fara' mai
    }
}
