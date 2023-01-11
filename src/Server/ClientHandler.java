package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> handlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;


    //TODO use one thread per client to handle ping pong

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine().split(" ")[1];
            handlers.add(this);
//            while(!handleAlreadyLoggedIn()) {
//                this.clientUsername = bufferedReader.readLine().split(" ")[1];
//            }
            this.bufferedWriter.write("INIT welcome to the server \nPlease log in with your username");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();

//            System.out.println("A new client has connected!");
//            broadcastMessage( "JOINED " + clientUsername);
        }
        catch(IOException E){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public boolean handleAlreadyLoggedIn() {
        int counter = 0;
        for (ClientHandler handler:ClientHandler.handlers) {
            if(this.clientUsername.equals(handler.clientUsername)) {
                counter++;
            }
        }
        if(counter>1) {
            try {
                Socket socketOfReceiver = this.getSocket();
                OutputStream os = socketOfReceiver.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write("FAIL01 User already logged in!");
                bw.newLine();
                bw.flush();
                handlers.remove(this);
                System.out.println("failed logging in");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        String messageFromClient;

        while(socket.isConnected()) {
            try{
                messageFromClient = bufferedReader.readLine();
                handleMessFromClient(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void removeClientHandler() {
        handlers.remove(this);
        broadcastMessage(clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try{
            if(bufferedReader != null) {
                bufferedReader.close();
            }
            if(bufferedWriter != null) {
                bufferedWriter.close();
            }
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleMessFromClient(String messFromClient) {
        String[] parts = messFromClient.split(" ", 2);
        System.out.println(messFromClient);
        switch (parts[0]) {
            case "IDENT" -> handleIdent();
            case "BCST" -> broadcastMessage(messFromClient);
            case "PONG" -> handleHeartBit();
            case "QUIT" -> closeEverything(socket, bufferedReader, bufferedWriter);
            case "PRIVATEMESS" -> handlePrivateMess(messFromClient);
        }
    }

    private void handleIdent() {
        try {
            Socket socketOfReceiver = this.getSocket();
            OutputStream os = socketOfReceiver.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write("IDENT OK");
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler handler: handlers) {
            try{
                if(!handler.clientUsername.equals(clientUsername)) {
                    handler.bufferedWriter.write(messageToSend);
                    handler.bufferedWriter.newLine();
                    handler.bufferedWriter.flush();
                }
            }catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
    }

    public void handleHeartBit() {

    }

    public void handlePrivateMess(String message) {
        String[] messageParts = message.split(" ",3);
        String userNameOfReceiver = messageParts[1];
        String messageToSend = "PRIVATEMESS " + clientUsername + " " + messageParts[2];
        for (ClientHandler handler: handlers) {
            try{
                if(handler.clientUsername.equals(userNameOfReceiver)) {
                    Socket socketOfReceiver = handler.getSocket();
                    OutputStream os = socketOfReceiver.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);
                    bw.write(messageToSend);
                    bw.newLine();
                    bw.flush();
                }
            }catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
    }

    public Socket getSocket() {
        return socket;
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
