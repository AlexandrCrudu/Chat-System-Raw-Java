package client;

import utils.MenuInput;
import utils.MessageSender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ClientSurveyHandler {
    public final Object condition = new Object();
    private final BufferedWriter bufferedWriter;
    private final Scanner scanner;
    private HashMap<String, String[]> surveyContents = new HashMap<>();

    public ClientSurveyHandler(BufferedWriter bufferedWriter, Scanner scanner) {
        this.bufferedWriter = bufferedWriter;
        this.scanner = scanner;
    }

    public void surveyRequest() throws IOException {
        MessageSender.sendMessage("SURVEYREQUEST", bufferedWriter);
        synchronized(condition) {
            try {
                condition.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendSurvey(String userList) throws IOException {
        HashMap<String, String[]> survey = buildSurvey();
        ArrayList<String> users = chooseUsers(userList);
        StringBuilder builder = new StringBuilder();
        builder.append("SURVEY ");
        for (String user:users) {
            builder.append(user).append("-");
        }
        builder.deleteCharAt(builder.length()-1);
        builder.append("//");
        for (Map.Entry<String, String[]> set: survey.entrySet()) {
            builder.append(set.getKey()).append(":");
            for (String string:set.getValue())
                if(string != null) {
                    builder.append(string).append("-");
                }
            builder.deleteCharAt(builder.length()-1);
            builder.append("//");
        }
        builder.deleteCharAt(builder.length()-1);
        builder.deleteCharAt(builder.length()-1);
        String finalString = builder.toString();
        MessageSender.sendMessage(finalString, bufferedWriter);
        synchronized(condition){
            condition.notify();
        };
        scanner.nextLine();
    }

    private ArrayList<String> chooseUsers(String userList){
        ArrayList<String> users = new ArrayList<>();
        String[] usersParts = userList.split(" ");
        System.out.println(usersParts.length);
        System.out.println("Here is a list of all available users: ");
        for (int i = 0; i < usersParts.length; i++) {
            System.out.println((i+1) + ". " + usersParts[i]);
        }
        System.out.println("Please select the number corresponding to the user of your choice:");
        int choice1 = 0;
        while(choice1 == 0)
            try {
                choice1 = scanner.nextInt();
                users.add(usersParts[choice1-1]);
            }catch(Exception e){
                choice1 = 0;
                System.out.println("Invalid input, try again!");
            }

        System.out.println("Please add users:");
        choice1 = 0;
        while(choice1 == 0)
            try {
                choice1 = scanner.nextInt();
                if(!users.contains(usersParts[choice1-1]))
                    users.add(usersParts[choice1-1]);
                else {
                    System.out.println("User already added, please try again!");
                    choice1 = 0;
                }
            }catch(Exception e){
                choice1 = 0;
                System.out.println("Invalid input, try again!");
            }

        System.out.println("Please add users or type 0 when you want to stop selecting users");

        while(true)
            try {
                choice1 = scanner.nextInt();
                if(choice1 == 0)
                    break;
                if(!users.contains(usersParts[choice1-1]))
                    users.add(usersParts[choice1-1]);
            }catch(Exception e){
                System.out.println("Invalid input, try again!");
            }
        return users;
    }

    private HashMap<String, String[]> buildSurvey() {
        HashMap<String, String[]> survey = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            System.out.println("Please add the question with number " + i + " to the survey");
            if(i > 1) {
                System.out.println("Send an empty message when you're done adding messages to the survey");
            }
            String question = scanner.nextLine();
            if(i == 1)
                while(question.isEmpty()){
                    System.out.println("You cannot submit a survey without questions, try again:");
                    question = scanner.nextLine();
                }

            if (question.isEmpty()) {
                break;
            }
            else {
                String[] answers = new String[4];
                System.out.println("Now you need to add a minimum of 2 answers, but you can add up to 4 answers.");
                for (int j = 1; j < 5 ; j++) {
                    System.out.println("Add the answer with number " + j + ": ");
                    if(j>2) {
                        System.out.println("Send empty to finish!");
                    }
                    String answer = scanner.nextLine();
                    if(j<=2 && answer.isEmpty()) {
                        while(answer.isEmpty()) {
                            System.out.println("Answer cannot be empty, try again:");
                            answer = scanner.nextLine();
                        }
                    } else if (j>2 && answer.isEmpty()) {
                        break;
                    }
                    answers[j-1] = answer;
                }
                survey.put(question,answers);
            }
        }
        return survey;
    }

    public void handleSurvey() throws IOException {
        if(!surveyContents.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUBMITSURVEY //");
            for (Map.Entry<String, String[]> entry : surveyContents.entrySet()) {
                System.out.println(entry.getKey());
                for (int i = 0; i < entry.getValue().length; i++) {
                    System.out.println((i + 1) + ". " + entry.getValue()[i]);
                }
                int choice = MenuInput.menuChoice(entry.getValue().length, scanner);
                stringBuilder.append(entry.getKey()).append(":").append(entry.getValue()[choice - 1]).append("//");
            }

            MessageSender.sendMessage(stringBuilder.toString(), bufferedWriter);
            surveyContents = new HashMap<>();
        } else {
            System.out.println("There are no ongoing surveys at the moment");
        }
    }

    public void releaseThread() {
        synchronized(condition){
            condition.notify();
        };
    }

    public void updateSurveyContents(HashMap<String, String[]> handleSurvey) {
        surveyContents = handleSurvey;
    }
}
