package sharedClasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

//classe che rappresenta un documento
public class Document {
    
    private String documentName;
    private int numOfSections; //numero sezioni documento
    int chatPortNumber; //porta della chat

    private Path docObjectPath; //path in cui si trova il documento

    private User creator;
    private ArrayList<User> editors;
    private ArrayList<DocumentSection> sections;

    /**
     *
     * @param name
     * @param nSec
     * @param creator
     * @param docObjectPath
     */
    public Document(String name, int nSec, User creator, Path docObjectPath, int port) {
        this.sections = new ArrayList<>();
        this.editors = new ArrayList<>();
        this.documentName = name;
        this.numOfSections = nSec;
        this.creator = creator;
        this.docObjectPath = docObjectPath;
        this.chatPortNumber = port;
    }

    //usato anche nel client e nel server
    /**
     * @effects legge tutte le righe di un file
     * @param path path del file dal leggere
     * @return stringa di cio' che e' stato letto
     */
    public static String readFile(Path path){
        String sectionContent = "";
        
            try(BufferedReader reader = 
                Files.newBufferedReader(path, Charset.forName("UTF-8"))){
                String currentLine = null;
                while((currentLine = reader.readLine()) != null){
                    sectionContent = sectionContent.concat(currentLine);
                }

            }catch(IOException ex){
                ex.printStackTrace(); 
            }
        
        return sectionContent;
    }
    
    public Path getDocObjectPath() {
        return docObjectPath;
    }
    public int getChatPortNumber() {
        return chatPortNumber;
    }
    public void setChatPortNumber(int chatPortNumber) {
        this.chatPortNumber = chatPortNumber;
    }
    public String getDocumentName() {
        return documentName;
    }
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    public int getNumOfSections() {
        return numOfSections;
    }
    public void setNumOfSections(int numOfSections) {
        this.numOfSections = numOfSections;
    }
    public ArrayList<DocumentSection> getSections() {
        return sections;
    }
    public void setSections(ArrayList<DocumentSection> sections) {
        this.sections = sections;
    }
    public User getCreator() {
        return creator;
    }
    public void setCreator(User creator) {
        this.creator = creator;
    }
    public ArrayList<User> getEditors() {
        return editors;
    }
    public void setEditors(ArrayList<User> editors) {
        this.editors = editors;
    } 
}
