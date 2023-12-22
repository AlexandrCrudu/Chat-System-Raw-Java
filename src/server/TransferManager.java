package server;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class TransferManager implements Runnable{
    private final Socket socket;
    private static final HashMap<String, Socket> receiverSockets = new HashMap<>();

    public TransferManager(Socket socket) {
        this.socket = socket;
    }

    synchronized void addReceiverSocket(String uuid, Socket socket){
        receiverSockets.put(uuid, socket);
    }

    @Override
    public void run() {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int messageLength = dataInputStream.readInt();
            if (messageLength > 0) {
                byte[] bytes = new byte[messageLength];
                dataInputStream.readFully(bytes, 0, bytes.length);
                String message = new String(bytes);
                String[] parts = message.split(" ");
                if (message.charAt(0) == 'r') {
                    addReceiverSocket(parts[1], socket);
                } else {
                    int fileNameBytesLength = dataInputStream.readInt();
                    byte[] fileNameBytes = new byte[fileNameBytesLength];
                    dataInputStream.readFully(fileNameBytes, 0, fileNameBytesLength);
                    byte[] fileUUIDBytes = parts[1].getBytes();

                    Socket wantedReceiverSocket = receiverSockets.get(parts[1]);
                    DataOutputStream dataOutputStream = new DataOutputStream(wantedReceiverSocket.getOutputStream());

                    dataOutputStream.writeInt(fileNameBytes.length);
                    dataOutputStream.write(fileNameBytes);

                    dataOutputStream.writeInt(fileUUIDBytes.length);
                    dataOutputStream.write(fileUUIDBytes);
                    dataInputStream.transferTo(dataOutputStream);
                }
            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }
}
