package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class PacketBreakup {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = PacketBreakup.class.getResourceAsStream("testconfig.properties");
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
    void TC4_1_identFollowedByBCSTWithMultipleFlushReturnsOk() {
        receiveLineWithTimeout(in); //init message
        out.print("IDENT m");
        out.flush();
        out.print("yname\r\nBC");
        out.flush();
        out.print("ST a\r\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK BCST a", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}