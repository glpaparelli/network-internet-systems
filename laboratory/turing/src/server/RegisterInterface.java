package server;
import java.rmi.Remote;
import java.rmi.RemoteException;
import sharedClasses.Message;
import sharedClasses.TuringValues;

public interface RegisterInterface extends Remote {
    /**
     * @effects registra utente 
     * @param msg
     * @return esito operazione
     * @throws RemoteException
     */
    public TuringValues turingRegister(Message msg) throws RemoteException;
}
