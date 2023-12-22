package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class EncryptionTests {
    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = EncryptionTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);

        socketUser3 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser3 = new BufferedReader(new InputStreamReader(socketUser3.getInputStream()));
        outUser3 = new PrintWriter(socketUser3.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {


        socketUser1.close();
        socketUser2.close();
        socketUser3.close();

    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

    //Method that logs in two users
    void login(){
        outUser1.println("IDENT user1");
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Made thread sleep to ensure user2 logs in before user3
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Connect user2
        outUser2.println("IDENT user2");
        outUser2.flush();


        skipLinesForUser(inUser1,2);
        skipLinesForUser(inUser2,2);
    }

    public void skipLinesForUser(BufferedReader user, int nr) {
        for (int i = 0; i < nr; i++) {
            receiveLineWithTimeout(user);
        }
    }

    @Test
    void sendingPublicKeyArrivesAtTheOtherUser() {
        login();

        outUser1.println("PUBKEY user2 randomPublicKey");

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("PUBKEY user1 randomPublicKey", serverResponse);
    }

    @Test
    void receiverSessionKeyArrivesAtTheSender() {
        login();

        outUser1.println("PUBKEY user2 randomPublicKey");
        outUser2.println("SESSIONKEY user1 encryptedSessionKey");


        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("SESSIONKEY user2 encryptedSessionKey", serverResponse);
    }

    @Test
    void sendingPublicKeyToUnknownUserReturnsError() {
        login();

        outUser1.println("PUBKEY user5555 randomPublicKey");

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("FAIL06 Unknown username", serverResponse);
    }

    @Test
    void sendingEncryptedMessageReturnsOk() {
        login();

        outUser1.println("ENCRYPTED user2 encryptedMessage");
        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("OK ENCRYPTED user2 encryptedMessage", serverResponse);
    }

    @Test
    void sendingEncryptedMessageIsReceivedByOtherClient() {
        login();
        outUser1.println("ENCRYPTED user2 encryptedMessage");


        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("ENCRYPTED user1 encryptedMessage", serverResponse);
    }

    @Test
    void sendingAnEncryptedMessageToAnUnknownUserReturnsError() {
        login();
        outUser1.println("ENCRYPTED user5555 encryptedMessage");


        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("FAIL06 Unknown username", serverResponse);
    }
}
