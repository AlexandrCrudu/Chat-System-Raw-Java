package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class AcceptedUsernames {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 1000;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = AcceptedUsernames.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
    }

    @Test
    void TC1_1_userNameWithThreeCharactersIsAccepted() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT mym");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT mym", serverResponse);
    }

    @Test
    void TC1_2_userNameWithTwoCharactersReturnsError() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT my");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertTrue(serverResponse.startsWith("FAIL02"), "Too short username accepted: "+serverResponse);
    }

    @Test
    void TC1_3_userNameWith14CharectersIsAccepted() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT abcdefghijklmn");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT abcdefghijklmn", serverResponse);
    }

    @Test
    void TC1_4_userNameWith15CharectersReturnsError() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT abcdefghijklmop");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertTrue(serverResponse.startsWith("FAIL02"), "Too long username accepted: "+serverResponse);
    }

    @Test
    void TC1_5_userNameWithBracketReturnsError() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT a)lmn");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertTrue(serverResponse.startsWith("FAIL02"), "Wrong character accepted");
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}