package utils;

import java.util.HashMap;

public class SurveyBuilder {

    public static HashMap<String, String[]> handleSurvey(String protocolString) {
        String stringWithoutProtocolWord = protocolString.split(" ",2)[1];
        String[] setsOfQuestionsAndAnswers = stringWithoutProtocolWord.split("//", 2)[1].split("//");
        HashMap<String, String[]> questionsAndAnswers = new HashMap<>();

        for (String set:setsOfQuestionsAndAnswers) {
            String[] separatedString = set.split(":");
            String question = separatedString[0];
            String[] answers = separatedString[1].split("-");
            questionsAndAnswers.put(question,answers);
        }
        return questionsAndAnswers;
    }
}
