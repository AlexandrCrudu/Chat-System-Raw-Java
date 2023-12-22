package server;

import utils.MessageSender;
import utils.UsernameFinder;

import java.io.IOException;

public class ServerEncryptionHandler {
    private final ClientHandler clientHandler;

    public ServerEncryptionHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handleEncryptedMessage(String username, String encryptedMessage) throws IOException {
        if(UsernameFinder.foundUsername(username)) {
            MessageSender.sendMessage("OK ENCRYPTED " + username + " " + encryptedMessage, clientHandler.getBufferedWriter());
            MessageSender.sendMessageToSpecificClient("ENCRYPTED " + clientHandler.getUsername() + " " + encryptedMessage, username);
        } else {
            MessageSender.sendMessage("FAIL06 Unknown username", clientHandler.getBufferedWriter());
        }
    }

    public void sendSessionKey(String sender, String sessionKey) {
        MessageSender.sendMessageToSpecificClient("SESSIONKEY " + clientHandler.getUsername() + " " + sessionKey, sender);
    }

    public void sendPubKey(String receiver, String pubKey) throws IOException {
        if(UsernameFinder.foundUsername(receiver)) {
            MessageSender.sendMessageToSpecificClient("PUBKEY " + clientHandler.getUsername() + " " + pubKey, receiver);
        } else {
            MessageSender.sendMessage("FAIL06 Unknown username", clientHandler.getBufferedWriter());
        }
    }
}
