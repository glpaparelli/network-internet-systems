package server;
import sharedClasses.*;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

//classe principale: turing server
public class Server extends RemoteServer
    implements RegisterInterface{

    private Map<String, User> turingUsers; //utenti di turing
    private HashMap<String, Document> turingDocuments; //documenti di turing
    private Path docsDirPathObject; //directory di tutti i documenti
    int chatPort;
    Selector selector;

    public static void main(String[] args) throws Exception{
        Server turingServer = new Server();
        try {
            turingServer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Server(){
        //vengono fatte solo operazioni elementari (put, get)
        this.turingUsers = new ConcurrentHashMap<>(); //necessaria per operazione composta nella register
        this.turingDocuments = new HashMap<>();

        //creo cartella di tutti i documenti
        String documentDirPath = "all_documents/server/";
        this.docsDirPathObject = Paths.get(documentDirPath);
        try {
            Files.createDirectories(docsDirPathObject);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //porta a caso
        this.chatPort = 33444;
    }

    public void start() throws Exception{
        //pubblico il metodo per la registrazione
        publishRegister();

        this.selector = Selector.open();

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        InetSocketAddress addr = new InetSocketAddress("localhost", 33333);

        serverSocket.bind(addr);
        serverSocket.configureBlocking(false);

        Selector selector = Selector.open();
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("\n** server pronto **");
        while(true){
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();

            while(keysIterator.hasNext()){
                SelectionKey actualKey = (SelectionKey) keysIterator.next();
                keysIterator.remove();
                try{
                  if(actualKey.isAcceptable()){
                      ServerSocketChannel sSocket =
                                  (ServerSocketChannel) actualKey.channel();

                      SocketChannel clientSocket = sSocket.accept();
                      clientSocket.configureBlocking(false);
                      SelectionKey newKey = clientSocket.register(selector, SelectionKey.OP_READ);

                  }else if(actualKey.isReadable()){
                      //un canale e' pronto per la lettura
                      SocketChannel clientSocket = (SocketChannel) actualKey.channel();

                      //controllo per chiusura della socket
                      int msgByteLength = Connection.readMessageByteLength(clientSocket);
                      if(msgByteLength == -1){
                          actualKey.cancel();
                          continue;
                      }
                      Message msg = Connection.readMessage(clientSocket, msgByteLength);
                      actualKey.interestOps(SelectionKey.OP_WRITE);
                      Message res = null;

                      switch(msg.getOp()){
                          case LOGIN:
                              res = this.turingLogin(msg, clientSocket);
                              break;
                          case CREATE:
                              res = this.turingCreateDocument(msg, clientSocket);
                              break;
                          case EDIT:
                              res = this.turingGetDocSection(msg, clientSocket);
                              break;
                          case SHOW_SECTION:
                              res = this.turingGetDocSection(msg, clientSocket);
                              break;
                          case SHOW:
                              res = this.turingShowDocument(msg, clientSocket);
                              break;
                          case LIST:
                              res = this.turingListDocuments(msg, clientSocket);
                              break;
                          case END_EDIT:
                              res = this.turingEndEdit(msg, clientSocket);
                              break;
                          case INVITE_HELLO:
                              this.setupInvite(msg, clientSocket);
                              break;
                          case SHARE:
                              res = this.turingShareDocument(msg, clientSocket);
                              break;
                          case LOGOUT:
                              this.turingLogout(msg, clientSocket);
                              break;
                      }
                      if(res != null)
                        actualKey.attach(res);
                  }
                  else if(actualKey.isWritable()){
                      Message res = (Message) actualKey.attachment();
                       //controllo per invite hello: le socket per gli inviti rimangono sempre in read
                      if(res != null){
                        SocketChannel clientSocket = (SocketChannel) actualKey.channel();

                        this.sendResponse(res, clientSocket);
                        actualKey.interestOps(SelectionKey.OP_READ);
                      }
                  }
                }catch (Exception ex){
                    actualKey.cancel();
                    try{
                      actualKey.channel().close();
                    }catch(IOException e){
                      e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     @effects pubblica oggetto remoto per la registrazione
     */
    public void publishRegister(){
        int port = 22222; //valore a caso
        try {
            RegisterInterface stub =
                (RegisterInterface) UnicastRemoteObject.exportObject(this, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("register", stub);

        }catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects registra l'utente
     * @param msg
     * @return TuringValues esito operazione
     */
    @Override
    public TuringValues turingRegister(Message msg) {
        User newUser = new User(msg.getUsername(), msg.getPassword());
        User oldUser = this.turingUsers.putIfAbsent(msg.getUsername(), newUser);

        //errore, utente con username gia' registrato
        if(oldUser != null){
          //rimetto il vecchio utente al suo posto
          this.turingUsers.put(oldUser.getUsername(), oldUser);
          return TuringValues.ALREADY_REG;
        }

        //creo la cartella utente in cui ci sono tutti i suoi documenti
        String userDocsDir =
            this.docsDirPathObject.toString() +
            "/" +
            newUser.getUsername()
        ;

        newUser.setDocsPath(Paths.get(userDocsDir));
        try {
            Files.createDirectories(newUser.getDocsPath());
        } catch (IOException ex) {
            ex.printStackTrace();
            return TuringValues.OP_FAIL;
        }

        return TuringValues.OP_OK;
    }

    /**
     * @effects logga l'utente
     * @param msg messaggio inviato dal client
     * @param clientSocket socket di comunicazione con il client
     * @return res risposta da inviare al client
     */
    public Message turingLogin(Message msg, SocketChannel clientSocket) throws Exception{
        Message res = new Message(TuringValues.OP_FAIL);
        User user = this.turingUsers.get(msg.getUsername());

        //utente non registrato
        if(user == null){
            res.setOpResult(TuringValues.UNREG_USER);
            return res;
        }

        //utente gia' loggato su un altro clint
        if(user.isOnline() == true){
            res.setOpResult(TuringValues.ALREADY_LOGGED);
            return res;
        }

        //password errata
        if(user.getPassword().equals(msg.getPassword()) == false){
            res.setOpResult(TuringValues.WRONG_PASSWORD);
            return res;
        }

        //password corretta, login accettabile
        user.setOnline(true);
        res.setOpResult(TuringValues.OP_OK);
        res.setUnseenInvites(user.getUnseenInvites());
        user.setUnseenInvites(null);

        return res;
    }

    /**
     * @effects crea un documento
     * @param msg
     * @param clientSocket
     * @return res risposta da inviare al client
     */
    public Message turingCreateDocument(Message msg, SocketChannel clientSocket){
        Message res = new Message(TuringValues.CREATE);
        User user = this.turingUsers.get(msg.getUsername());

        res.setOpResult(TuringValues.OP_OK);

        //crea la cartella del documento
        String documentPath =
            user.getDocsPath().toString()
            + "/"
            + msg.getDocumentName()
        ;

        try {
            Files.createDirectory(Paths.get(documentPath));
        } catch (IOException ex) {
            res.setOpResult(TuringValues.DOCUMENT_ALREADY_EXISTS);
            return res;
        }

        Document newDoc = new Document(
            msg.getDocumentName(),
            msg.getNumOfSections(),
            user,
            Paths.get(documentPath),
            this.chatPort
        );

        //ogni chat del documento ha una porta diversa' che verra'
        //comunicata al client
        this.chatPort++;

        ArrayList<DocumentSection> tmp = newDoc.getSections();

        //creo le n sezioni richieste (funziona come indici array)
        for (int i = 0; i < msg.getNumOfSections(); i++) {
            String docSection = documentPath + "/" + i + ".txt";
            Path docSecPathObj = Paths.get(docSection);

            tmp.add(new DocumentSection(docSecPathObj));

            try {
                Files.createFile(docSecPathObj);
            } catch (IOException ex) {
                ex.printStackTrace();
                res.setOpResult(TuringValues.OP_FAIL);
            }
        }

        //aggiunge il documento all'insieme di documenti di turing
        this.turingDocuments.put(newDoc.getDocumentName(), newDoc);
        //aggiunge il documento fra quelli posseduti dall'utente creatore
        user.getOwnedDocuments().put(newDoc.getDocumentName(), newDoc);
        return res;
    }

    /**
     * @effects ritorna al client il contenuto di una sezione di un documento
     *          usato per la edit e per la show di una sezione
     * @param msg
     * @param clientSocket
     * @return res risposta da inviare al client
     */
    public Message turingGetDocSection(Message msg, SocketChannel clientSocket){

        User user = this.turingUsers.get(msg.getUsername());
        Message res = new Message(TuringValues.EDIT);

        Document requestedDoc =
            user.getOwnedDocuments().get(msg.getDocumentName());

        //magari l'utente e' solo un editor
        if(requestedDoc == null){
            requestedDoc =
                user.getEditableDocuments().get(msg.getDocumentName());
        }

       //se requestedDoc e' ancora null significa che l'utente non e'
       //il proprietario e non e' un editor
       if(requestedDoc == null){
            res.setOpResult(TuringValues.UNAUTHORIZATED);
            return res;
        }

        //richeista sezione non esistente
        if(msg.getSelectedSection() >= requestedDoc.getNumOfSections()){
          res.setOpResult(TuringValues.SECTION_UNK);
          return res;
        }

        DocumentSection docSection =
            requestedDoc.getSections().get(msg.getSelectedSection());

        if(msg.getOp() == TuringValues.EDIT){
            if(docSection.getLocked() != null){
                res.setOpResult(TuringValues.DOC_LOCKED);
                return res;

            }else{
                docSection.setLocked(user);
                res.setChatPort(requestedDoc.getChatPortNumber());
            }
        }

        //qui ci arrivo solo se il documento e' richiesto in lettura
        //o se sono riuscito a bloccarlo per la modifica
        res.setOpResult(TuringValues.OP_OK);
        res.setDocumentName(msg.getDocumentName());
        res.setDocumentContent(docSection.getSectionContent());

        return res;
    }

    /**
     * @param msg
     * @param clientSocket
     * @return res risposta da inviare al client
     */
    public Message turingShowDocument(Message msg, SocketChannel clientSocket) {
        Message res = new Message(TuringValues.EDIT);
        User user = this.turingUsers.get(msg.getUsername());

        Document requestedDoc =
            user.getOwnedDocuments().get(msg.getDocumentName());

        if(requestedDoc == null){
            requestedDoc =
                user.getEditableDocuments().get(msg.getDocumentName());
        }

        //documento non presente fra quelli a cui puo' accedere il client
        if(requestedDoc == null){
            res.setOpResult(TuringValues.UNAUTHORIZATED);
            return res;
        }

        res.setOpResult(TuringValues.OP_OK);
        res.setDocumentName(msg.getDocumentName());

        //leggo tutto il documento
        String wholeDoc = "";
        for (DocumentSection section : requestedDoc.getSections())
            wholeDoc = wholeDoc.concat(section.getSectionContent() + "\n");

        res.setDocumentContent(wholeDoc);

      return res;
    }

    /**
     * @effects mostra la lista di documenti editabili dal client
     * @param msg
     * @param clientSocket
     * @return res risposta da inviare al client
     */
    public Message turingListDocuments(Message msg, SocketChannel clientSocket){

        User user = this.turingUsers.get(msg.getUsername());
        Message res = new Message(TuringValues.LIST);

        res.setOpResult(TuringValues.OP_OK);
        //lista dei documenti in cui e' coinvolto l'utente (owner o editor)
        ArrayList<Document> relevantDocs = new ArrayList<>();

        for(Document d:user.getOwnedDocuments().values())
            relevantDocs.add(d);

        for(Document d:user.getEditableDocuments().values())
            relevantDocs.add(d);

        ArrayList<ArrayList<String>> list = new ArrayList<>();

        /*
            lista di liste (elementi)
            un elemento rappresenta un documento:
                elemento(0) -> nome documento
                elemento(1) -> creatore documento
                elemento(i) con i >= 2 -> editors del documento
        */
        for(Document d : relevantDocs){
            ArrayList<String> usersOfDoc = new ArrayList<>();
            usersOfDoc.add(0, d.getDocumentName());
            usersOfDoc.add(1, d.getCreator().getUsername());
            int i = 2;
            for(User u : d.getEditors()){
                usersOfDoc.add(i, u.getUsername());
                i++;
            }
            list.add(usersOfDoc);
        }

        res.setRequestedDocsList(list);
        return res;
    }

    /**
     * @effects risponde al client in relazione alla richiesta fatta
     * @param res
     * @param clientSocket
     */
    public void sendResponse(Message res, SocketChannel clientSocket){
        int resByteLength = Message.typeMsgByteLength(res); //lunghezza in byte della rispsota
        try {
            //comunico lunghezza in byte della risposta
            Connection.sendMessageByteLength(clientSocket, resByteLength);
            //invio la risposta
            Connection.sendMessage(res, clientSocket, resByteLength);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @effects termina fase di editing
     * @param msg
     * @param clientSocket
     * @return res risposta da inviare al client
     */
    public Message turingEndEdit(Message msg, SocketChannel clientSocket){
        User user = this.turingUsers.get(msg.getUsername());
        Message res = new Message(TuringValues.END_EDIT);

        Document requestedDoc =
            user.getOwnedDocuments().get(msg.getDocumentName());

        if(requestedDoc == null)
            requestedDoc =
                user.getEditableDocuments().get(msg.getDocumentName());

        if(requestedDoc == null){
            res.setOpResult(TuringValues.UNAUTHORIZATED);
            return res;
        }

        DocumentSection docSection =
            requestedDoc.getSections().get(msg.getSelectedSection());

        //richiesta sezione sbagliata
        if((docSection == null) || (msg.getSelectedSection() >= requestedDoc.getNumOfSections())){
            res.setOpResult(TuringValues.SECTION_UNK);
            return res;
        }

        if(docSection.getLocked().
                getUsername().equals(user.getUsername()) == false){
            res.setOpResult(TuringValues.UNAUTHORIZATED);
            return res;
        }

        /*
            a questo punto siamo sicuri che
            - client user registrato e loggato
            - il documento e la sezione esistono
            - il client user e' autorizzato ad editare la sezione
            - la sezione non e' gia' in editing da un altro utente
        */
        //eseguo l'op e rispondo al client
        res.setOpResult(TuringValues.OP_OK);
        docSection.setSectionContent(msg.getDocumentContent());
        docSection.setLocked(null);
        res.setChatPort(requestedDoc.getChatPortNumber());
        return res;
    }

    //associo la socket svegliata da HelloMsg all'utente nel server
    public void setupInvite(Message msg, SocketChannel inviteSocket) {
        User user = this.turingUsers.get(msg.getUsername());
        user.setInviteSocket(inviteSocket);
    }

    /**
     * @effects condivide un documento con un altro utente
     * @param msg
     * @param clientSocket
     */
    public Message turingShareDocument(Message msg, SocketChannel clientSocket){
        User inviter = this.turingUsers.get(msg.getUsername());
        User invited = this.turingUsers.get(msg.getInvitedEditorUsername());

        Message res = new Message(TuringValues.SHARE);

        if(invited == null){
            res.setOpResult(TuringValues.UNREG_USER);
            return res;
        }

        res.setOpResult(TuringValues.OP_OK);
        Document doc = inviter.getOwnedDocuments().get(msg.getDocumentName());

        if(doc == null){
            res.setOpResult(TuringValues.UNAUTHORIZATED);
            return res;
        }

        invited.getEditableDocuments().put(msg.getDocumentName(), doc);
        doc.getEditors().add(invited);

        if(invited.isOnline()){
            //manda il messaggio al thread in attesa degli inviti
            //che e' sempre in attesa
            Message newInvite = new Message(TuringValues.SHARE);
            newInvite.setDocumentName(doc.getDocumentName());
            this.sendResponse(newInvite, invited.getInviteSocket());
        }else{
            //l'invito verra' visualizzato al momento del login dell'utente
            ArrayList<String> tmp = new ArrayList<>();
            tmp.add(doc.getDocumentName());
            invited.setUnseenInvites(tmp);
        }

        return res;
    }
    /**
     * @effects slogga l'utente
     * @param msg
     * @param clientSocket
     */
    public void turingLogout(Message msg, SocketChannel clientSocket){
        User user = this.turingUsers.get(msg.getUsername());
        if(user.isOnline()){
            user.setOnline(false);
            try {
                user.getInviteSocket().close();
                clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
