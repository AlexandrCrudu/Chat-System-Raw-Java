package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client2 {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private static String username;
    private boolean successfullyLoggedIn = false;
    Scanner scanner = new Scanner(System.in);

    public Client2(Socket socket, String username) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            sendIdent();
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void sendMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected() && !successfullyLoggedIn) {
                    printMenu();
                }
            }
        }).start();

    }

    public void printMenu() {
        System.out.println("""
                Enter the number corresponding to the command you wish to execute:
                1. Send a broadcast message
                2. Send private message
                3. Exit chat""");
        String userChoice = scanner.nextLine();
        handleMenuOptions(Integer.parseInt(userChoice));
    }

    private void handleMenuOptions(int userChoice) {
        switch (userChoice) {
            case 1 -> sendBroadcastMessage();
            case 2 -> sendPrivateMessage();
            case 3 -> exitChat();
        }
    }

    private void sendPrivateMessage() {
        System.out.println("Type in the exact username of who you want to send a message");
        String username = scanner.nextLine();
        System.out.println("Type in the message you want to send:");
        String messageToChatMember = scanner.nextLine();
        try {
            bufferedWriter.write("PRIVATEMESS " + username + " " + messageToChatMember);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    private void exitChat() {
        try {
            bufferedWriter.write("QUIT");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    private void sendBroadcastMessage() {
        System.out.print("Type in your message: ");
        String input = scanner.nextLine();
        try {
            bufferedWriter.write("BCST " + username + ": " + input);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
               while(socket.isConnected()) {
                   try{
                       String messageFromChat = bufferedReader.readLine();
                       handleReceivedMessage(messageFromChat);
                   } catch(IOException e) {
                       closeEverything(socket,bufferedReader,bufferedWriter);
                   }
               }
            }
        }).start();
    }

    public void handleReceivedMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(" ", 3);
        switch (parts[0]) {
            case "JOINED":
                System.out.println(parts[1] + " has joined the chat!");
                break;
            case "BCST" :
                System.out.println(parts[1] + "(public): " + parts[2]);
                break;
            case "INIT":
                System.out.println(parts[1]);
                System.out.println("Log in with your username: ");
                username = scanner.nextLine();
                break;
            case "PRIVATEMESS":
                System.out.println(parts[1] + "(private): " + parts[2]);
                break;
            case "OK" :
                switch (parts[1]) {
                    case "IDENT" -> {
                        successfullyLoggedIn = true;
                        System.out.println("You have successfully connected to the chat!");
                    }
                    case "BCST" -> System.out.println("Message successfully sent to all chat members!");
                    case "Goodbye" -> System.out.println("You have successfully disconnected from the chat!");
                }
                break;
            case "PING" :
                sendPong();
                break;
            case "FAIL01":
                while(!successfullyLoggedIn) {
                    System.out.println("Client with this username is already logged in!");
                    System.out.println("Please log in with another username:");
                    username = scanner.nextLine();
                    sendIdent();
                }
                break;
        }
    }

    private void sendPong() {

    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try{
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendIdent() {
        try {
            bufferedWriter.write("IDENT " + username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader,bufferedWriter);
        }
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1338);
        Client2 client = new Client2(socket, username);
        client.listenForMessage();
        client.sendMessage();
    }
}
