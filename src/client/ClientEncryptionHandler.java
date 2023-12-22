package client;

import utils.MessageSender;

import javax.crypto.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;

public class ClientEncryptionHandler {
    private final Scanner scanner;
    private final HashMap<String, SecretKey> sessionKeys = new HashMap<>();
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final BufferedWriter bufferedWriter;
    private final HashMap<String, String> firstMessage = new HashMap<>();

    public ClientEncryptionHandler(Scanner scanner, BufferedWriter bufferedWriter) {
        this.scanner = scanner;
        this.bufferedWriter = bufferedWriter;

        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();
    }

    public void sendEncryptedMessage() throws IOException {
        System.out.println("Enter the exact name of the user you want to send the encrypted message to: ");
        String userName = scanner.nextLine();

        System.out.println("Enter the exact message you want to send: ");
        String messageToBeEncrypted = scanner.nextLine();

        if(!sessionKeys.containsKey(userName)) {
            byte[] encodedPublicKey = publicKey.getEncoded();
            String encodedPublicKeyString = Base64.getEncoder().encodeToString(encodedPublicKey);
            MessageSender.sendMessage("PUBKEY " + userName + " " + encodedPublicKeyString, bufferedWriter);
            firstMessage.put(userName, messageToBeEncrypted);
        }
        else {
            sendEncryptedMessageToUser(userName, messageToBeEncrypted);
        }
    }

    public void sendEncryptedMessageToUser(String userName, String messageToBeEncrypted){
        Cipher cipher;
        byte[] encryptedMessage;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sessionKeys.get(userName));
            encryptedMessage = cipher.doFinal(messageToBeEncrypted.getBytes());
            String base64Message = Base64.getEncoder().encodeToString(encryptedMessage);
            MessageSender.sendMessage("ENCRYPTED " + userName + " " + base64Message, bufferedWriter);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException e) {
            System.out.println("Cannot sent message to person who does not exist!");
        }
    }

    public void handleReceivedEncryptedMessage(String sender, String message) {
        Cipher cipher;
        try {
            byte[] decodedSessionKey = Base64.getDecoder().decode(message);
            cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, sessionKeys.get(sender));
            byte[] decryptedMessage = cipher.doFinal(decodedSessionKey);
            System.out.println(sender + "(encrypted message): " + new String(decryptedMessage));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public void handleReceivedSessionKey(String sender, String receivedSessionKey) {
        byte[] decodedSessionKey = Base64.getDecoder().decode(receivedSessionKey);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            SecretKey sessionKey = (SecretKey) cipher.unwrap(decodedSessionKey, "AES", Cipher.SECRET_KEY);
            sessionKeys.put(sender, sessionKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        if(firstMessage.get(sender) != null){
            sendEncryptedMessageToUser(sender, firstMessage.get(sender));
            firstMessage.remove(sender);
        }
    }

    public void handleReceivedPubKey(String sender, String pubKey) {
        byte[] decodedPublicKey = Base64.getDecoder().decode(pubKey);
        KeyGenerator keyGenerator;
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPublicKey);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey sessionKey = keyGenerator.generateKey();
            sessionKeys.put(sender, sessionKey);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.WRAP_MODE, publicKey);
            byte[] encryptedSessionKey = cipher.wrap(sessionKey);
            String encodedSessionKeyString = Base64.getEncoder().encodeToString(encryptedSessionKey);
            MessageSender.sendMessage("SESSIONKEY " + sender + " " + encodedSessionKeyString, bufferedWriter);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }
    }
}
