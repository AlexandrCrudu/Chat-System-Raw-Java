package utils;

import java.util.HashMap;
import java.util.Map;

public class SurveyInfo {
    private final String question;
    private final HashMap<String, Integer> answersStatistics = new HashMap<>();

    public String getQuestion() {
        return question;
    }

    public SurveyInfo(String question, String[]answers) {
        this.question = question;
        for (String answer: answers) {
            answersStatistics.put(answer,0);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(question).append(":");
        for (Map.Entry<String, Integer> entry:answersStatistics.entrySet()) {
            builder.append(entry.getKey()).append("-").append(entry.getValue());
            builder.append("/");
        }
        return builder.substring(0,builder.toString().length()-1);
    }

    public void updateVotes(String answer) {
        answersStatistics.put(answer, answersStatistics.get(answer) + 1);
    }
}
