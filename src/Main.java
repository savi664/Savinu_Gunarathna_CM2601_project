import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Service.CSVHandler;
import Service.PersonalityClassifier;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Exception.InvalidSurveyDataException;
import Exception.SkillLevelOutOfBoundsException;
import Service.TeamBuilder;

public class Main {
    public static void main(String[] args) throws InvalidSurveyDataException, SkillLevelOutOfBoundsException, IOException {
        Scanner scanner = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        PersonalityClassifier personalityClassifier = new PersonalityClassifier();
        List<Participant> participants;
        TeamBuilder teamBuilder;

        String menu = """
                                \n
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
            System.out.print("Enter path to participants CSV file: ");
            String csvPath = scanner.nextLine();
            try {
                participants = csvHandler.ReadCSV(csvPath);
                System.out.println("Loaded " + participants.size() + " participants.");
                teamBuilder = new TeamBuilder(participants,participants.size()/5);
                teamBuilder.formTeams();
            } catch (Exception e) {
                System.out.println("Error reading CSV: " + e.getMessage());
                continue;
            }

            System.out.println(menu);
            System.out.print("Please enter what you want to do: ");
            int choice = scanner.nextInt();

            switch (choice){
                case 1:
                    System.out.print("Enter Participant ID: ");
                    String id = scanner.nextLine();
                    scanner.nextLine();

                    System.out.print("Enter Name: ");
                    String name = scanner.nextLine();

                    // Email input with regex validation
                    String email;
                    while (true) {
                        System.out.print("Enter Email: ");
                        email = scanner.nextLine();
                        if (isValidEmail(email)) break;
                        System.out.println("Invalid email format. Please try again.");
                    }

                    System.out.print("Enter Preferred Game: ");
                    String preferredGame = scanner.nextLine();

                    System.out.print("Enter Skill Level (integer): ");
                    int skillLevel = scanner.nextInt();
                    if (skillLevel > 10){
                        throw new SkillLevelOutOfBoundsException("Please enter a value between 1-10");
                    }
                    scanner.nextLine(); // consume newline

                    System.out.print("Select Preferred Role (STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR): ");
                    String roleInput = scanner.nextLine().toUpperCase();
                    RoleType preferredRole = RoleType.valueOf(roleInput);


                    int score  = personalityClassifier.CalculatePersonalityScore(personalityClassifier.ConductSurvey());

                    PersonalityType personalityType = personalityClassifier.classifyPersonality(score);

                    Participant participant = new Participant(id, name, email, preferredGame, skillLevel, preferredRole, score, personalityType);

                    int teamId = teamBuilder.addMembertoTeam(participant);
                    System.out.println("The participant was added to team "+ teamId );
                    break;


                case 2:
                    System.out.print("Please enter the ID of the participant you want to remove from the team: ");
                    String idToRemove = scanner.nextLine();
                    teamBuilder.RemoveMemberFromTeam(idToRemove);
                    break;

                case 3:
                    String pathToPaste = scanner.nextLine();
                    csvHandler.toCSV(pathToPaste);
                    break;

                case 4:
                    break;

                case 5:
                    teamBuilder.printTeams();
                    break;

                case 6:
                    System.out.println("\n Exiting.....");
                    return;

                default:
                    System.out.println("Please enter a valid choice");
                    break;


            }

        }
    }

    // Helper method to check if the email is in the right format
    private static boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
