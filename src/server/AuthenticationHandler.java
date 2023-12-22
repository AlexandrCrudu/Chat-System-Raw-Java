package server;

import utils.MessageSender;

import java.io.*;


public class AuthenticationHandler {
    private final BufferedWriter bufferedWriter;
    private final ClientHandler clientHandler;

    public AuthenticationHandler(BufferedWriter bufferedWriter, ClientHandler clientHandler) {
        this.bufferedWriter = bufferedWriter;
        this.clientHandler = clientHandler;
    }

    public boolean handleAlreadyLoggedIn(String username) {
        int counter = 0;
        for (ClientHandler handler:ClientHandler.handlers) {
            if(username.equals(handler.getClientUsername())) {
                counter++;
            }
        }
        if(counter>0) {
            try {
                MessageSender.sendMessage("FAIL01 User already logged in",bufferedWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean isLoggedIn() throws IOException {
        return clientHandler.getClientUsername() != null;
    }

    public void handleIdent(String username) {
        try {
            if(isLoggedIn())
                MessageSender.sendMessage("FAIL04 User cannot login twice", bufferedWriter);
            else if(!handleAlreadyLoggedIn(username) && checkUsername(username)) {
                clientHandler.setUsername(username);
                ClientHandler.handlers.add(clientHandler);
                MessageSender.sendMessage("OK IDENT " + username, bufferedWriter);
                MessageSender.sendMessagesToEveryone("JOINED " + username, username);
                clientHandler.handlePing();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkUsername(String username){
        boolean matches = username.matches("^[a-zA-Z0-9]+([_]?[a-zA-Z0-9]+){2,15}");
        if(!(matches && username.length() <= 14)){
            try {
                MessageSender.sendMessage("FAIL02 Username has an invalid format or length", this.bufferedWriter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return matches && username.length() <= 14;
    }
}
