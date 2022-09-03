package sharedClasses;

import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

//classe che rappresenta l'utente di turing
public class User {
    
    private String username;
    private String password;
    private ArrayList<String> unseenInvites;

    private boolean logged;
    private boolean registerd;
    
    private HashMap<String, Document> ownedDocuments;
    private HashMap<String, Document> editableDocuments;
    
    private SocketChannel inviteSocket;
    private Path docsPath; 
     /**
     * @param username nome nuovo utente
     * @param password password nuovo utente
     */
    public User(String username, String password) {
        this.ownedDocuments = new HashMap<>();
        this.editableDocuments = new HashMap<>();
        this.username = username;
        this.password = password;
        this.logged = false;
        this.docsPath = null;
        this.inviteSocket = null;
        this.unseenInvites = null;
    }
    
    public SocketChannel getInviteSocket() {
        return inviteSocket;
    }
    public void setInviteSocket(SocketChannel inviteSocket) {
        this.inviteSocket = inviteSocket;
    }
    public ArrayList<String> getUnseenInvites() {
        return unseenInvites;
    }
    public void setUnseenInvites(ArrayList<String> unseenInvites) {
        this.unseenInvites = unseenInvites;
    }
    public Path getDocsPath() {
        return docsPath;
    }
    public void setDocsPath(Path myDocuments) {
        this.docsPath = myDocuments;
    }
    public String getUsername() {
        return username;
    }
    public HashMap<String, Document> getOwnedDocuments() {
        return ownedDocuments;
    }
    public HashMap<String, Document> getEditableDocuments() {
        return editableDocuments;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isOnline() {
        return logged;
    }
    public void setOnline(boolean online) {
        this.logged = online;
    }
}
