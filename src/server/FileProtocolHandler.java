package server;

import utils.MessageSender;
import utils.UsernameFinder;

import java.io.IOException;
import java.util.HashMap;

public class FileProtocolHandler {

    private final ClientHandler clientHandler;
    private static final HashMap<String, String> sendersByUUID = new HashMap<>();

    public FileProtocolHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handleTransferAccept(String messFromClient) {
        String[] parts = messFromClient.split(" ");
        String fileName = parts[1];
        String sender = parts[2];
        String uuid = parts[3];

        sendersByUUID.put(uuid, sender);
        MessageSender.sendMessageToSpecificClient("FILETRANSFERACCEPT " + fileName + " " + clientHandler.getUsername() + " " + uuid, sender);
    }

    public void handleTransferReject(String messFromClient){
        String[] parts = messFromClient.split(" ");
        String fileName = parts[1];
        String sender = parts[2];
        String uuid = parts[3];

        MessageSender.sendMessageToSpecificClient("FILETRANSFERREJECT " + fileName + " " + clientHandler.getUsername() + " " + uuid, sender);
    }

    public void handleTransferReq(String messFromClient) throws IOException {
        String[] parts = messFromClient.split(" ");
        String fileName = parts[1];
        String fileSize = parts[2];
        String receiver = parts[3];
        String uuid = parts[4];

        if(UsernameFinder.foundUsername(receiver)) {
            MessageSender.sendMessage("OK " + messFromClient, clientHandler.getBufferedWriter());
            MessageSender.sendMessageToSpecificClient("FILETRANSFERREQ " + fileName + " " + fileSize + " " + clientHandler.getUsername() + " " + uuid,receiver);
        } else {
            MessageSender.sendMessage("FAIL06 Unknown username", clientHandler.getBufferedWriter());
        }
    }
}
