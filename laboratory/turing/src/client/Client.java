package client;
import sharedClasses.*;

import com.google.gson.*;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.RegisterInterface;

//classe che rappresenta il client della applicazione turing
public class Client {

    public Scanner keyboard; //gestore input da tastiera
    public String clientUsername;
    public String clientPassword;

    /***
     * docsDirPathObject e' il path della cartella di tutti i documenti, associati
     * ai vari utenti che si loggano tramite questo client (un utente alla volta)
        * e' tenuta in considerazione che piu' utenti possano usare lo stesso
          computer e la stessa applicazione client per usare TURING
     */
    public Path docsDirPathObject;
    public boolean clientIsOnline;
    public boolean clientIsEditing;
    public InetSocketAddress addr;
    public SocketChannel clientSocket;
    public SocketChannel inviteSocket;

    public Thread inviteThread;
    public Thread chatThread;
    public ChatHandler chatTask;
    public TuringInviteHandler inviteTask;


    public Path clientDocsDir; // cartella di tutti documenti dell'utente attuale
    public Path nowEditingSection; //file in editing
    public Path nowEditngDocument; //cartella documento in editing

    // ######## METODI PER LEGGIBILITA' ######## //
    public void askUsername(Message msg){
        System.out.print("  username: ");
        String read = this.keyboard.nextLine();

        while(acceptableString(read) != true){
          System.out.println("\nerrore di immissione: inserisci un username valido");
          System.out.println("l'username deve contenere almeno 6 caratteri\n");

          System.out.print("  username: ");
          read = this.keyboard.nextLine();
        }

        msg.setUsername(read);
    }

    public void askPassword(Message msg){
        System.out.print("  password: ");
        String read = this.keyboard.nextLine();

        while(acceptableString(read) != true){
          System.out.println("\nerrore di immissione: inserisci una password valida");
          System.out.println("la password deve contenere almeno 6 caratteri\n");
          System.out.print("  password: ");
          read = this.keyboard.nextLine();
        }

        msg.setPassword(read);
    }

    public void askDocumentName(Message msg){
        System.out.print("  nome del documento: ");
        String read = this.keyboard.nextLine();

        while(acceptableString(read) != true){
          System.out.println("\nerrore di immissione: inserisci un nome valido");
          System.out.println("il nome deve contenere almeno 6 caratteri \n");
          System.out.print("  nome del documento: ");
          read = this.keyboard.nextLine();
        }

        msg.setDocumentName(read);
    }

    public void askNewEditor(Message msg){
        System.out.print("  username del nuovo editor: ");
        String read = this.keyboard.nextLine();

        while(acceptableString(read) != true){
          System.out.println("\nerrore di immissione: inserisci un username valido");
          System.out.println("l'username deve contenere almeno 6 caratteri\n");
          System.out.print("  username del nuovo editor: ");
          read = this.keyboard.nextLine();
        }

      msg.setInvitedEditorUsername(read);

    }

    public void askDocumentSection(Message msg){
        System.out.print("  sezione del documento: ");
        String read = this.keyboard.nextLine();

        while(acceptableInt(read) != true){
          System.out.println("\nerrore di immissione: inserisci un numero intero");
          System.out.print("  sezione del documento: ");
          read = this.keyboard.nextLine();
        }

        msg.setSelectedSection(Integer.parseInt(read));

    }

    public void askNumOfSections(Message msg){
        System.out.print("  numero sezioni: ");
        String read = this.keyboard.nextLine();

        while(acceptableInt(read) != true){
          System.out.println("\nerrore di immissione: inserisci un numero intero");
          System.out.print("  numero sezioni: ");
          read = this.keyboard.nextLine();
        }

        msg.setNumOfSections(Integer.parseInt(read));
    }

