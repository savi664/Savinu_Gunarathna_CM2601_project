import Model.Participant;
import Model.RoleType;
import Service.CSVHandler;
import Service.PersonalityClassifier;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        PersonalityClassifier personalityClassifier = new PersonalityClassifier();
        List<Participant> participants;

        String menu = """
                                \n
                                Menu
                |==================================|
                |1. Add a new member               |
                |2. Remove a member                |
                |3. Assign members to teams        |
                |4. Paste the teams to a CSV file  |
                |5. Change attribute of participant|
                |6. Print the teams                |
                |7. Exit                           |
                |==================================|
                """;
        while (true) {
            System.out.print("Enter path to participants CSV file: ");
            String csvPath = scanner.nextLine();
            try {
                participants = csvHandler.ReadCSV(csvPath);
                System.out.println("Loaded " + participants.size() + " participants.");
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
                    scanner.nextLine(); // consume newline

                    System.out.println("Select Preferred Role (STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR): ");
                    String roleInput = scanner.nextLine().toUpperCase();
                    RoleType preferredRole = RoleType.valueOf(roleInput);


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
