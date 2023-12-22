package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class SingleUserTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 100;

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SingleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();

        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
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
    void TC5_1_initialConnectionToServerReturnsInitMessage() {
        String firstLine = receiveLineWithTimeout(in);
        assertTrue(firstLine.startsWith("INIT Welcome"));
    }

    @Test
    void TC5_2_validIdentMessageReturnsOkMessage() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT myname");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname", serverResponse);
    }

    @Test
    void TC5_3_emptyIdentMessageReturnsErrorMessage() {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("FAIL02 Username has an invalid format or length", serverResponse);
    }

    @Test
    void TC5_4_invalidIdentMessageReturnsErrorMessage(){
        receiveLineWithTimeout(in); //init message
        out.println("IDENT *a*");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("FAIL02 Username has an invalid format or length", serverResponse);
    }

    @Test
    void TC5_5_pongWithoutPingReturnsErrorMessage(){
        receiveLineWithTimeout(in); //init message
        out.println("IDENT myname\nPONG");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("FAIL05 Pong without ping", serverResponse);
    }

    @Test
    void TC5_6_logInTwiceReturnsErrorMessage(){
        receiveLineWithTimeout(in); //init message
        out.println("IDENT first");
        out.flush();
        assertEquals("OK IDENT first", receiveLineWithTimeout(in));
        out.println("IDENT second");
        out.flush();
        assertEquals("FAIL04 User cannot login twice", receiveLineWithTimeout(in));
    }

    @Test
    void TC5_7_pingIsReceivedAtExpectedTime(TestReporter testReporter) {
        receiveLineWithTimeout(in); //init message
        out.println("IDENT myname");
        out.flush();
        receiveLineWithTimeout(in); //server response

        //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
        assertTimeoutPreemptively(ofMillis(ping_time_ms + ping_time_ms_delta_allowed), () -> {
            Instant start = Instant.now();
            String ping = in.readLine();
            Instant finish = Instant.now();

            // Make sure the correct response is received
            assertEquals("PING", ping);

            // Also make sure the response is not received too early
            long timeElapsed = Duration.between(start, finish).toMillis();
            testReporter.publishEntry("timeElapsed", ""+timeElapsed);
            assertTrue(timeElapsed > ping_time_ms - ping_time_ms_delta_allowed);
        });
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}