    public void createFile(Path path){
        try {
            Files.createFile(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void createDirectory(Path path){
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void removeFile(Path path){
        try {
            Files.delete(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects manda un messaggio al server
     * @param msg messaggio da inviare
     */
    public void sendRequest(Message msg){
        //lunghezza in byte del messaggio
        int msgByteLength = Message.typeMsgByteLength(msg);
        try {
            //comunica al server la lunghezza del messaggio
            Connection.sendMessageByteLength(this.clientSocket, msgByteLength);
            //invia il messaggio
            Connection.sendMessage(msg, this.clientSocket, msgByteLength);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects causa l'inserimento della key nel selettore cosi'
     *          che il server sappia a quale utente appartiene quella key
     * @param msg messaggio di configurazione per la socket di gestione inviti
     */
    public void sendInviteHello(Message msg){
        int helloByteLength = Message.typeMsgByteLength(msg);
        try {
            Connection.sendMessageByteLength(this.inviteSocket, helloByteLength);
            Connection.sendMessage(msg, this.inviteSocket, helloByteLength);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects legge risposta dal server
     * @return res messaggio di risposta dal server
     */
    public Message readResponse(){
        int resByteLength;
        Message res = null;
        try {
            //legge la risposta
            resByteLength = Connection.readMessageByteLength(clientSocket);
            res = Connection.readMessage(this.clientSocket, resByteLength);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    // ######## Metodi del Client ######## //
    public static void main(String[] args){
        Client turingClient = new Client();
        turingClient.startClient();
    }

    public Client() {
        this.keyboard = new Scanner(System.in);
        //porta scelta a caso
        this.addr = new InetSocketAddress("localhost", 33333);
        this.clientSocket = null;
        this.inviteSocket = null;
        this.inviteTask = null;
        this.inviteThread = null;
        this.chatTask = null;
        this.chatThread = null;

        this.docsDirPathObject = null;
        this.clientDocsDir = null;
        this.clientUsername = null;
        this.clientPassword = null;
        this.clientIsOnline = false;
        this.clientIsEditing = false;
        this.nowEditingSection = null;
        this.nowEditngDocument = null;
    }

    public void startClient(){
        String documentsDirPath = "all_documents/client/";
        this.docsDirPathObject = Paths.get(documentsDirPath);
        this.createDirectory(this.docsDirPathObject);

        System.out.println("-- Benvenuto in TURING --");
        System.out.println("   per chiedere aiuto: turing --help");
        String userInput;

        boolean keepRunning = true;
        while (keepRunning) {
            System.out.print("\n> ");
            userInput = keyboard.nextLine();

            switch (userInput) {
                case "turing register":
                    this.turingRegister();
                    break;
                case "turing login":
                    this.turingLogin();
                    break;
                case "turing create":
                    this.turingCreateDocument();
                    break;
                case "turing logout":
                    keepRunning = false;
                    this.turingLogout();
                    break;
                case "turing edit":
                    this.turingEditDocument();
                    break;
                case "turing show":
                    this.turingShowDocument();
                    break;
                case "turing list":
                    this.turingListDocuments();
                    break;
                case "turing --help":
                    this.turingHelp();
                    break;
                case "turing end edit":
                    this.turingEndEditDoc();
                    break;
                case "turing share":
                    this.turingShareDocument();
                    break;
                case "turing send":
                    this.turingChatSend();
                    break;
                case "turing receive":
                    this.turingChatReceive();
                    break;
                default:
                    System.err.println("input errato: turing --help");
            }
        }
    }

    /**
     * @effects invoca metodo RMI e registra un utente
     */
    public void turingRegister(){
        if(this.clientIsOnline == true){
          System.out.println("errore: per registrare un altro utente esegui il logout");
          return;
        }
        Message msg = new Message(TuringValues.REGISTER);
        this.askUsername(msg);
        this.askPassword(msg);

        int port = 22222; //valore a caso
        RegisterInterface serverObject;
        Remote remoteObject;
        TuringValues result;

        try {
            Registry registry = LocateRegistry.getRegistry(port);
            remoteObject = registry.lookup("register");
            serverObject = (RegisterInterface) remoteObject;

            if((result = serverObject.turingRegister(msg)) != TuringValues.OP_OK)
                this.turingErrorHandling(result);
            else
                System.out.println("registrazione completata");
        }catch(RemoteException ex){
            ex.printStackTrace();
        }catch(NotBoundException ex){
            ex.printStackTrace();
        }
    }

    /**
     * @effects logga l'utente
     */
    public void turingLogin(){
        try {
            this.clientSocket = SocketChannel.open(this.addr);
            this.inviteSocket = SocketChannel.open(this.addr);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //crea messaggio
        Message msg = new Message(TuringValues.LOGIN);
        this.askUsername(msg);
        this.askPassword(msg);

        //invia messaggio
        this.sendRequest(msg);
        //legge risposta
        Message res = this.readResponse();

        //il server ha riscontrato un errore
        if (res.getOpResult() != TuringValues.OP_OK){
            this.turingErrorHandling(res.getOpResult());
            return;
        }

        System.out.println("login completato");
        this.clientIsOnline = true;
        this.clientUsername = msg.getUsername();
        this.clientPassword = msg.getPassword();

        //crea la cartella associata allo specifico utente connesso
        String documentDirPath =
            this.docsDirPathObject.toString() +
            "/" +
            this.clientUsername
        ;

        this.clientDocsDir = Paths.get(documentDirPath);
        this.createDirectory(this.clientDocsDir);

        /*
            controlla che l'utente non sia stato invitato ad editare documenti
            mentre era offline
        */
        if (res.getUnseenInvites() != null) {
            System.out.println("** sei stato a collaborare con i seguenti documenti **");
            for (String s : res.getUnseenInvites())
                System.out.println("   " + s);
        }

        //"sveglia" la socket relativa agli inviti cosi' il server
        // possa associarla allo specifico utente
        Message hello = new Message(TuringValues.INVITE_HELLO);
        hello.setUsername(this.clientUsername);
        this.sendInviteHello(hello);

        //fa partire il thread che aspetta gli inviti
        this.inviteTask = new TuringInviteHandler(this.inviteSocket);
        this.inviteThread = new Thread(inviteTask);
        this.inviteThread.start();
    }

    /**
     @effects crea un documento nel server
     */
    public void turingCreateDocument(){

        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        Message msg = new Message(TuringValues.CREATE);
        msg.setUsername(this.clientUsername);
        msg.setPassword(this.clientPassword);
        this.askDocumentName(msg);
        this.askNumOfSections(msg);

        this.sendRequest(msg);
        Message res = this.readResponse();

        if (res.getOpResult() == TuringValues.OP_OK)
            System.out.println("creazione documento completata");
        else
            this.turingErrorHandling(res.getOpResult());
    }

    /**
     * @effects mostra un documento
     */
    public void turingShowDocument(){
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        Message msg = new Message(TuringValues.SHOW);
        Message res;
        msg.setUsername(this.clientUsername);
        this.askDocumentName(msg);

        System.out.print("  mostrare tutto il documento (y or n): ");
        String opt = this.keyboard.nextLine();

        while((opt.equals("y") == false) && (opt.equals("n") == false)){
          System.out.print("errore, inserire y oppure n: ");
          opt = this.keyboard.nextLine();
        }

        if(!opt.equals("y")) {
            msg.setOp(TuringValues.SHOW_SECTION);
            this.askDocumentSection(msg);

            this.sendRequest(msg);
            res = this.readResponse();

            if((res != null) && (res.getOpResult() == TuringValues.OP_OK)){
                System.out.println("\nla sezione richiesta: ");
                System.out.println(res.getDocumentContent());
            }
        }else {
            this.sendRequest(msg);
            res = this.readResponse();

            if((res != null) && (res.getOpResult() == TuringValues.OP_OK)){
                System.out.println("ecco il documento: ");
                System.out.println(res.getDocumentContent());
            }
        }

        if(res.getOpResult() != TuringValues.OP_OK)
            this.turingErrorHandling(res.getOpResult());
    }

    /**
     * @effects chiede l'editing di un documento
     */
    public void turingEditDocument(){
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        if(this.clientIsEditing == true){
          this.turingErrorHandling(TuringValues.ALREADY_EDITING);
          return;
        }

        Message msg = new Message(TuringValues.EDIT);
        msg.setUsername(this.clientUsername);
        this.askDocumentName(msg);
        this.askDocumentSection(msg);

        this.sendRequest(msg);
        Message res = this.readResponse();

        if(res.getOpResult() != TuringValues.OP_OK){
            this.turingErrorHandling(res.getOpResult());
            return;
        }

        this.clientIsEditing = true;

        //creo documento (directory di file (sezioni))
        String documentPath =
            this.clientDocsDir.toString()
            + "/"
            + msg.getDocumentName()
        ;
        this.nowEditngDocument = Paths.get(documentPath);
        this.createDirectory(Paths.get(documentPath));

        //creo sezione del file
        String documentSectionPath =
            documentPath + "/" +
            msg.getSelectedSection() +
            ".txt"
        ;
        this.nowEditingSection = Paths.get(documentSectionPath);
        this.createFile(nowEditingSection);

        try {
            //scrivo nel file la versione che ha il server fino a quael momento
            Files.write(nowEditingSection,
                    res.getDocumentContent().getBytes()
            );
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //entro nella chat degli utenti che editano questo documento
        this.chatTask = new ChatHandler(msg.getUsername(), res.getChatPort());
        this.chatThread = new Thread(chatTask);
        this.chatThread.start();

        System.out.println("il file e' pronto per essere editato");
    }

    /**
     * @effects richiede la lista di documenti editabili dall'utente
     */
    public void turingListDocuments(){
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        Message msg = new Message(TuringValues.LIST);
        msg.setUsername(this.clientUsername);
        this.sendRequest(msg);
        Message res = this.readResponse();

        if(res.getOpResult() != TuringValues.OP_OK){
            this.turingErrorHandling(res.getOpResult());
            return;
        }

        /*
            lista di liste (elementi)
            un elemento rappresenta un documento:
                elemento(0) -> nome documento
                elemento(1) -> creatore documento
                elemento(i) con i >= 2 -> editors del documento
        */
        ArrayList<ArrayList<String>> docList = res.getRequestedDocsList();

        if(docList.size() != 0){
          for(ArrayList<String> doc : docList){
              System.out.println("Documento: " + doc.get(0));
              System.out.println("  Creatore: " + doc.get(1));
              System.out.print("  Collaboratori: ");

              for(int i = 2; i < doc.size(); i++)
                  System.out.print(doc.get(i) + " ");
          }
          System.out.println();
        }else{
          System.out.println("nessun documento disponibile");
        }
    }

    /**
     *  @effects termina l'edit inviando le modifiche al server
     */
    public void turingEndEditDoc(){
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        if(this.clientIsEditing == false){
          this.turingErrorHandling(TuringValues.NOT_EDITING);
          return;
        }

        Message msg = new Message(TuringValues.END_EDIT);
        msg.setUsername(this.clientUsername);
        this.askDocumentName(msg);
        this.askDocumentSection(msg);

        //legge il documento locale e lo immette nel messaggio
        String sectionContent = Document.readFile(this.nowEditingSection);
        msg.setDocumentContent(sectionContent);

        if(this.nowEditingSection.getFileName().toString().equals(msg.getSelectedSection() + ".txt") == false){
          this.turingErrorHandling(TuringValues.WRONG_SECTION);
          return;
        }

        //invia messaggio ed aspetta la risposta
        this.sendRequest(msg);
        Message res = this.readResponse();

        if(res.getOpResult() != TuringValues.OP_OK){
            this.turingErrorHandling(res.getOpResult());
            return;
        }

        System.out.println("modifiche salvate nel server");
        this.clientIsEditing = false;

        //cancello la versione locale del documento (che ora sta nel server)
        this.removeFile(this.nowEditingSection); //sezione in editing
        this.nowEditingSection = null;
        this.removeFile(this.nowEditngDocument); //directory documento
        this.nowEditngDocument = null;

        //gestisce l'uscita dalla chat
        this.chatTask.stopChat();
        try {
            this.chatThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        this.chatTask = null;
        this.chatThread = null;
    }

    /**
     * @effects invita un utente ad editare il documento
     */
    public void turingShareDocument(){
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        Message msg = new Message(TuringValues.SHARE);
        msg.setUsername(this.clientUsername);
        this.askDocumentName(msg);
        this.askNewEditor(msg);

        this.sendRequest(msg);
        Message res = this.readResponse();

        if(res.getOpResult() != TuringValues.OP_OK){
            this.turingErrorHandling(res.getOpResult());
            return;
        }else{
          System.out.println("documento condiviso correttamente");
        }
    }

    /**
     * @effects slogga l'utente
     */
    public void turingLogout() {
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }

        if(this.clientIsEditing == true){
           System.out.print(""
                + "sei in editing di un documento: "
                + "scartare le modifiche e procedere al logout? "
                + "(y or n) "
            );

            if (this.keyboard.nextLine().equals("y") == false)
                return;
        }

        Message msg = new Message(TuringValues.LOGOUT);
        msg.setUsername(this.clientUsername);
        this.sendRequest(msg);

        //fermo il thread che gestisce gli inviti
        this.inviteThread.interrupt();
        try {
            this.inviteThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        this.inviteThread = null;

        //chiudo le socket di comunicazione con il server
        try {
            this.clientSocket.close();
            this.inviteSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.clientSocket = null;
        this.inviteSocket = null;
        this.clientIsOnline = false;

        //rimuovo sezione e documento
        if(this.clientIsEditing){
            //rimuovo sezione in editing
            this.removeFile(this.nowEditingSection);
            this.nowEditingSection = null;

            //rimuovo cartella documento
            this.removeFile(this.nowEditngDocument);
            this.clientIsEditing = false;

            //fermo la chat del documento
            this.chatTask.stopChat();
            this.chatTask = null;
            this.chatThread = null;
        }

        //rimuovo cartella utente
        this.removeFile(this.clientDocsDir);
        this.clientDocsDir = null;
    }

    public void turingErrorHandling(TuringValues opResult) {
        switch(opResult){
            case ALREADY_REG:
                System.err.println("errore: utente gia' registrato");
                break;
            case NOT_EDITING:
                System.err.println("errore: non stai editando nulla");
                break;
            case ALREADY_LOGGED:
                System.err.println("errore: utente gia' loggato");
                break;
            case NOT_LOGGED:
                System.err.println("errore: utente non loggato");
                break;
            case DOC_LOCKED:
                System.err.println("errore: sezione documento gia' in editing");
                break;
            case UNREG_USER:
                System.err.println("errore: utente non registrato");
                break;
            case OP_FAIL:
                System.err.println("errore: operazione fallita");
                break;
            case UNAUTHORIZATED:
                System.err.println("errore: il documento non esiste " +
                                    "o non sei autorizzato alla operazione richiesta");
                break;
            case WRONG_PASSWORD:
                System.err.println("errore: password errata");
                break;
            case WRONG_SECTION:
                System.err.println("errore: non stai editando questa sezione");
                break;
            case UNKNOWN_DOCUMENT:
                System.err.println("errore: documento non esistente");
                break;
            case DOCUMENT_ALREADY_EXISTS:
                System.err.println("errore: documento gia' esistente");
                break;
            case ALREADY_EDITING:
                System.err.println("errore: puoi editare solo un documento alla volta");
                break;
            case SECTION_UNK:
                System.err.println("errore: sezione non esistente o non in editing");
                break;
        }
    }

    public void turingHelp() {
        System.out.println(
            "   \nregistrazione: turing register \n" +
            "       <username> \n" +
            "       <password> \n" +
            "   \nlogin: turing login \n" +
            "       <username> \n" +
            "       <password> \n" +
            "   \ncreazione documento: turing create \n" +
            "       <username> \n" +
            "       <password> \n" +
            "       <nome documento> \n" +
            "   \nvisualizzazione documento: turing show \n" +
            "       <nome documento> \n" +
            "   \nmodifica documento: turing edit \n" +
            "       <nome documento> \n" +
            "       <numero sezione> \n" +
            "   \nlista di documenti editabili: turing list \n"+
            "   \nfine editing documento: turing end edit \n" +
            "       <nome documento> \n" +
            "       <numero sezione> \n" +
            "   \ncondivisione documento: turing share \n" +
            "       <nome documento> \n" +
            "       <nome nuovo editor> \n" +
            "   \nlogout: turing logout \n" +
            "   \ninvio messaggio: turing send \n" +
            "     <testo messaggio> \n" +
            "   \nricezione messaggi: turing recive \n"
        );
    }

    public boolean acceptableString(String read){
      if(read == null)
        return false;
      if(read.length() < 6)
        return false;

      return true;
    }

    public boolean acceptableInt(String read){
      if(read == null)
        return false;

      if(read.isEmpty())
        return false;

      try{
        Integer.parseInt(read);
        return true;
      }catch(NumberFormatException ex){
        return false;
      }

    }

    public void turingChatSend() {
      if(this.clientIsOnline == false){
          this.turingErrorHandling(TuringValues.NOT_LOGGED);
          return;
      }

      if(this.clientIsEditing == false){
        System.out.println(
            "errore: puoi inviare messaggi solo agli utenti\n" +
            "        con cui stai editando un documento"
        );
        return;
      }

        System.out.print("  testo del messaggio: ");
        String chatText = this.keyboard.nextLine();
        while(chatText.isEmpty()){
          System.out.println("  errore: messaggio vuoto");
          System.out.print("  testo del messaggio: ");
          chatText = this.keyboard.nextLine();
        }
        ChatMessage chatMsg = new ChatMessage(chatText, this.clientUsername);
        this.chatTask.sendMessage(chatMsg);
        System.out.println("  messaggio inviato correttamente");
    }

    public void turingChatReceive() {
        if(this.clientIsOnline == false){
            this.turingErrorHandling(TuringValues.NOT_LOGGED);
            return;
        }
        if(this.clientIsEditing == false){
          this.turingErrorHandling(TuringValues.NOT_EDITING);
          return;
        }
        this.chatTask.printReveivedMessage();
    }
}
