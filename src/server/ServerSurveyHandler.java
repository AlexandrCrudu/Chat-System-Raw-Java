package server;

import utils.MessageSender;
import utils.SurveyBuilder;
import utils.SurveyInfo;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;

public class ServerSurveyHandler {
    private static boolean startedSurvey = false;
    private static String surveyCreator = " ";
    private static ArrayList<SurveyInfo> surveyObjects = new ArrayList<>();
    private static ArrayList<String> usersInSurvey = new ArrayList<>();
    private static int numberOfResponses;
    private Thread surveyThread = new Thread();
    private final ClientHandler clientHandler;


    public ServerSurveyHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handleRequest() throws IOException {
        if(!startedSurvey && ClientHandler.handlers.size() >= 3) {
            surveyCreator = clientHandler.getUsername();
            startedSurvey = true;
            String userList = buildUserList();
            MessageSender.sendMessage("OK SURVEYREQUEST" + userList, clientHandler.getBufferedWriter());
        }
        else if(startedSurvey)
            MessageSender.sendMessage("FAIL08 Cannot start survey while there is an ongoing survey already!", clientHandler.getBufferedWriter());
        else MessageSender.sendMessage("FAIL09 Cannot start survey while there are less than 3 users online!", clientHandler.getBufferedWriter());
    }


    private void sendStatistics(){
        StringBuilder builder = new StringBuilder();
        builder.append("SURVEYSTATISTICS ");
        for (SurveyInfo surveyObject:surveyObjects) {
            builder.append(surveyObject).append("&");
        }
        String surveyString = builder.substring(0,builder.length()-1);
        System.out.println(surveyString);
        MessageSender.sendMessageToAllChatMembers(surveyString);
        resetSurveyVariables();
    }

    public void resetSurveyVariables(){
        surveyCreator = null;
        startedSurvey = false;
        surveyObjects = new ArrayList<>();
        usersInSurvey = new ArrayList<>();
        numberOfResponses = 0;
        ClientHandler.surveyCompletedUsers = new ArrayList<>();
    }

    public void handleSurveySubmission(String message) {
        if(!ClientHandler.surveyCompletedUsers.contains(clientHandler.getUsername())) {
            MessageSender.sendMessageToSpecificClient("OK " + message, clientHandler.getUsername());
            HashMap<String, String[]> survey = SurveyBuilder.handleSurvey(message);
            for (Map.Entry<String, String[]> entry : survey.entrySet()) {
                for (SurveyInfo surveyObject : surveyObjects) {
                    if (surveyObject.getQuestion().equals(entry.getKey())) {
                        surveyObject.updateVotes(entry.getValue()[0]);
                    }
                }
            }
            numberOfResponses++;
            if (numberOfResponses == usersInSurvey.size()) {
                surveyThread.interrupt();
                sendStatistics();
            }
            else
                ClientHandler.surveyCompletedUsers.add(clientHandler.getUsername());
        }
        else{
            MessageSender.sendMessageToSpecificClient("FAIL11 You cannot submit a survey twice!", clientHandler.getUsername());
        }
    }

    public void handleSurvey(String string) {
        System.out.println(string);
        String messageWithoutProtocolWord = string.split(" ",2)[1];
        String[] parts = messageWithoutProtocolWord.split("//",2);
        String[] usernames = parts[0].split("-");
        usersInSurvey.addAll(Arrays.asList(usernames));
        transferHashMapToSurvey(SurveyBuilder.handleSurvey(string));
        forwardSurvey(Arrays.stream(usernames).toList(), string);
        startSurveyThread();
    }


    public void transferHashMapToSurvey(HashMap<String, String[]> builtSurvey){
        for(Map.Entry<String, String[]> entry: builtSurvey.entrySet()){
            surveyObjects.add(new SurveyInfo(entry.getKey(), entry.getValue()));
        }

    }

    public void startSurveyThread(){
        surveyThread = new Thread(() -> {
            try {
                sleep(300000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            sendStatistics();
        });
        surveyThread.start();
    }

    public void forwardSurvey(List<String> usernames, String surveyContents){
        for (ClientHandler handler: ClientHandler.handlers) {
            try{
                if(usernames.contains(handler.getUsername())) {
                    MessageSender.sendMessage(surveyContents,handler.getBufferedWriter());
                }
                else if(Objects.equals(handler.getUsername(), clientHandler.getUsername())){
                    MessageSender.sendMessage("OK " + surveyContents, handler.getBufferedWriter());
                }
            }catch (IOException e) {
                handler.closeEverything(clientHandler.getSocket(), clientHandler.getBufferedReader(),clientHandler.getBufferedWriter());
            }
        }
    }

    public String buildUserList(){
        StringBuilder list = new StringBuilder();
        for (ClientHandler handler : ClientHandler.handlers)
            if (!handler.getUsername().equals(clientHandler.getUsername()))
                list.append(" ").append(handler.getUsername());
        return list.toString();
    }

    public String getSurveyCreator() {
        return surveyCreator;
    }

    public Thread getSurveyThread() {
        return surveyThread;
    }

}
