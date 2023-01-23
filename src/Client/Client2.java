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
    private boolean disconnected = false;
    Scanner scanner = new Scanner(System.in);

    public Client2(Socket socket, String username) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void sendMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected() && successfullyLoggedIn) {
                        printMenu();
                }
            }
        }).start();

    }

    public void printMenu() {
        if (!disconnected) {
            System.out.println("""
                    Enter the number corresponding to the command you wish to execute:
                    1. Send a broadcast message
                    2. Send private message
                    3. See list of active users
                    4. Start survey
                    5. Exit chat""");


            String userChoice = scanner.nextLine();
            handleMenuOptions(Integer.parseInt(userChoice));
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleMenuOptions(int userChoice) {
        switch (userChoice) {
            case 1 -> sendBroadcastMessage();
            case 2 -> sendPrivateMessage();
            case 3 -> sendListRequest();
            case 4 -> sendSurvey();
            case 5 -> exitChat();
        }
    }

    public void sendSurvey() {
        HashMap<String, String[]> survey = buildSurvey();
        ArrayList<String> users = chooseUsers();
        StringBuilder builder = new StringBuilder();
        builder.append("SURVEY ");
        for (String user:users) {
            builder.append(user).append("-");
        }
        builder.deleteCharAt(builder.length()-1);
        builder.append("||");
        for (Map.Entry<String, String[]> set: survey.entrySet()) {
            builder.append(set.getKey()).append(":");
            for (String string:set.getValue()) {
                builder.append(string).append("-");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append("||");
        }

        builder.deleteCharAt(builder.length()-1);
        builder.deleteCharAt(builder.length()-1);
        String finalString = builder.toString();
        System.out.println(finalString);
    }

    private ArrayList<String> chooseUsers(){
        ArrayList<String> users = new ArrayList<>();
        System.out.println("Please add users:");
        String choice1 = scanner.nextLine();

        while(choice1.isEmpty()){
            System.out.println("The survey must be sent to at least 2 more users!");
            System.out.println("Please try again");
            choice1 = scanner.nextLine();
        }
        users.add(choice1);
        System.out.println("Please add users:");
        choice1 = scanner.nextLine();

        while(choice1.isEmpty()){
            System.out.println("The survey must be sent to at least one more user!");
            System.out.println("Please try again");
            choice1 = scanner.nextLine();
        }
        users.add(choice1);
        System.out.println("Please add users or send a blank message when you want to stop choosing users:");

        choice1 = scanner.nextLine();
        while(!choice1.isEmpty()){
            users.add(choice1);
            System.out.println("Please add users or send a blank message when you want to stop choosing users:");
            choice1 = scanner.nextLine();
        }
        System.out.println(users);
        return users;
    }

    private HashMap<String, String[]> buildSurvey() {
        HashMap<String, String[]> survey = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            System.out.println("Please add the question with number " + i + " to the survey");
            if(i > 1) {
                System.out.println("Send an empty message when you're done adding messages to the survey");
            }
            String question = scanner.nextLine();
            if(i == 1)
                while(question.isEmpty()){
                    System.out.println("You cannot submit a survey without questions, try again:");
                    question = scanner.nextLine();
                }

            if (question.isEmpty()) {
                break;
            }
            else {
                String[] answers = new String[4];
                System.out.println("Now you need to add a minimum of 2 answers, but you can add up to 4 answers.");
                for (int j = 1; j < 5 ; j++) {
                    System.out.println("Add the answer with number " + j + ": ");
                    if(j>2) {
                        System.out.println("Send empty to finish!");
                    }
                    String answer = scanner.nextLine();
                    if(j<=2 && answer.isEmpty()) {
                        while(answer.isEmpty()) {
                            System.out.println("Answer cannot be empty, try again:");
                            answer = scanner.nextLine();
                        }
                    } else if (j>2 && answer.isEmpty()) {
                        break;
                    }
                    answers[j-1] = answer;
                }
                survey.put(question,answers);
            }
        }
        for (String question: survey.keySet()) {
            System.out.println(question);
            for(String answer: survey.get(question))
                System.out.println(answer);
        }
        return survey;
    }

    private void sendListRequest(){
        try {
            bufferedWriter.write("LIST");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
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
            closeEverything(socket,bufferedReader,bufferedWriter);
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    private void sendBroadcastMessage() {
        System.out.print("Type in your message: ");
        String input = scanner.nextLine();
        try {
            bufferedWriter.write("BCST " + username + " " + input);
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
//                       closeEverything(socket,bufferedReader,bufferedWriter);
                   }
               }
            }
        }).start();
    }

    public void handleReceivedMessage(String receivedMessage) {
//        System.out.println(receivedMessage);
        if (receivedMessage != null) {
            String[] parts = receivedMessage.split(" ", 4);

            switch (parts[0]) {

                case "JOINED":
                    System.out.println(parts[1] + " has joined the chat!");
                    break;
                case "BCST":
                    System.out.println(parts[1] + "(public): " + parts[2] + " " + parts[3]);
                    break;
                case "INIT":
                    System.out.println(parts[1] + " " + parts[2] + " " + parts[3]);
                    //System.out.println("Log in with your username: ");
                    username = scanner.nextLine();
                    sendIdent();
                    break;
                case "PRIVATEMESS":
                    System.out.println(parts[1] + "(private): " + parts[2] + " " + parts[3]);
                    break;
                case "OK":
                    switch (parts[1]) {
                        case "IDENT" -> {
                            successfullyLoggedIn = true;
                            System.out.println("You have successfully connected to the chat!");
                            client.sendMessage();
                            break;
                        }
                        case "BCST" -> {
                            System.out.println("Message successfully sent to all chat members!");
                            break;
                        }
                        case "Goodbye" -> {
                            System.out.println("You have successfully disconnected from the chat!");
                            break;
                        }
                        case "LIST" -> {
                            System.out.println("List of users:\n" + parts[2] + " " + parts[3]);
                            break;
                        }
                        case "PRIVATEMESS" -> {
                            System.out.println("Sucessfully sent private message to " + parts[2]);
                        }
                    }
                    break;
                case "PING":
                    sendPong();
                    break;
                case "FAIL01":
                    System.out.println("Client with this username is already logged in!");
                    System.out.println("Please log in with another username:");
                    username = scanner.nextLine();
                    sendIdent();
                    break;
                case "FAIL02":
                    System.out.println("Invalid username!");
                    System.out.println("A username may only consist of characters, numbers, and underscores ('_') and has a length between 3 to 14 characters");
                    username = scanner.nextLine();
                    sendIdent();
                    break;
                case "FAIL03":
                    System.out.println("Please log in first!");
                    break;
                case "FAIL04":
                    System.out.println("You are already logged in, you can't log in twice!");
                    break;
                case "FAIL05":
                    System.out.println("Connection lost. Disconnected...");
                    closeEverything(socket, bufferedReader,bufferedWriter);
                    break;
                case "FAIL06":
                    System.out.println("Unknown username");
                    break;
                case "FAIL07":
                    System.out.println("Cannot empty empty message!");
                    break;
                case "FAIL00":
                    System.out.println("Unknown command");
                    break;
            }
        }
    }

    private void sendPong() {
        try {
            bufferedWriter.write("PONG");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        disconnected = true;
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
    private static Client2 client;
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1338);
        client = new Client2(socket, username);
        client.listenForMessage();
    }
}
