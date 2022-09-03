package client;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import sharedClasses.Connection;
import sharedClasses.Message;
import sharedClasses.TuringValues;

//gestore degli inviti ad editare
public class TuringInviteHandler implements Runnable {

    private SocketChannel socket;
    private boolean keepRun = false;

    /**
     * @effects Costruttore 
     * @param socket socket di comunicazione con il server
     */
    public TuringInviteHandler(SocketChannel socket) {
        this.socket = socket;
        this.keepRun = true;
    }
    
    /**
     * @effects si mette in attesa infinita per inviti di editing
    */
    @Override
    public void run() {
        while(keepRun){
            try {
                int inviteByteLength = Connection.readMessageByteLength(this.socket);
                Message invite = Connection.readMessage(this.socket, inviteByteLength);
                    System.out.println("** "
                        + "sei stato invitato a collaborare al documento: " 
                        + invite.getDocumentName() + " **"
                    );
                    System.out.print("> ");
                
            } catch (Exception ex) {
                this.keepRun = false;
                //ignoro eccezione Interrupted
                //ex.printStackTrace();
            }
        }
    }
   
}
