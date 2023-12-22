package client;

import utils.MessageSender;
import utils.SocketCloser;
import utils.SurveyBuilder;
import utils.TransferInfo;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client2 {
    private static Client2 client;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private static String username;
    private boolean successfullyLoggedIn = false;
    private boolean disconnected = false;
    private final Scanner scanner = new Scanner(System.in);
    private ClientSurveyHandler surveyHandler;
    private ClientEncryptionHandler encryptionHandler;
    private ClientFileTransferHandler fileTransferHandler;
    private MessageToClientHandler messageToClientHandler;

    public Client2(Socket socket, String username) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            surveyHandler = new ClientSurveyHandler(bufferedWriter, scanner);
            encryptionHandler = new ClientEncryptionHandler(scanner, bufferedWriter);
            fileTransferHandler = new ClientFileTransferHandler(scanner, bufferedWriter);
            messageToClientHandler = new MessageToClientHandler(scanner, bufferedWriter, socket, bufferedReader);
            Client2.username = username;
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void sendMessage() {
        Thread messagingThread = new Thread(() -> {
            while (socket.isConnected() && successfullyLoggedIn) {
                try {
                    printMenu();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        messagingThread.start();
    }


    public void printMenu() throws IOException {
        if (!disconnected) {
            System.out.println("""
                    Enter the number corresponding to the command you wish to execute:
                    1. Send a broadcast message
                    2. Send private message
                    3. See list of active users
                    4. Start survey
                    5. Open survey
                    6. Send File
                    7. View transfer requests
                    8. Send encrypted message
                    9. Exit chat""");

            boolean rightInput = false;
            while(!rightInput) {
                try{
                    String userChoice = scanner.nextLine();
                    handleMenuOptions(Integer.parseInt(userChoice));
                    rightInput = true;
                }
                catch (NumberFormatException e) {
                    System.out.println("Please input a number!");
                }
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleMenuOptions(int userChoice) throws IOException {
        switch (userChoice) {
            case 1 -> messageToClientHandler.sendBroadcastMessage();
            case 2 -> messageToClientHandler.sendPrivateMessage();
            case 3 -> sendListRequest();
            case 4 -> surveyHandler.surveyRequest();
            case 5 -> surveyHandler.handleSurvey();
            case 6 -> fileTransferHandler.sendFile();
            case 7 -> fileTransferHandler.seePendingTransfers();
            case 8 -> encryptionHandler.sendEncryptedMessage();
            case 9 -> exitChat();
            default -> System.out.println("This menu choice is not valid!");
        }
    }

    private void sendListRequest(){
        try {
            MessageSender.sendMessage("LIST", bufferedWriter);
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    private void exitChat() {
        try {
            MessageSender.sendMessage("QUIT", bufferedWriter);
            closeEverything(socket,bufferedReader,bufferedWriter);
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
        System.exit(0);
    }

    private void displayListOfUsers(String receivedMessage) {
        String[] parts = receivedMessage.split(" ",3);
        if(parts.length > 2) {
        String[] users  = parts[2].split(" ");;
            for (String user : users) {
                System.out.println("- " + user);
            }
        } else {
            System.out.println("You're the only one online!");
        }
    }


    public void listenForMessage() {
        Thread listeningThread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    String messageFromChat = bufferedReader.readLine();
                    handleReceivedMessage(messageFromChat);
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    System.out.println("Chat connection lost!");
                    System.exit(0);
                }
            }
        });
        listeningThread.start();
    }


    public void handleReceivedMessage(String receivedMessage) {
        if (receivedMessage != null) {
            String[] parts = receivedMessage.split(" ", 3);
            switch (parts[0]) {
                case "CHECKSUMSUCCESS" -> System.out.println("File successfully downloaded by " + parts[1]);
                case "CHECKSUMFAIL" -> System.out.println("Checksum mismatch with the file of " + parts[1]);
                case "CHECKSUM" -> fileTransferHandler.handleChecksum(receivedMessage);
                case "FILETRANSFERREJECT"-> fileTransferHandler.handleRejectedTransfer(receivedMessage);
                case "ENCRYPTED" -> encryptionHandler.handleReceivedEncryptedMessage(parts[1], parts[2]);
                case "SESSIONKEY" -> encryptionHandler.handleReceivedSessionKey(parts[1], parts[2]);
                case "PUBKEY" -> encryptionHandler.handleReceivedPubKey(parts[1], parts[2]);
                case "FILESUCCESS" -> System.out.println("File with name: " + parts[1] + " has been successfully downloaded");
                case "FILETRANSFERACCEPT" ->
                    new Thread(() -> fileTransferHandler.handleAcceptedRequest(receivedMessage)).start();
                case "FILETRANSFERREQ"-> {
                    System.out.println("A user is trying to send you a file. Check your transfer menu in order to accept or decline.");
                    String[] transferObjectParts = receivedMessage.split(" ");
                    fileTransferHandler.updateTransferObjects(new TransferInfo(transferObjectParts[4], transferObjectParts[3], transferObjectParts[1], transferObjectParts[2]));
                }
                case "SURVEY" -> {
                    System.out.println("A new survey that you are invited to has started. Press 5 to open the survey! ");
                    surveyHandler.updateSurveyContents(SurveyBuilder.handleSurvey(receivedMessage));
                }
                case "JOINED" -> System.out.println(parts[1] + " has joined the chat!");
                case "BCST" -> System.out.println(parts[1] + "(public): " + parts[2]);
                case "INIT" -> {
                    System.out.println(parts[1] + " " + parts[2]);
                    username = scanner.nextLine();
                    sendIdent();
                }
                case "PRIVATEMESS" -> System.out.println(parts[1] + "(private): " + parts[2]);
                case "SURVEYSTATISTICS" -> displayStatistics(receivedMessage);
                case "OK" -> {
                    switch (parts[1]) {
                        case "ENCRYPTED" -> System.out.println("Encrypted message successfully sent to " + parts[2].split(" ")[0]);
                        case "SUBMITSURVEY" -> System.out.println("Your answers for the survey have been successfully submited!");
                        case "SURVEY" -> System.out.println("Survey has been successfully created!");
                        case "FILETRANSFERREQ" -> System.out.println("Successfully sent transfer request!");
                        case "SURVEYREQUEST" -> new Thread(() -> {
                            try {
                                surveyHandler.sendSurvey(parts[2]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        case "IDENT" -> {
                            successfullyLoggedIn = true;
                            System.out.println("You have successfully connected to the chat!");
                            client.sendMessage();
                        }
                        case "BCST" -> System.out.println("Message successfully sent to all chat members!");
                        case "Goodbye" -> System.out.println("You have successfully disconnected from the chat!");
                        case "LIST" -> displayListOfUsers(receivedMessage);
                        case "PRIVATEMESS" -> System.out.println("Sucessfully sent private message to " + parts[2]);
                    }
                }
                case "PING" -> sendPong();
                case "FAIL01" -> {
                    System.out.println("Client with this username is already logged in!");
                    System.out.println("Please log in with another username:");
                    username = scanner.nextLine();
                    sendIdent();
                }
                case "FAIL02" -> {
                    System.out.println("Invalid username!");
                    System.out.println("A username may only consist of characters, numbers, and underscores ('_') and has a length between 3 to 14 characters");
                    username = scanner.nextLine();
                    sendIdent();
                }
                case "FAIL03" -> System.out.println("Please log in first!");
                case "FAIL04" -> System.out.println("You are already logged in, you can't log in twice!");
                case "FAIL05" -> {
                    System.out.println("PONG WITHOUT PING");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
                case "FAIL06" -> System.out.println("Unknown username");
                case "FAIL07" -> System.out.println("Cannot empty empty message!");
                case "FAIL08" -> {
                    System.out.println("Cannot start survey while there is an ongoing survey already!");
                    surveyHandler.releaseThread();
                }
                case "FAIL09" -> {
                    System.out.println(parts[1] + " " + parts[2]);
                    surveyHandler.releaseThread();
                }
                case "FAIL 10" -> System.out.println("Transfer for file " + parts[1] + " to user " + parts[2] + " has failed due to a checksum mismatch!");
                case "FAIL11", "FAIL12" -> System.out.println(parts[1] + " " + parts[2]);
                case "FAIL0" -> System.out.println("Unknown command");
            }
        }
    }

    private void displayStatistics(String receivedMessage) {
        System.out.println("Survey statistics:\n");
        String[] questionAndAnswersSet = receivedMessage.split(" ", 2)[1].split("&");
        for (String set:questionAndAnswersSet) {
            String question = set.split(":")[0];
            String[] answers = set.split(":")[1].split("/");
            System.out.println(question + ":");
            for (String answer: answers) {
                System.out.println(answer + " votes");
            }
            System.out.println();
        }
    }

    private void sendPong() {
        try {
            MessageSender.sendMessage("PONG", bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        disconnected = true;
        SocketCloser.closeEverything(socket, bufferedReader, bufferedWriter);
    }

    public void sendIdent() {
        try {
            MessageSender.sendMessage("IDENT " + username, bufferedWriter);
        } catch (IOException e) {
            closeEverything(socket, bufferedReader,bufferedWriter);
        }
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1337);
        client = new Client2(socket, username);
        client.listenForMessage();
    }
}
