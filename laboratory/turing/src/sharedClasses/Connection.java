package sharedClasses;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.net.SocketException;

//connessione fra cliente server
public class Connection {
    /**
     * @effects legge la lunghezza del messaggio che deve essere letto
     * @param socket
     * @return lunghezza del "futuro" messaggio
     * @throws IOException
     */
    public static int readMessageByteLength(SocketChannel socket) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        int n = socket.read(buffer);

        if(n == -1)
            return -1; //socket chiusa

        while(n < 4)
            n += socket.read(buffer);

        buffer.flip();
        int byteLength = buffer.getInt();

        return byteLength;
    }

    /**
     * @effects comunica quanto sara' lungo il prossimo messaggio che sara' inviato
     * @param socket
     * @param byteLength
     * @throws IOException
     */
    public static void sendMessageByteLength(SocketChannel socket, int byteLength) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(byteLength);
        buffer.flip();

        int n = socket.write(buffer);

        if(n == -1)
            throw new SocketException("socket chiusa");

        while(n < 4){
            n += socket.write(buffer);
        }
    }

    /**
     * @effects legge il messaggio (tanti byte quanto anticipatamente detto)
     * @param socket
     * @param msgByteLength
     * @return messaggio letto
     * @throws IOException
     */
    public static Message readMessage(SocketChannel socket, int msgByteLength) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(msgByteLength);

        int n = socket.read(buffer);

        if(n == -1)
            throw new SocketException("socket chiusa");

        while(n < msgByteLength){
           n+= socket.read(buffer);
        }

        String jsonMsg = new String(buffer.array()).trim();
        Message msg = Message.gson.fromJson(jsonMsg, Message.class);

        return msg;
    }

    /**
     * @effects invia il messaggio (invia n byte con n = byte length del msg)
     * @param msg
     * @param socket
     * @param msgByteLength lunghezza del messaggio msg in byte
     * @throws IOException
     */
    public static void sendMessage(Message msg, SocketChannel socket,
            int msgByteLength) throws IOException{

        String jsonMsg = Message.gson.toJson(msg);

        byte[] jsonBuffer = jsonMsg.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(jsonBuffer);

        int n = socket.write(buffer);

        if(n == -1)
            throw new SocketException("socket chiusa");

        while(n < msgByteLength){
            System.out.println("send msg loop");
            n += socket.write(buffer);
        }
    }
}
