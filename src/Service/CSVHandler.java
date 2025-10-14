package Service;
import Exception.InvalidSurveyDataException;
import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVHandler {

    public List<Participant> ReadCSV(String path) throws IOException, InvalidSurveyDataException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
        String line = reader.readLine();
        List<Participant> participantList = new ArrayList<>();
        while (line!= null){
            String[] values = line.split(",");
            if (values.length < 7) {
                throw new InvalidSurveyDataException("Invalid CSV row: " + line);
            }
            String id = values[0].trim();
            String email = values[1].trim();
            String preferredGame = values[2].trim();
            RoleType role = RoleType.valueOf(values[4]);
            int skillLevel;
            int personalityScore;

            try {
                skillLevel = Integer.parseInt(values[3].trim());
                personalityScore = Integer.parseInt(values[5].trim());
            } catch (NumberFormatException e) {
                throw new InvalidSurveyDataException("Invalid number in row: " + line);
            }

            PersonalityType personalityType  = PersonalityType.valueOf(values[6]);
            participantList.add(new Participant(id,email,preferredGame,skillLevel,role,personalityScore,personalityType));
            line = reader.readLine();
        }
        reader.close();
        return participantList;
    }
}
