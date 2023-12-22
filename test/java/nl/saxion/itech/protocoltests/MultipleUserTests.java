package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class MultipleUserTests {

    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultipleUserTests.class.getResourceAsStream("testconfig.properties");
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

    @Test
    /* This test is expected to fail with the given NodeJS server. Make sure the test works when implementing
       your own server in Java
     */
    void TC3_1_joinedIsReceivedByOtherUserWhenUserConnects() {
        receiveLineWithTimeout(inUser1); //INIT
        receiveLineWithTimeout(inUser2); //INIT

        // Connect user1
        outUser1.println("IDENT user1");
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println("IDENT user2");
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK

        //JOINED is received by user1 when user2 connects
        String resIdent = receiveLineWithTimeout(inUser1);
        assertEquals("JOINED user2", resIdent);
    }

    @Test
    /* This test is expected to fail with the given NodeJS server. Make sure the test works when implementing
       your own server in Java
     */
    void TC3_2_bcstMessageIsReceivedByOtherConnectedClients() {
        receiveLineWithTimeout(inUser1); //INIT
        receiveLineWithTimeout(inUser2); //INIT

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
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send BCST from user 1
        outUser1.println("BCST messagefromuser1");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1);
        assertEquals("OK BCST messagefromuser1", fromUser1);

        String fromUser2 = receiveLineWithTimeout(inUser2);
        assertEquals("BCST user1 messagefromuser1", fromUser2);

        //send BCST from user 2
        outUser2.println("BCST messagefromuser2");
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2);
        assertEquals("OK BCST messagefromuser2", fromUser2);

        fromUser1 = receiveLineWithTimeout(inUser1);
        assertEquals("BCST user2 messagefromuser2", fromUser1);
    }

    @Test
    void TC3_3_identMessageWithAlreadyConnectedUsernameReturnsError() {
        receiveLineWithTimeout(inUser1); //init message
        receiveLineWithTimeout(inUser2); //init message

        // Connect user 1
        outUser1.println("IDENT user1");
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect using same username
        outUser2.println("IDENT user1");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        assertEquals("FAIL01 User already logged in", resUser2);
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}