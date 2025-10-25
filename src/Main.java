import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;
import Service.CSVHandler;
import Service.PersonalityClassifier;
import Service.TeamBuilder;
import Exception.InvalidSurveyDataException;
import Exception.SkillLevelOutOfBoundsException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Main {
    public static void main(String[] args) throws InvalidSurveyDataException, SkillLevelOutOfBoundsException, IOException {
        Scanner scanner = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        PersonalityClassifier personalityClassifier = new PersonalityClassifier();
        List<Participant> participants;
        TeamBuilder teamBuilder;
        List<Team> formedTeams;

        // Load CSV once
        System.out.print("Enter path to participants CSV file: ");
        String csvPath = scanner.nextLine();

        try {
            participants = csvHandler.ReadCSV(csvPath);
            System.out.println("Loaded " + participants.size() + " participants.");
            teamBuilder = new TeamBuilder(participants, participants.size() / 5);
            formedTeams = teamBuilder.formTeams();
        } catch (Exception e) {
            System.out.println("Error reading CSV: " + e.getMessage());
            return;
        }

        String menu = """
                                
                                Menu
                |==================================|
                |1. Add a new member               |
                |2. Remove a member                |
                |3. Paste the teams to a CSV file  |
                |4. Change attribute of participant|
                |5. Print the teams                |
                |6. Exit                           |
                |==================================|
                """;

        while (true) {
            System.out.println(menu);
            System.out.print("Please enter what you want to do: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number from 1–6.");
                continue;
            }

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter Participant ID: ");
                    String id = scanner.nextLine();

                    System.out.print("Enter Name: ");
                    String name = scanner.nextLine();

                    // Email validation
                    String email;
                    while (true) {
                        System.out.print("Enter Email: ");
                        email = scanner.nextLine();
                        if (isValidEmail(email)) break;
                        System.out.println("Invalid email format. Please try again.");
                    }

                    System.out.print("Enter Preferred Game: ");
                    String preferredGame = scanner.nextLine();

                    System.out.print("Enter Skill Level (1–10): ");
                    int skillLevel;
                    try {
                        skillLevel = Integer.parseInt(scanner.nextLine().trim());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number. Try again.");
                        continue;
                    }
                    if (skillLevel < 1 || skillLevel > 10) {
                        throw new SkillLevelOutOfBoundsException("Please enter a value between 1–10");
                    }

                    System.out.print("Select Preferred Role (STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR): ");
                    RoleType preferredRole = RoleType.valueOf(scanner.nextLine().trim().toUpperCase());

                    int score = personalityClassifier.CalculatePersonalityScore(personalityClassifier.ConductSurvey());
                    PersonalityType personalityType = personalityClassifier.classifyPersonality(score);

                    Participant participant = new Participant(id, name, email, preferredGame, skillLevel, preferredRole, score, personalityType);

                    int teamId = teamBuilder.addMemberToTeam(participant);
                    System.out.println("Participant added to Team " + teamId);
                }

                case 2 -> {
                    System.out.print("Enter the ID of the participant to remove: ");
                    String idToRemove = scanner.nextLine().trim();
                    teamBuilder.removeMemberFromTeams(idToRemove);
                }

                case 3 -> {
                    System.out.print("Enter path to save the CSV file: ");
                    String pathToPaste = scanner.nextLine();
                    csvHandler.toCSV(pathToPaste, formedTeams);
                    System.out.println("Teams successfully saved to " + pathToPaste);
                }

                case 4 -> System.out.println("Feature not implemented yet.");

                case 5 -> teamBuilder.printTeams();

                case 6 -> {
                    System.out.println("\nExiting...");
                    return;
                }

                default ->{System.out.println("Please enter a valid choice (1–6).");}

            }
        }
    }

    private static boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
