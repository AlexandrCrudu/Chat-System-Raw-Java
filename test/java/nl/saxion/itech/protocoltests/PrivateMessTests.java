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

public class PrivateMessTests {
    private static Properties props = new Properties();
    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;
    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = PrivateMessTests.class.getResourceAsStream("testconfig.properties");
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
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

    @Test
    void privateMessReturnsOk() {
        // Connect user1
        outUser1.println("IDENT user1");
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Connect user2
        outUser2.println("IDENT user2");
        outUser2.flush();

        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);

        outUser1.println("PRIVATEMESS user2 hey");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("OK PRIVATEMESS user2 hey", serverResponse);

    }

    @Test
    void privateMessReceivedWorks() {
        // Connect user1
        outUser1.println("IDENT user1");
        outUser1.flush();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Connect user2
        outUser2.println("IDENT user2");
        outUser2.flush();

        receiveLineWithTimeout(inUser2);
        receiveLineWithTimeout(inUser2);

        outUser1.println("PRIVATEMESS user2 hey");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("PRIVATEMESS user1 hey", serverResponse);
    }

    @Test
    void sendPrivateMessWithEmptyStringReturnsError() {
        // Connect user1
        outUser1.println("IDENT user1");
        outUser1.flush();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        outUser2.println("IDENT user2");
        outUser2.flush();

        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);

        outUser1.println("PRIVATEMESS user2 ");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("FAIL07 Cannot send empty message", serverResponse);
    }

    @Test
    void sendPrivateMessToUnknownUserReturnsError() {
        // Connect user1
        outUser1.println("IDENT user1");
        outUser1.flush();

        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);

        outUser1.println("PRIVATEMESS user2 hey");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("FAIL06 Unknown username", serverResponse);
    }

}
