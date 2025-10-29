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

    public List<Participant> readCSV(String path) throws IOException, InvalidSurveyDataException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
        String line = reader.readLine();// skip header if exists
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



    // For List<Team>
    public void toCSV(String path, List<Team> teams) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("TeamID,ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType");
            writer.newLine();

            for (Team team : teams) {
                for (Participant p : team.getParticipantList()) {
                    writeParticipant(writer, team.getTeam_id(), p);
                }
            }
        }
    }

    // For List<Participant> (e.g. unassigned people)
    public void exportUnassignedUser(String path, List<Participant> participants) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("TeamID,ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType");
            writer.newLine();

            for (Participant p : participants) {
                writeParticipant(writer, -1, p);  // -1 = no team
            }
        }
    }

    // Helper to avoid duplication
    private void writeParticipant(BufferedWriter writer, int teamId, Participant p) throws IOException {
        writer.write(String.join(",",
                String.valueOf(teamId),
                escapeCSV(p.getId()),
                escapeCSV(p.getName()),
                escapeCSV(p.getEmail()),
                escapeCSV(p.getPreferredGame()),
                String.valueOf(p.getSkillLevel()),
                escapeCSV(p.getPreferredRole().name()),
                String.valueOf(p.getPersonalityScore()),
                escapeCSV(p.getPersonalityType().name())
        ));
        writer.newLine();
    }
    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    public void addToCSV(Participant p) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("participants_sample.csv", true))) {
            writer.write(String.join(",",
                    escapeCSV(p.getId()),
                    escapeCSV(p.getName()),
                    escapeCSV(p.getEmail()),
                    escapeCSV(p.getPreferredGame()),
                    String.valueOf(p.getSkillLevel()),
                    escapeCSV(p.getPreferredRole().name()),
                    String.valueOf(p.getPersonalityScore()),
                    escapeCSV(p.getPersonalityType().name())
            ));
            writer.newLine();
        }
    }

    public void removeFromCSV(String id) throws InvalidSurveyDataException, IOException {
        List<Participant> participants = readCSV("participants_sample.csv");
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id)) {
                participants.remove(p);
                exportUnassignedUser("participants_sample.csv", participants);
            }
        }
        System.out.println("Participant not in the competition");
    }


}
