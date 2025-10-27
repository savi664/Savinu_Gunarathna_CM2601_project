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

import static Service.TeamBuilder.isValidEmail;

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
            teamBuilder = new TeamBuilder(participants);
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
                    formedTeams = teamBuilder.getTeams();
                    System.out.println("Participant added to Team " + teamId);
                }

                case 2 -> {
                    System.out.print("Enter the ID of the participant to remove: ");
                    String idToRemove = scanner.nextLine().trim();
                    teamBuilder.removeMemberFromTeams(idToRemove);
                    formedTeams =  teamBuilder.formTeams();
                }

                case 3 -> {
                    System.out.print("Enter path to save the CSV file: ");
                    String pathToPaste = scanner.nextLine();
                    csvHandler.toCSV(pathToPaste, formedTeams);
                    System.out.println("Teams successfully saved to " + pathToPaste);
                }

                case 4 -> {
                    System.out.print("Enter Participant ID to modify: ");
                    String idToModify = scanner.nextLine().trim();

                    System.out.println("Select attribute to change:");
                    System.out.println("1. ID");
                    System.out.println("2. Email");
                    System.out.println("3. Preferred Game");
                    System.out.println("4. Skill Level");
                    System.out.println("5. Preferred Role");
                    System.out.print("Enter choice (1–5): ");
                    String attrChoiceStr = scanner.nextLine().trim();
                    int attrChoice;
                    try {
                        attrChoice = Integer.parseInt(attrChoiceStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number from 1–5.");
                        continue;
                    }

                    // Validate attrChoice before switch expression
                    if (attrChoice < 1 || attrChoice > 5) {
                        System.out.println("Invalid choice. Please enter 1–5.");
                        continue;
                    }

                    // Map valid choice to attribute
                    String attribute = switch (attrChoice) {
                        case 1 -> "id";
                        case 2 -> "email";
                        case 3 -> "preferred game";
                        case 4 -> "skill level";
                        case 5 -> "preferred role";
                        default -> throw new IllegalStateException("Unexpected value: " + attrChoice);
                    };

                    System.out.print("Enter new value for " + attribute + ": ");
                    String newValueInput = scanner.nextLine().trim();

                    try {
                        Object newValue;
                        switch (attrChoice) {
                            case 1, 2, 3 -> newValue = newValueInput; // String for id, email, preferred game
                            case 4 -> {
                                try {
                                    int skillLevel = Integer.parseInt(newValueInput);
                                    if (skillLevel < 1 || skillLevel > 10) {
                                        throw new SkillLevelOutOfBoundsException("Skill level must be between 1 and 10");
                                    }
                                    newValue = skillLevel;
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid skill level. Must be an integer between 1 and 10.");
                                    continue;
                                }
                            }
                            case 5 -> {
                                try {
                                    RoleType.valueOf(newValueInput.toUpperCase()); // Validate role
                                    newValue = newValueInput.toUpperCase(); // Pass as string for updateAtrribiute
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Invalid role. Must be STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, or COORDINATOR.");
                                    continue;
                                }
                            }
                            default -> {
                                System.out.println("Invalid choice.");
                                continue;
                            }
                        }

                        if (attrChoice == 2 && !isValidEmail(newValueInput)) {
                            System.out.println("Invalid email format.");
                            continue;
                        }

                        teamBuilder.updateAtrribiute(idToModify, attribute, newValue);
                        formedTeams = teamBuilder.getTeams(); // Update formedTeams
                    } catch (IllegalArgumentException | SkillLevelOutOfBoundsException e) {
                        System.out.println("Error updating participant: " + e.getMessage());
                    }
                }

                case 5 -> teamBuilder.printTeams();

                case 6 -> {
                    System.out.println("\nExiting...");
                    return;
                }

                default ->{System.out.println("Please enter a valid choice (1–6).");}

            }
        }
    }


}
