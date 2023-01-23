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
    private boolean sentPong = false;
    private boolean disconnected = false;



    //TODO use one thread per client to handle ping pong

    public void handlePing() {
        Thread pingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    if(!disconnected) {
                        try {
                            bufferedWriter.write("PING");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (sentPong == false) {
                            try {
                                bufferedWriter.write("FAIL05");
                                bufferedWriter.newLine();
                                bufferedWriter.flush();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            closeEverything(socket, bufferedReader, bufferedWriter);
                            handlers.remove(this);
                        } else {
                            sentPong = false;
                        }
                    }
                    else break;
                }
            }
        });
        pingThread.start();
    }

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            this.clientUsername = bufferedReader.readLine().split(" ")[1];

//            while(!handleAlreadyLoggedIn()) {
//                this.clientUsername = bufferedReader.readLine().split(" ")[1];
//            }
            this.bufferedWriter.write("INIT Welcome to the server. Please log in with your username");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();

//            System.out.println("A new client has connected!");
//            broadcastMessage( "JOINED " + clientUsername);
        }
        catch(IOException E){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public boolean handleAlreadyLoggedIn(String username) {
        int counter = 0;
        for (ClientHandler handler:ClientHandler.handlers) {
            if(username.equals(handler.clientUsername)) {
                counter++;
            }
        }
        if(counter>0) {
            try {
                Socket socketOfReceiver = this.getSocket();
                OutputStream os = socketOfReceiver.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write("FAIL01 User already logged in!");
                bw.newLine();
                bw.flush();
//                handlers.remove(this);
                System.out.println("failed logging in");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean isLoggedIn() throws IOException {
        if(this.clientUsername == null)
            return false;
        return true;
    }

    public void write(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    @Override
    public void run() {
        String messageFromClient;

        while(socket.isConnected()) {
            try{
                messageFromClient = bufferedReader.readLine();
                handleMessFromClient(messageFromClient);
            } catch (IOException e) {
                if(!disconnected)
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
        disconnected = true;
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



    public void handleMessFromClient(String messFromClient) throws IOException {
        if(messFromClient != null) {
            String[] parts = messFromClient.split(" ", 3);
            System.out.println(messFromClient);
//            switch (parts[0]) {
//                case "IDENT":
//                    handleIdent(parts[1]);
//                    break;
//                case "BCST":
//                    broadcastMessage(parts[2]);
//                    break;
//                case "PONG":
//                    handleHeartBit();
//                    break;
//                case "QUIT":
//                    closeEverything(socket, bufferedReader, bufferedWriter);
//                    break;
//                case "PRIVATEMESS":
//                    handlePrivateMess(messFromClient);
//                    break;
//                case "LIST":
//                    handleList();
//                    break;
//                default:
//                    handleUnknown();
//                    break;
//            }
            if (parts[0].equals("IDENT")) {
                handleIdent(parts[1]);
            } else {
                if(isLoggedIn()) {
                    switch (parts[0]) {
                        case "BCST" -> broadcastMessage(parts[2]);
                        case "PONG" -> handleHeartBit();
                        case "QUIT" -> closeEverything(socket, bufferedReader, bufferedWriter);
                        case "PRIVATEMESS" -> handlePrivateMess(messFromClient);
                        case "LIST" -> handleList();
                        default -> handleUnknown();
                    }
                } else {
                    write("FAIL03 Please log in first");
                }
            }
        }
    }

    private void handleList(){
        try {
            String list = "-";
            for(ClientHandler handler: handlers)
                if(!handler.clientUsername.equals(this.clientUsername))
                    list = list + " " + handler.clientUsername;

            this.bufferedWriter.write("OK LIST " + list);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleIdent(String username) {
        try {
            if(isLoggedIn())
                write("FAIL04 User cannot login twice");
            else if(!handleAlreadyLoggedIn(username) && checkUsername(username)) {
                this.clientUsername = username;
                handlers.add(this);
                broadcastMessage("Hello I have connected to the chat!");
                bufferedWriter.write("OK IDENT");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                handlePing();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkUsername(String username){
        boolean matches = username.matches("^[a-zA-Z0-9]+([_]?[a-zA-Z0-9]+){3,14}");
        if(!(matches && username.length() <= 13)){
            try {
                this.bufferedWriter.write("FAIL02 Username has an invalid format or length");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return matches && username.length() <= 13;
    }

    public boolean checkLoginTwice() {
        return false;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler handler: handlers) {
            try{
                if(!handler.clientUsername.equals(clientUsername)) {
                    handler.bufferedWriter.write("BCST " + clientUsername + " " + messageToSend);
                    handler.bufferedWriter.newLine();
                    handler.bufferedWriter.flush();
                }
                else{
                    handler.bufferedWriter.write("OK BCST " + messageToSend);
                    handler.bufferedWriter.newLine();
                    handler.bufferedWriter.flush();
                }
            }catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
    }

    public void handleHeartBit() {
        sentPong = true;
    }
//    public boolean validUser(){
//        if(handlers);
//    }
    public void handlePrivateMess(String message) throws IOException {
        String[] messageParts = message.split(" ",3);
        if(messageParts[2]!=null && !messageParts[2].equals("")) {
            String userNameOfReceiver = messageParts[1];
            String messageToSend = "PRIVATEMESS " + clientUsername + " " + messageParts[2];
            boolean foundUser = false;
            for (ClientHandler handler : handlers) {
                try {
                    if (handler.clientUsername.equals(userNameOfReceiver)) {
                        foundUser = true;
                        Socket socketOfReceiver = handler.getSocket();
                        OutputStream os = socketOfReceiver.getOutputStream();
                        OutputStreamWriter osw = new OutputStreamWriter(os);
                        BufferedWriter bw = new BufferedWriter(osw);
                        bw.write(messageToSend);
                        bw.newLine();
                        bw.flush();

                        this.bufferedWriter.write("OK PRIVATEMESS " + messageParts[1] + " " + messageParts[2]);
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
            if (!foundUser)
                write("FAIL06 Unknown username");
        }
        else{
            write("FAIL07 Cannot send empty message");
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
