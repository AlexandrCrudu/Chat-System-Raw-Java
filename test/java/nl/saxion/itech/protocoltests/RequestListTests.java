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

public class RequestListTests {
    private static Properties props = new Properties();
    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2, outUser3;
    private final static int max_delta_allowed_ms = 1000;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = RequestListTests.class.getResourceAsStream("testconfig.properties");
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
        outUser3 = new PrintWriter(socketUser3.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
        socketUser3.close();
    }

    @Test
    void requestListReturnsListOfUsers() {
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

        // Made thread sleep to ensure user2 logs in before user3
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        outUser3.println("IDENT user3");
        outUser3.flush();

        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);

        outUser1.println("LIST");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("OK LIST user2 user3", serverResponse);
    }

    @Test
    void requestListWhenYouAreTheOnlyOneOnlineReturnsNoUsernames() {
        outUser1.println("IDENT user1");
        outUser1.flush();
        System.out.println(receiveLineWithTimeout(inUser1)); //OK
        System.out.println(receiveLineWithTimeout(inUser1));

        outUser1.println("LIST");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        System.out.println(serverResponse);
        assertEquals("OK LIST", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }
}
