package server;

import utils.MessageSender;

import java.io.BufferedWriter;
import java.io.IOException;

public class ServerPrivateMessageHandler {
    private final ClientHandler clientHandler;

    public ServerPrivateMessageHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handlePrivateMess(String message) throws IOException {
        String[] messageParts = message.split(" ",3);
        if(messageParts[2]!=null && !messageParts[2].equals("")) {
            String userNameOfReceiver = messageParts[1];
            String messageToSend = "PRIVATEMESS " + clientHandler.getUsername() + " " + messageParts[2];
            boolean foundUser = false;
            for (ClientHandler handler : ClientHandler.handlers) {
                try {
                    if (handler.getUsername().equals(userNameOfReceiver)) {
                        foundUser = true;
                        BufferedWriter bw = handler.getBufferedWriter();

                        MessageSender.sendMessage(messageToSend,bw);
                        MessageSender.sendMessage("OK PRIVATEMESS " + messageParts[1] + " " + messageParts[2], clientHandler.getBufferedWriter());
                    }
                } catch (IOException e) {
                    clientHandler.closeEverything(clientHandler.getSocket(), clientHandler.getBufferedReader(), clientHandler.getBufferedWriter());
                }
            }
            if (!foundUser)
                MessageSender.sendMessage("FAIL06 Unknown username", clientHandler.getBufferedWriter());
        }
        else{
            MessageSender.sendMessage("FAIL07 Cannot send empty message", clientHandler.getBufferedWriter());
        }
    }
}
