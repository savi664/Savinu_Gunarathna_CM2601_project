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

    /**
     * Reads participants from a CSV file (input format: no TeamID)
     * Header is skipped if present.
     */
    public List<Participant> readCSV(String path) throws IOException, InvalidSurveyDataException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
        List<Participant> participantList = new ArrayList<>();

        // Skip header
        reader.readLine();
        String line = reader.readLine();

        while (line != null) {
            String[] values = line.split(",", -1); // Preserve empty trailing fields
            if (values.length < 8) {
                throw new InvalidSurveyDataException("Invalid CSV row (expected 8 columns): " + line);
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

    /**
     * Exports formed teams to CSV with TeamID
     */
    public void toCSV(String path, List<Team> teams) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("TeamID,ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType");
            writer.newLine();

            for (Team team : teams) {
                for (Participant p : team.getParticipantList()) {
                    writeParticipantWithTeam(writer, team.getTeam_id(), p);
                }
            }
        }
    }

    /**
     * Exports unassigned participants (pool) to CSV — NO TeamID
     */
    public void exportUnassignedUser(String path, List<Participant> participants) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType");
            writer.newLine();

            for (Participant p : participants) {
                writeParticipantNoTeam(writer, p);
            }
        }
    }

    /**
     * Adds a new participant to the original participant CSV (append mode)
     */
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

    /**
     * Removes a participant from the original CSV by rewriting the file
     */
    public void removeFromCSV(String id) throws IOException, InvalidSurveyDataException {
        List<Participant> participants = readCSV("participants_sample.csv");
        boolean removed = participants.removeIf(p -> p.getId().equalsIgnoreCase(id));

        if (removed) {
            exportUnassignedUser("participants_sample.csv", participants);
            System.out.println("Participant removed from CSV.");
        } else {
            System.out.println("Participant not found in CSV.");
        }
    }

    // === Helper Methods ===

    private void writeParticipantWithTeam(BufferedWriter writer, int teamId, Participant p) throws IOException {
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

    private void writeParticipantNoTeam(BufferedWriter writer, Participant p) throws IOException {
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

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}