package nl.saxion.itech.protocoltests;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class LineEndings {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = LineEndings.class.getResourceAsStream("testconfig.properties");
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
    void TC2_1_identFollowedByBCSTWithWindowsLineEndingsReturnsOk() {
        receiveLineWithTimeout(in); //init message
        out.print("IDENT myname1\r\nBCST a\r\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname1", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK BCST a", serverResponse);
    }

    @Test
    void TC2_2_identFollowedByBCSTWithLinuxLineEndingsReturnsOk() {
        receiveLineWithTimeout(in); //init message
        out.print("IDENT myname2\nBCST a\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname2", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK BCST a", serverResponse);
    }

    @Test
    void TC2_3_identFollowedByBCSTWithSlashRLineEndingsReturnsOk() {
        receiveLineWithTimeout(in); //init message
        out.print("IDENT myname3\nBCST a\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK IDENT myname3", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK BCST a", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}