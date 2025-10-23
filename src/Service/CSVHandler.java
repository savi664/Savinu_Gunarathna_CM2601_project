package Service;
import Exception.InvalidSurveyDataException;
import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVHandler {

    public List<Participant> ReadCSV(String path) throws IOException, InvalidSurveyDataException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
        String line = reader.readLine(); // skip header if exists
        List<Participant> participantList = new ArrayList<>();
        line = reader.readLine();

        while (line != null) {
            String[] values = line.split(",");
            if (values.length < 8) { // CSV has 8 columns
                throw new InvalidSurveyDataException("Invalid CSV row: " + line);
            }

            try {
                String id = values[0].trim();
                String name = values[1].trim();
                String email = values[2].trim();
                String preferredGame = values[3].trim();
                int skillLevel = Integer.parseInt(values[4].trim());
                RoleType role = RoleType.valueOf(values[5].trim().toUpperCase());
                int personalityScore = Integer.parseInt(values[6].trim());
                PersonalityType personalityType = PersonalityType.valueOf(values[7].trim().toUpperCase());

                participantList.add(new Participant(id, name, email, preferredGame, skillLevel, role, personalityScore, personalityType));

            } catch (NumberFormatException e) {
                throw new InvalidSurveyDataException("Invalid number in row: " + line);
            } catch (IllegalArgumentException e) {
                throw new InvalidSurveyDataException("Invalid enum value in row: " + line);
            }

            line = reader.readLine();
        }

        reader.close();
        return participantList;
    }



    public void toCSV(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));

        // Write header
        writer.write("TeamID,ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType");
        writer.newLine();

        for (Team team : TeamBuilder.teams) {
            for (Participant p : team.getParticipantList()) {
                String line = String.join(",",
                        String.valueOf(team.getTeam_id()), // Team ID
                        p.getId(),
                        p.getName(),
                        p.getEmail(),
                        p.getPreferredGame(),
                        String.valueOf(p.getSkillLevel()),
                        p.getPreferredRole().name(),
                        String.valueOf(p.getPersonalityScore()),
                        p.getPersonalityType().name()
                );
                writer.write(line);
                writer.newLine();
            }
            writer.newLine();
        }

        writer.close();
    }

}
