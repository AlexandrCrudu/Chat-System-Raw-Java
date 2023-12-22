package client;

import utils.MenuInput;
import utils.MessageSender;
import utils.TransferInfo;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class ClientFileTransferHandler {
    private final Scanner scanner;
    private String filePath;
    private final BufferedWriter bufferedWriter;
    private final ArrayList<TransferInfo> transferObjects;
    private final HashMap<String, String> checksumByUUID = new HashMap<>();
    private final HashMap<String, String> userByUUID = new HashMap<>();
    public ClientFileTransferHandler(Scanner scanner, BufferedWriter bufferedWriter) {
        this.scanner = scanner;
        this.bufferedWriter = bufferedWriter;
        transferObjects = new ArrayList<>();
    }

    public void sendFile() throws IOException {
        System.out.println("Chose the number corresponding to the file you want to transfer: ");
        System.out.println("""
                1. transfer-demo.txt\s
                2. doc-demo.docx\s
                3. doc-demo.pdf\s
              """
        );

        int choice = MenuInput.menuChoice(3, scanner);
        switch (choice) {
            case 1 -> filePath = "resources/transfer-demo.txt";
            case 2 -> filePath = "resources/doc-demo.docx";
            case 3 -> filePath = "resources/doc-demo.pdf";
        }
        File file = new File(filePath);
        long fileSize = file.length();
        String fileName = file.getName();
        System.out.println("Please insert the name of the user you'd like to send a file!");
        String receiver = scanner.nextLine();
        String uuid = UUID.randomUUID().toString();
        MessageSender.sendMessage("FILETRANSFERREQ " + fileName + " " + fileSize + " " + receiver + " " + uuid, bufferedWriter);
    }

    public void handleRejectedTransfer(String receivedMessage) {
        String[] parts = receivedMessage.split(" ");
        System.out.println("File transfer request for the file: " + parts[1] + " with uuid: " + parts[3] + " to user " + parts[2] + " has been REJECTED!");
    }

    public void handleAcceptedRequest(String receivedMessage) {
        String[] parts = receivedMessage.split(" ");
        System.out.println("File transfer request for the file: " + parts[1] + " with uuid: " + parts[3] + " to user " + parts[2] + " has been ACCEPTED");
        try {
            File file = new File("resources/" + parts[1]);
            FileInputStream fileInputStream = new FileInputStream(file);
            Socket socket = new Socket("localhost", 1338);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            String serverMessage = "s " + parts[3];
            byte[] messageBytes = serverMessage.getBytes();
            dataOutputStream.writeInt(messageBytes.length);
            dataOutputStream.write(messageBytes);

            String fileName = parts[1];
            byte[] fileNameBytes = fileName.getBytes();
            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);

            byte[] fileBytes = new byte[1024];

            int bytesRead = 0;
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while((bytesRead = fileInputStream.read(fileBytes)) != -1){
                messageDigest.update(fileBytes, 0 , bytesRead);
                dataOutputStream.write(fileBytes, 0, bytesRead);
            }
            socket.close();

            byte[] checksumBytes = messageDigest.digest();
            String checksum = new BigInteger(1, checksumBytes).toString(16);
            String checksumProtocolMessage = "CHECKSUM " + checksum + " " + parts[3] + " " +parts[2];

            MessageSender.sendMessage(checksumProtocolMessage, bufferedWriter);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    public void listenForTransferMessages(Socket socket) {
            new Thread(() -> {
                try {
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    while (socket.isConnected()) {
                        int nameLength = dataInputStream.readInt();
                        byte[] nameBytes = new byte[nameLength];
                        dataInputStream.readFully(nameBytes, 0, nameLength);
                        String fileNameString = new String(nameBytes);

                        int uuidLength = dataInputStream.readInt();
                        byte[] uuidBytes = new byte[uuidLength];
                        dataInputStream.readFully(uuidBytes, 0, uuidLength);
                        String uuidString = new String(uuidBytes);

                        FileOutputStream fileOutputStream = new FileOutputStream("downloads/" + fileNameString);

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        MessageDigest messageDigest = MessageDigest.getInstance("MD5");

                        while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            messageDigest.update(buffer, 0 , bytesRead);
                            if (bytesRead < buffer.length) {
                                break;
                            }
                        }
                        System.out.println("File Downloaded!");

                        byte[] checksumBytes = messageDigest.digest();
                        String checksum = new BigInteger(1, checksumBytes).toString(16);
                        while(true){
                            if(checksumByUUID.get(uuidString) != null)
                                break;
                        }
                        if(checksumByUUID.get(uuidString).equals(checksum)) {
                            bufferedWriter.write("CHECKSUMSUCCESS " + userByUUID.get(uuidString));
                            System.out.println("Checksum matched, file validated!");
                        }
                        else{
                            bufferedWriter.write("CHECKSUMFAIL " + userByUUID.get(uuidString));
                            System.out.println("Checksum did not match, something went wrong with the transfer!");
                        }
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        fileOutputStream.close();
                    }

                    dataInputStream.close();
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }).start();
        }


    public void seePendingTransfers() throws IOException {
        if(transferObjects.size() != 0) {
            System.out.println("Available transfer requests");
            for (int i = 0; i < transferObjects.size(); i++) {
                System.out.println((i + 1) + ". " + transferObjects.get(i));
            }
            System.out.println("Choose a number associated with the transfer request in order to handle it.");
            int requestChoice = MenuInput.menuChoice(transferObjects.size(), scanner);
            System.out.println("1. Yes");
            System.out.println("2. No");
            int acceptChoice = MenuInput.menuChoice(2, scanner);
            if (acceptChoice == 2) {
                MessageSender.sendMessage("FILETRANSFERREJECT " + transferObjects.get(requestChoice - 1).getFileName() + " " + transferObjects.get(requestChoice - 1).getSender() + " " + transferObjects.get(requestChoice - 1).getUuid(), bufferedWriter);
                transferObjects.remove(requestChoice - 1);
            } else {
                MessageSender.sendMessage("FILETRANSFERACCEPT " + transferObjects.get(requestChoice - 1).getFileName() + " " + transferObjects.get(requestChoice - 1).getSender() + " " + transferObjects.get(requestChoice - 1).getUuid(), bufferedWriter);
                handleFileTransfer(transferObjects.get(requestChoice - 1).getUuid());
                System.out.println("Started to handle task: " + transferObjects.get(requestChoice - 1));
            }
        }
        else
            System.out.println("No current available transfer requests.");
    }

    private void handleFileTransfer(String uuid){
        try {
            Socket socket = new Socket("localhost", 1338);
            listenForTransferMessages(socket);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            String messageToBeSent = "r " + uuid;
            byte[] messageBytes = messageToBeSent.getBytes();

            dataOutputStream.writeInt(messageBytes.length);
            dataOutputStream.write(messageBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTransferObjects(TransferInfo transferObject) {
        transferObjects.add(transferObject);
    }

    public void handleChecksum(String receivedMessage) {
        String[] parts = receivedMessage.split( " ");
        checksumByUUID.put(parts[2], parts[1]);
        userByUUID.put(parts[2], parts[3]);
    }
}
