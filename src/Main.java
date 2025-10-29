import Model.*;
import Service.CSVHandler;
import Service.PersonalityClassifier;
import Service.TeamBuilder;
import Exception.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CSVHandler csvHandler = new CSVHandler();
    private static final PersonalityClassifier classifier = new PersonalityClassifier();

    private static List<Participant> allParticipants = null;
    private static TeamBuilder teamBuilder = null;
    private static List<Team> formedTeams = null;

    public static void main(String[] args) {
        while (true) {
            showActorMenu();
            int choice = getUserInput("Choose (1=Participant, 2=Organizer, 3=Exit): ", 3);
            if (choice == 3) {
                System.out.println("Goodbye!");
                break;
            }
            if (choice == 1) participantMenu();
            else organizerMenu();
        }
        scanner.close();
    }

    // ============================= ACTOR MENU =============================
    private static void showActorMenu() {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("           WHO ARE YOU?");
        System.out.println("1. Participant");
        System.out.println("2. Organizer");
        System.out.println("3. Exit");
        System.out.println("=".repeat(55));
    }

    // ============================= PARTICIPANT MENU =============================
    private static void participantMenu() {
        while (true) {
            System.out.println("\n--- Participant Menu ---");
            System.out.println("1. Register (Take Survey)");
            System.out.println("2. Withdraw");
            System.out.println("3. Check My Team");
            System.out.println("4. Update My Info");
            System.out.println("5. Back");
            int c = getUserInput("Choose (1–5): ", 5);
            if (c == 5) break;

            try {
                switch (c) {
                    case 1 -> registerParticipant();
                    case 2 -> withdrawParticipant();
                    case 3 -> checkMyTeam();
                    case 4 -> updateParticipantInfo();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void registerParticipant() throws IOException, SkillLevelOutOfBoundsException, InvalidSurveyDataException {
        System.out.println("\n--- Register New Participant ---");

        String id = getInput("Enter ID: ");
        String name = getInput("Enter Name: ");
        String email = getValidEmail();
        String game = getInput("Enter Preferred Game: ");
        int skill = getUserInput("Enter Skill Level (1–10): ", 10);
        RoleType role = getValidRole();

        System.out.println("Starting personality survey...");
        int[] answers = classifier.ConductSurvey();
        int score = classifier.CalculatePersonalityScore(answers);
        PersonalityType type = classifier.classifyPersonality(score);

        Participant p = new Participant(id, name, email, game, skill, role, score, type);
        csvHandler.addToCSV(p);
        allParticipants = null; // force reload
        System.out.println("Registered and saved to CSV!");
    }

    private static void withdrawParticipant() throws IOException, InvalidSurveyDataException {
        String id = getInput("Enter your ID to withdraw: ");
        csvHandler.removeFromCSV(id);
        allParticipants = null;
        if (teamBuilder != null) {
            teamBuilder.removeMemberFromTeams(id);
            formedTeams = teamBuilder.getTeams();
        }
    }

    private static void checkMyTeam() {
        if (teamBuilder == null || formedTeams == null) {
            System.out.println("Teams not formed yet. Ask organizer.");
            return;
        }
        String id = getInput("Enter your ID: ");
        Team team = findTeamByParticipantId(id);
        if (team != null) {
            System.out.println("You are in Team " + team.getTeam_id());
            System.out.println("Team Members:");
            team.getParticipantList().forEach(p -> System.out.println("  • " + p.getName() + " (" + p.getId() + ")"));
        } else {
            System.out.println("You are not assigned to any team.");
        }
    }

    private static void updateParticipantInfo() throws IOException, SkillLevelOutOfBoundsException {
        if (teamBuilder == null || formedTeams == null) {
            System.out.println("Teams not formed yet. Ask organizer to form teams first.");
            return;
        }

        String id = getInput("Enter your ID: ");
        Participant p = teamBuilder.findInTeams(id);
        if (p == null) {
            System.out.println("You are not in any team.");
            return;
        }

        System.out.println("Current Info:");
        System.out.println("  Name: " + p.getName());
        System.out.println("  Email: " + p.getEmail());
        System.out.println("  Game: " + p.getPreferredGame());
        System.out.println("  Skill: " + p.getSkillLevel());
        System.out.println("  Role: " + p.getPreferredRole());

        System.out.println("\nWhat do you want to change?");
        System.out.println("1. Email");
        System.out.println("2. Preferred Game");
        System.out.println("3. Skill Level");
        System.out.println("4. Preferred Role");
        int choice = getUserInput("Choose (1–4): ", 4);

        String attribute;
        Object newValue;

        switch (choice) {
            case 1 -> {
                attribute = "email";
                newValue = getValidEmail();
            }
            case 2 -> {
                attribute = "preferred game";
                newValue = getInput("New Preferred Game: ");
            }
            case 3 -> {
                attribute = "skill level";
                newValue = getUserInput("New Skill Level (1–10): ", 10);
            }
            case 4 -> {
                attribute = "preferred role";
                newValue = getValidRole().name();
            }
            default -> {
                System.out.println("Invalid choice.");
                return;
            }
        }

        teamBuilder.updateAttribute(id, attribute, newValue);
        syncCSVAfterUpdate();
        System.out.println("Your info has been updated and saved to CSV.");
    }

    private static void syncCSVAfterUpdate() throws IOException {
        try {
            List<Participant> fromCSV = csvHandler.readCSV("participants_sample.csv");
            List<Participant> updated = new ArrayList<>();

            for (Participant p : fromCSV) {
                Participant inTeam = teamBuilder.findInTeams(p.getId());
                if (inTeam != null) {
                    updated.add(inTeam); // use updated version
                } else {
                    updated.add(p); // keep original
                }
            }
            csvHandler.exportUnassignedUser("participants_sample.csv", updated);
        } catch (Exception e) {
            System.out.println("Warning: Could not sync CSV: " + e.getMessage());
        }
    }

    private static Team findTeamByParticipantId(String id) {
        if (formedTeams == null) return null;
        for (Team t : formedTeams) {
            if (t.containsParticipant(id) != null) {
                return t;
            }
        }
        return null;
    }

    // ============================= ORGANIZER MENU =============================
    private static void organizerMenu() {
        System.out.print("Enter CSV path (or press Enter for participants_sample.csv): ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) path = "participants_sample.csv";

        try {
            allParticipants = csvHandler.readCSV(path);
            System.out.println("Loaded " + allParticipants.size() + " participants.");
        } catch (Exception e) {
            System.out.println("Failed to load CSV: " + e.getMessage());
            return;
        }

        teamBuilder = null;
        formedTeams = null;

        while (true) {
            System.out.println("\n--- Organizer Menu ---");
            System.out.println("1. Form Teams");
            System.out.println("2. View Teams");
            System.out.println("3. Remove Participant");
            System.out.println("4. Export Teams to CSV");
            System.out.println("5. Back");
            int c = getUserInput("Choose (1–5): ", 5);
            if (c == 5) break;

            try {
                switch (c) {
                    case 1 -> formTeams();
                    case 2 -> viewTeams();
                    case 3 -> removeParticipant();
                    case 4 -> exportTeams();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void formTeams() {
        if (allParticipants == null || allParticipants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        teamBuilder = new TeamBuilder(allParticipants);
        formedTeams = teamBuilder.formTeams();
        System.out.println("Teams formed successfully!");
    }

    private static void viewTeams() {
        if (teamBuilder == null || formedTeams == null) {
            System.out.println("Teams not formed yet. Choose 'Form Teams' first.");
            return;
        }
        teamBuilder.printTeams();
    }

    private static void removeParticipant() {
        if (allParticipants == null) {
            System.out.println("No participants loaded.");
            return;
        }
        String id = getInput("Enter Participant ID to remove: ");
        boolean removed = allParticipants.removeIf(p -> p.getId().equalsIgnoreCase(id));
        if (removed) {
            System.out.println("Removed from participant list.");
            if (teamBuilder != null) {
                teamBuilder.removeMemberFromTeams(id);
                formedTeams = teamBuilder.getTeams();
            }
            try {
                csvHandler.exportUnassignedUser("participants_sample.csv", allParticipants);
            } catch (IOException e) {
                System.out.println("Warning: Could not update CSV: " + e.getMessage());
            }
        } else {
            System.out.println("Participant not found.");
        }
    }

    private static void exportTeams() {
        if (formedTeams == null) {
            System.out.println("No teams to export. Form teams first.");
            return;
        }
        System.out.print("Save as (e.g. teams_output.csv): ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) path = "teams_output.csv";

        try {
            csvHandler.toCSV(path, formedTeams);
            System.out.println("Exported to " + path);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    // ============================= INPUT HELPERS =============================
    private static String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String getValidEmail() {
        while (true) {
            String e = getInput("Enter Email: ");
            if (e.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) return e;
            System.out.println("Invalid email format.");
        }
    }

    private static RoleType getValidRole() {
        while (true) {
            System.out.print("Enter Role (STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR): ");
            try {
                return RoleType.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid role.");
            }
        }
    }

    private static int getUserInput(String prompt, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= 1 && v <= max) return v;
                System.out.println("Enter a number between " + 1 + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Must be a number.");
            }
        }
    }
}