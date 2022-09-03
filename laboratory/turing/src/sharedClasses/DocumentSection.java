package sharedClasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//classe che rappresenta la sezione di un documento
public class DocumentSection {

    private String sectionContent;
    private Path docSectionPathObj;
    private int byteLength;
    private User lockedBy;

    /**
     * @param docSectionPath path della sezione
     */
    public DocumentSection(Path docSectionPath) {
        this.docSectionPathObj = docSectionPath;
        this.sectionContent = "";
        this.lockedBy = null;
    }

    /**
     * @effects richiede quale utente ha lockato la sezione
     * @return utente che ha lockato questa sezione
     */
    public User getLocked() {
        return lockedBy;
    }

    /**
     * @effects setta l'utente che blocca la sezione (per editarla)
     * @param lockedBy utente che locka questa sezione
     */
    public void setLocked(User lockedBy) {
        this.lockedBy = lockedBy;
    }

    /**
     * @effects richiede path della sezione
     * @return path della sezione
     */
    public Path getDocSectionPath() {
        return docSectionPathObj;
    }

    /**
     * @effects imposta il path della sezione
     * @param docSectionPath path della sezione
     */
    public void setDocSectionPath(Path docSectionPath) {
        this.docSectionPathObj = docSectionPath;
    }

    /**
     * @effects richiede lunghezza in byte della sezione
     * @return lunghezza in byte della sezione
     */
    public int getByteLength() {
        return (int) docSectionPathObj.toFile().length();
    }

    /**
     * @effects imposta la lunghezza in byte della sezione
     * @param byteLength lunghezza in byte della sezione
     */
    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    /**
     * @effects inserisce contenuto nella sezione del documento
     * @param sectionContent nuovo contenuto della sezione
     */
    public void setSectionContent(String sectionContent){
        try {
            Files.write(this.docSectionPathObj,
                    sectionContent.getBytes()
            );
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

     /**
     * @effects richiede il contenuto nella sezione del documento
     * @return contenuto della sezione
     */
    public String getSectionContent(){
        return Document.readFile(this.docSectionPathObj);
    }
}
