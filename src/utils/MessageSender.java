package utils;

import server.ClientHandler;

import java.io.*;

public class MessageSender {
    public static void sendMessage(String message, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public static void sendMessagesToEveryone(String message, String username) {
        for (ClientHandler clientHandler:ClientHandler.handlers) {
            if(!clientHandler.getClientUsername().equals(username)) {
                sendMessageToSpecificClient(message,clientHandler.getClientUsername());
            }
        }
    }

    public static void sendMessageToAllChatMembers(String message) {
        for (ClientHandler clientHandler:ClientHandler.handlers) {
            sendMessageToSpecificClient(message,clientHandler.getClientUsername());
        }
    }

    public static void sendMessageToSpecificClient(String message, String username) {
        for (ClientHandler handler:ClientHandler.handlers) {
            if(handler.getClientUsername().equals(username)) {

                BufferedWriter bw = handler.getBufferedWriter();

                try {
                    sendMessage(message, bw);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void broadcastMessage(String messageToSend, String sender) {
        System.out.println(messageToSend);
        for (ClientHandler handler: ClientHandler.handlers) {
            try{
                if(!handler.getUsername().equals(sender)) {
                    sendMessage("BCST " + sender + " " + messageToSend, handler.getBufferedWriter());
                }
                else{
                    sendMessage("OK BCST " + messageToSend, handler.getBufferedWriter());
                }
            }catch (IOException e) {
                System.out.println("Error sending broadcast message");
            }
        }
    }

}
