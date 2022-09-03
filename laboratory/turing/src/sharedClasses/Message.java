package sharedClasses;

import com.google.gson.Gson;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

//classe che rappresenta il messaggio che viene scambiato fra client e server
public class Message implements Serializable{
    public static Gson gson = new Gson();
    
    private TuringValues op; 
    private TuringValues opResult;

    private int numOfSections;
    private int selectedSection;
    private int messageByteLength;
    private int chatPort;
    
    private String username;
    private String documentContent;
    private String newEditor;
    private String chatMessageContent;
    private String password;
    private String documentName;
    private String invitedEditorUsername;
    private ArrayList<String> unseenInvites;
    private ArrayList<String> chatMessagesRecived; 
    private ArrayList<ArrayList<String>> requestedDocsList;

    /**
     * @param op operazione che si vuole chiedere al server
     */
    public Message(TuringValues op) {
        this.username = null;
        this.documentContent = null;
        this.newEditor = null;
        
        this.chatMessageContent = null;
        this.chatMessagesRecived = null;
        this.password = null;
        this.opResult = TuringValues.OP_FAIL;
        this.op = op;
        this.requestedDocsList = null;
        this.invitedEditorUsername = null;
        this.unseenInvites = null;
    }
    
    //getters & setters di eventuali campi del msg (che trasformati in json 
    //non saranno presenti qualora non fossero valorizzati (perche non necessari)
    public int getChatPort() {
        return chatPort;
    }
    public void setChatPort(int chatPort) {
        this.chatPort = chatPort;
    }
    public ArrayList<String> getUnseenInvites() {
        return unseenInvites;
    }
    public void setUnseenInvites(ArrayList<String> unseenInvites) {
        this.unseenInvites = unseenInvites;
    }
    public String getInvitedEditorUsername() {
        return invitedEditorUsername;
    } 
    public void setInvitedEditorUsername(String invitedEditorUsername) {
        this.invitedEditorUsername = invitedEditorUsername;
    }
    public ArrayList<ArrayList<String>> getRequestedDocsList() {
        return requestedDocsList;
    }  
    public void setRequestedDocsList(ArrayList<ArrayList<String>> requestedDocsList) {
        this.requestedDocsList = requestedDocsList;
    } 
    public int getMessageByteLength() {
        return messageByteLength;
    } 
    public void setMessageByteLength(int messageByteLength) {
        this.messageByteLength = messageByteLength;
    }
    public String getDocumentName() {
        return documentName;
    }
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    public String getPassword() {
        return password;
    } 
    public void setPassword(String password) {
        this.password = password;
    }
    public TuringValues getOp() {
        return this.op;
    }  
    public void setOp(TuringValues op) {
        this.op = op;
    }   
    public int getNumOfSections() {
        return numOfSections;
    } 
    public void setNumOfSections(int numOfSections) {
        this.numOfSections = numOfSections;
    } 
    public int getSelectedSection() {
        return selectedSection;
    }   
    public void setSelectedSection(int selectedSection) {
        this.selectedSection = selectedSection;
    }   
    public String getUsername() {
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public String getDocumentContent() {
        return documentContent;
    } 
    public void setDocumentContent(String documentContent) {
        this.documentContent = documentContent;
    }   
    public String getNewEditor() {
        return newEditor;
    }   
    public void setNewEditor(String newEditor) {
        this.newEditor = newEditor;
    } 
    public String getChatMessageContent() {
        return chatMessageContent;
    }
    public void setChatMessageContent(String chatMessageContent) {
        this.chatMessageContent = chatMessageContent;
    }  
    public ArrayList<String> getChatMessagesRecived() {
        return chatMessagesRecived;
    }
    public void setChatMessagesRecived(ArrayList<String> chatMessagesRecived) {
        this.chatMessagesRecived = chatMessagesRecived;
    } 
    public TuringValues getOpResult() {
        return opResult;
    }
    public void setOpResult(TuringValues opResult) {
        this.opResult = opResult;
    }
    public static int typeMsgByteLength(Message msg){
        return Message.gson.toJson(msg).getBytes().length;
    }
     
}
