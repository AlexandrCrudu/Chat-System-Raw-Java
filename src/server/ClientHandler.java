package server;

import utils.SocketCloser;
import utils.MessageSender;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> handlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private boolean sentPong = false;
    private boolean disconnected = false;
    public static ArrayList<String> surveyCompletedUsers = new ArrayList<>();
    private boolean sentPing = false;

    private AuthenticationHandler authenticationHandler;
    private ServerSurveyHandler serverSurveyHandler;
    private ServerEncryptionHandler serverEncryptionHandler;
    private FileProtocolHandler serverFileTransferHandler;
    private ServerPrivateMessageHandler serverPrivateMessageHandler;



    public void handlePing() {
        Thread pingThread = new Thread(() -> {
            while (!disconnected) {
                try {
                    Thread.sleep(10000);
                    sentPing = true;
                    MessageSender.sendMessage("PING", bufferedWriter);
                    Thread.sleep(3000);
                    if (!sentPong) {
                        MessageSender.sendMessage("FAIL12 Pong was not received!", bufferedWriter);
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        handlers.remove(this);
                        break;
                    } else {
                        sentPong = false;
                        sentPing = false;
                    }
                } catch (IOException | InterruptedException e) {}
            }
        });

        pingThread.start();
    }


    public String getUsername() {
        return clientUsername;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter.write("INIT Welcome to the server. Please log in with your username");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();

            authenticationHandler = new AuthenticationHandler(bufferedWriter, this);
            serverSurveyHandler = new ServerSurveyHandler(this);
            serverEncryptionHandler = new ServerEncryptionHandler(this);
            serverFileTransferHandler = new FileProtocolHandler(this);
            serverPrivateMessageHandler = new ServerPrivateMessageHandler(this);
        }
        catch(IOException E){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void setUsername(String username) {
        this.clientUsername = username;
    }


    @Override
    public void run() {
        String messageFromClient;

        while(!socket.isClosed()) {
            try{
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient == null) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                handleMessFromClient(messageFromClient);

            } catch(IOException e){
                e.printStackTrace();
                break;
            }
        }
        closeEverything(socket, bufferedReader, bufferedWriter);
    }

    public void removeClientHandler() {
        handlers.remove(this);
        MessageSender.broadcastMessage(clientUsername + " has left the chat!", "Chat");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
            removeClientHandler();
            disconnected = true;
            SocketCloser.closeEverything(socket, bufferedReader, bufferedWriter);
            surveyCompletedUsers.remove(getClientUsername());
            try {
                if (getClientUsername().equals(serverSurveyHandler.getSurveyCreator())) {
                    serverSurveyHandler.resetSurveyVariables();
                    serverSurveyHandler.getSurveyThread().interrupt();
                }
            } catch (NullPointerException e) {
            }
    }

    public void handleMessFromClient(String messFromClient) throws IOException {
        if(messFromClient != null) {
            String[] parts = messFromClient.split(" ", 3);
            System.out.println(messFromClient);
            if(messFromClient.equals("IDENT")) {
                MessageSender.sendMessage("FAIL02 Username has an invalid format or length", bufferedWriter);
            }
            if (parts[0].equals("IDENT")) {
                if(parts.length > 1) {
                    authenticationHandler.handleIdent(parts[1]);
                }
                else{
                    MessageSender.sendMessage("FAIL02 Username has an invalid format or length", bufferedWriter);
                }
            } else {
                if(authenticationHandler.isLoggedIn()) {
                    switch (parts[0]) {
                        case "FILETRANSFERREJECT" -> serverFileTransferHandler.handleTransferReject(messFromClient);
                        case "ENCRYPTED" ->  serverEncryptionHandler.handleEncryptedMessage(parts[1], parts[2]);
                        case "SESSIONKEY" -> serverEncryptionHandler.sendSessionKey(parts[1], parts[2]);
                        case "PUBKEY" -> serverEncryptionHandler.sendPubKey(parts[1], parts[2]);
                        case "FILETRANSFERACCEPT" -> serverFileTransferHandler.handleTransferAccept(messFromClient);
                        case "FILETRANSFERREQ" ->  serverFileTransferHandler.handleTransferReq(messFromClient);
                        case "SURVEYREQUEST" -> serverSurveyHandler.handleRequest();
                        case "SUBMITSURVEY" -> serverSurveyHandler.handleSurveySubmission(messFromClient);
                        case "BCST" -> MessageSender.broadcastMessage(messFromClient.split(" ",2)[1], clientUsername);
                        case "PONG" -> handleHeartBeat();
                        case "QUIT" -> closeEverything(socket, bufferedReader, bufferedWriter);
                        case "PRIVATEMESS" -> serverPrivateMessageHandler.handlePrivateMess(messFromClient);
                        case "LIST" -> handleList();
                        case "SURVEY" -> serverSurveyHandler.handleSurvey(messFromClient);
                        case "CHECKSUM" -> handleChecksum(messFromClient);
                        case "CHECKSUMSUCCESS" -> MessageSender.sendMessageToSpecificClient("CHECKSUMSUCCESS " + clientUsername, parts[1]);
                        case "CHECKSUMFAIL" -> MessageSender.sendMessageToSpecificClient("CHECKSUMFAIL " + clientUsername, parts[1]);
                        default -> handleUnknown();
                    }
                } else {
                    MessageSender.sendMessage("FAIL03 Please log in first", bufferedWriter);
                }
            }
        }
    }

    private void handleChecksum(String message) {
        String[] parts = message.split(" ");
        MessageSender.sendMessageToSpecificClient(parts[0] + " " + parts[1] + " " + parts[2] + " " + this.clientUsername, parts[3]);
    }

    private void handleUnknown() {
        try {
            MessageSender.sendMessage("FAIL00 Unknown command", this.bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleList(){
        try {
            StringBuilder list= new StringBuilder();
            for(ClientHandler handler: handlers)
                if(!handler.clientUsername.equals(this.clientUsername))
                    list.append(" ").append(handler.clientUsername);

            MessageSender.sendMessage("OK LIST" + list, this.bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void handleHeartBeat() throws IOException {
        if(sentPing)
            sentPong = true;
        else
            MessageSender.sendMessage("FAIL05 Pong without ping", bufferedWriter);
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (ClientHandler handler:handlers) {
            builder.append(handler.clientUsername).append(",");
        }
        return builder.toString();
    }
}
