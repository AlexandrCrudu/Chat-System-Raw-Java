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

public class SurveyTests {

    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SurveyTests.class.getResourceAsStream("testconfig.properties");
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

    @Test
    void requestSurveyReturnsOkSurveyWithListWhen3UsersAreOnline() {
        login();
        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("OK SURVEYREQUEST user2 user3", serverResponse);
    }

    @Test
    void initiateSurveyWhenLessThan3UsersAreOnlineReturnsError() {
        // Connect user1
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

        // Made thread sleep to ensure user2 logs in before user3
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser1);

        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("FAIL09 Cannot start survey while there are less than 3 users online!", serverResponse);
    }


    @Test
    void startingANewSurveyWhenThereIsAlreadyOneOngoingReturnsError() {
        login();

        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        outUser2.println("SURVEYREQUEST");
        outUser2.flush();


        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("FAIL08 Cannot start survey while there is an ongoing survey already!", serverResponse);
    }

    @Test
    void receivedSurveyByEveryoneWorks() {
        login();

        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        receiveLineWithTimeout(inUser1);
        outUser1.println("SURVEY user2-user3-//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6");

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("SURVEY user2-user3-//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6", serverResponse);
        String serverResponse2 = receiveLineWithTimeout(inUser3);
        assertEquals("SURVEY user2-user3-//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6", serverResponse2);
    }

    @Test
    void creatingSurveyReturnsOk() {
        login();

        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        receiveLineWithTimeout(inUser1);
        outUser1.println("SURVEY user2-user3//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6");

        String serverResponse = receiveLineWithTimeout(inUser1);
        assertEquals("OK SURVEY user2-user3//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6", serverResponse);

    }
    @Test
    void submittingSurveyDetailsReturnsOk() {
        createAndSubmitSurveyForAllUsers();

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("OK SUBMITSURVEY //Question1:Answer1//Question2:Answer5//", serverResponse);
        String serverResponse2 = receiveLineWithTimeout(inUser3);
        assertEquals("OK SUBMITSURVEY //Question1:Answer2//Question2:Answer6//", serverResponse2);
    }



    @Test
    void afterAllUsersCompleteSurveyEveryoneGetsStatistics() {
        createAndSubmitSurveyForAllUsers();
        receiveLineWithTimeout(inUser2);
        receiveLineWithTimeout(inUser3);

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("SURVEYSTATISTICS Question2:Answer4-0/Answer5-1/Answer6-1&Question1:Answer2-1/Answer3-0/Answer1-1", serverResponse);
        String serverResponse2 = receiveLineWithTimeout(inUser3);
        assertEquals("SURVEYSTATISTICS Question2:Answer4-0/Answer5-1/Answer6-1&Question1:Answer2-1/Answer3-0/Answer1-1", serverResponse2);
    }

    @Test
    void submittingASurveyTwiceReturnsError() {
        login();
        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        receiveLineWithTimeout(inUser1);
        outUser1.println("SURVEY user2-user3//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6");

        receiveLineWithTimeout(inUser2);
        receiveLineWithTimeout(inUser3);

        outUser2.println("SUBMITSURVEY //Question1:Answer1//Question2:Answer5//");
        outUser2.println("SUBMITSURVEY //Question1:Answer2//Question2:Answer6//");

        receiveLineWithTimeout(inUser2);

        String serverResponse = receiveLineWithTimeout(inUser2);
        assertEquals("FAIL11 You cannot submit a survey twice!", serverResponse);
    }



    public void skipLinesForUser(BufferedReader user, int nr) {
        for (int i = 0; i < nr; i++) {
            receiveLineWithTimeout(user);
        }
    }

    void createAndSubmitSurveyForAllUsers(){
        login();

        outUser1.println("SURVEYREQUEST");
        outUser1.flush();

        receiveLineWithTimeout(inUser1);
        outUser1.println("SURVEY user2-user3//Question1:Answer1-Answer2-Answer3//Question2:Answer4-Answer5-Answer6");

        receiveLineWithTimeout(inUser2);
        receiveLineWithTimeout(inUser3);

        outUser2.println("SUBMITSURVEY //Question1:Answer1//Question2:Answer5//");
        outUser3.println("SUBMITSURVEY //Question1:Answer2//Question2:Answer6//");

    }

    public void login() {
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

        // Made thread sleep to ensure user2 logs in before user3
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        outUser3.println("IDENT user3");
        outUser3.flush();

        skipLinesForUser(inUser1,3);
        skipLinesForUser(inUser2,3);
        skipLinesForUser(inUser3,2);

    }
}
