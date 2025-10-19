package Service;

import Model.Participant;
import Model.PersonalityType;

import java.util.List;

public class PersonalityClassifier {
    public PersonalityType CalculatePersonalityScore(int Q1, int Q2 , int Q3 , int Q4 , int Q5){
        int score =  (Q1+Q2+Q3+Q4+Q5) * 4;
        return classifyPersonality(score);
    }

    //Helper class to classify the personality
    private PersonalityType classifyPersonality(int score){
        if (score >= 90) return PersonalityType.LEADER;
        else if (score >= 70) return PersonalityType.BALANCED;
        else if(score >= 50) return PersonalityType.THINKER;
        else return PersonalityType.SOCIALIZER;
    }

    public void ClassifyPersonalityForTeam(List<Participant> participantList){
        for (Participant p : participantList){
            p.setPersonalityType(classifyPersonality(p.getPersonalityScore()));
        }
    }
}
