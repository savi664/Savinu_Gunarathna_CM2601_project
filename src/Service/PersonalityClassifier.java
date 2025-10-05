package Service;

import Model.Participant;
import Model.PersonalityType;

import java.util.List;

public class PersonalityClassifier {
    public PersonalityType classifyPersonality(int score){
        if (score >= 90) return PersonalityType.LEADER;
        else if (score >= 70) return PersonalityType.BALANCED;
        else return PersonalityType.THINKER;
    }

    public void ClassifyPersonalityForTeam(List<Participant> participantList){
        for (Participant p : participantList){
            p.setPersonalityType(classifyPersonality(p.getPersonalityScore()));
        }
    }
}
