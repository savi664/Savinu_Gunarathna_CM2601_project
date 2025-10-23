package Service;
import Model.PersonalityType;
import Exception.InvalidSurveyDataException;

import java.util.Arrays;
import java.util.Scanner;


public class PersonalityClassifier {
    Scanner scanner = new Scanner(System.in);
    public int CalculatePersonalityScore(int[] scores){
        return Arrays.stream(scores).sum()*4;
    }

    //Helper class to classify the personality
    public PersonalityType classifyPersonality(int score){
        if (score >= 90) return PersonalityType.LEADER;
        else if (score >= 70) return PersonalityType.BALANCED;
        else if(score >= 50) return PersonalityType.THINKER;
        else return PersonalityType.SOCIALIZER;
    }

    public int[] ConductSurvey() throws InvalidSurveyDataException {
        int[] answers = new int[5];
        String[] questions= {
                "Q1: I enjoy taking the lead and guiding others during group activities.",
                "Q2: I prefer analyzing situations and coming up with strategic solutions.",
                "Q3: I work well with others and enjoy collaborative teamwork.",
                "Q4: I am calm under pressure and can help maintain team morale.",
                "Q5: I like making quick decisions and adapting in dynamic situations."
        };

        for (int i = 0; i < questions.length; i++) {
            System.out.println(questions[i] + " (Enter a number 1-5): ");
            if (!scanner.hasNextInt()) {
                throw new InvalidSurveyDataException("Invalid input. Must be an integer between 1 and 5.");
            }
            int answer = scanner.nextInt();
            scanner.nextLine();
            if (answer < 1 || answer > 5) {
                throw new InvalidSurveyDataException("Invalid answer for Q" + (i + 1) + ". Must be 1-5.");
            }
            answers[i] = answer;
        }

        return answers;
    }

}
