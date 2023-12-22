package client;

import utils.SocketCloser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class MessageToClientHandler {
    private final Scanner scanner;
    private final BufferedWriter bufferedWriter;
    private final Socket socket;
    private final BufferedReader bufferedReader;

    public MessageToClientHandler(Scanner scanner, BufferedWriter bufferedWriter, Socket socket, BufferedReader bufferedReader) {
        this.scanner = scanner;
        this.bufferedWriter = bufferedWriter;
        this.bufferedReader = bufferedReader;
        this.socket = socket;
    }

    public void sendPrivateMessage() {
        System.out.println("Type in the exact username of who you want to send a message");
        String username = scanner.nextLine();
        System.out.println("Type in the message you want to send:");
        String messageToChatMember = scanner.nextLine();
        try {
            bufferedWriter.write("PRIVATEMESS " + username + " " + messageToChatMember);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            SocketCloser.closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void sendBroadcastMessage() {
        System.out.print("Type in your message: ");
        String input = scanner.nextLine();
        try {
            bufferedWriter.write("BCST " + input);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            SocketCloser.closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

}
