package Service;

import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;
import Exception.SkillLevelOutOfBoundsException;
import com.sun.tools.javac.Main;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

public class TeamBuilder {
    private final Deque<Participant> pool;
    private final List<Team> teams = new ArrayList<>();
    private int nextTeamId = 1;

    // Configurable caps
    private static final int MAX_SAME_GAME = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MAX_LEADERS = 1;
    private static final int MAX_SOCIALIZERS = 1;
    private static final int MAX_TEAM_MEMBERS = 6; // max members per team

    public TeamBuilder(List<Participant> participants) {
        if (participants.isEmpty()) throw new IllegalArgumentException("Participants list cannot be empty");
        List<Participant> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled, new Random());
        this.pool = new LinkedList<>(shuffled);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams); // Return a copy to prevent external modification
    }

    /** Form teams evenly, respecting personality and game caps */
    public List<Team> formTeams() {
        int totalParticipants = pool.size();
        int estimatedTeamCount = Math.max(1, (int) Math.ceil((double) totalParticipants / MAX_TEAM_MEMBERS));

        // Initialize empty teams
        for (int i = 0; i < estimatedTeamCount; i++) {
            teams.add(new Team(nextTeamId++));
        }

        // Round-robin fill respecting caps
        int idx = 0;
        while (!pool.isEmpty()) {
            Participant p = pool.removeFirst();
            boolean placed = false;
            for (int attempts = 0; attempts < teams.size(); attempts++) {
                Team t = teams.get((idx + attempts) % teams.size());
                if (canAddStrict(t, p)) {
                    t.addMember(p);
                    placed = true;
                    idx++;
                    break;
                }
            }
            if (!placed) {
                // If no team can take this participant, place in the team with fewest members (soft add)
                Team minTeam = teams.stream().min(Comparator.comparingInt(a -> a.getParticipantList().size())).orElse(teams.get(0));
                minTeam.addMember(p);
                idx++;
            }
        }

        // Skill balancing
        balanceSkillLevels(teams);

        return teams;
    }

    /** Remove a participant from any team */
    public void removeMemberFromTeams(String participantId) {
        for (Team t : teams) {
            int idx = t.containsParticipant(participantId);
            if (idx != -1) { // Check for != -1 instead of >= 0
                Participant removed = t.getParticipantList().get(idx);
                t.removeMember(removed);
                pool.addLast(removed);
                System.out.println("Participant " + removed.getName() + " (ID: " + participantId + ") removed from Team " + t.getTeam_id());
                return;
            }
        }
        System.out.println("Participant with ID " + participantId + " not found in any team.");
    }

    public void updateAtrribiute(String participantId, String whatToChange, Object newValue) throws IllegalArgumentException, SkillLevelOutOfBoundsException {
        synchronized (teams) { // Ensure thread safety
            for (Team t : teams) {
                int idx = t.containsParticipant(participantId);
                if (idx != -1) {
                    Participant personToUpdate = t.getParticipantList().get(idx);

                    switch (whatToChange.toLowerCase().trim()) {
                        case "id":
                            if (!(newValue instanceof String)) {
                                throw new IllegalArgumentException("New ID must be a String");
                            }
                            String newId = (String) newValue;
                            personToUpdate.setId(newId);
                            break;
                        case "email":
                            if (!(newValue instanceof String)) {
                                throw new IllegalArgumentException("New email must be a String");
                            }
                            String newEmail = (String) newValue;
                            if (!isValidEmail(newEmail)) {
                                throw new IllegalArgumentException("Invalid email format");
                            }
                            personToUpdate.setEmail(newEmail);
                            break;
                        case "preferred game":
                            if (!(newValue instanceof String newPreferredGame)) {
                                throw new IllegalArgumentException("New preferred game must be a String");
                            }
                            personToUpdate.setPreferredGame(newPreferredGame);
                            break;
                        case "skill level":
                            if (!(newValue instanceof Integer)) {
                                throw new IllegalArgumentException("New skill level must be an Integer");
                            }
                            Integer newSkillLevel = (Integer) newValue;
                            if (newSkillLevel < 1 || newSkillLevel > 10) {
                                throw new SkillLevelOutOfBoundsException("Skill level must be between 1 and 10");
                            }
                            personToUpdate.setSkillLevel(newSkillLevel);
                            break;
                        case "preferred role":
                            if (!(newValue instanceof String)) {
                                throw new IllegalArgumentException("New role must be a String");
                            }
                            try {
                                RoleType newRole = RoleType.valueOf(((String) newValue).trim().toUpperCase());
                                personToUpdate.setPreferredRole(newRole);
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Invalid role. Must be STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, or COORDINATOR");
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid attribute to change. Must be id, email, preferred game, skill level, or preferred role");
                    }
                    System.out.println("Participant " + personToUpdate.getName() + " (ID: " + participantId + ") updated successfully.");
                    return;
                }
            }
        }
        System.out.println("Participant with ID " + participantId + " not found in any team.");
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /** Add a participant dynamically to a team */
    public int addMemberToTeam(Participant candidate) {
        Team bestTeam = null;
        int minSize = Integer.MAX_VALUE;
        for (Team t : teams) {
            if (!canAddStrict(t, candidate)) continue;
            int size = t.getParticipantList().size();
            if (size < minSize) {
                minSize = size;
                bestTeam = t;
            }
        }

        if (bestTeam != null) {
            bestTeam.addMember(candidate);
            System.out.println("Added " + candidate.getName() + " (ID: " + candidate.getId() + ") to Team " + bestTeam.getTeam_id());
            return bestTeam.getTeam_id();
        } else {
            Team newTeam = new Team(nextTeamId++);
            newTeam.addMember(candidate);
            teams.add(newTeam);
            System.out.println("Created new Team " + newTeam.getTeam_id() + " and added " + candidate.getName());
            return newTeam.getTeam_id();
        }
    }

    /** Print all teams */
    public void printTeams() {
        if (teams.isEmpty()) {
            System.out.println("No teams created.");
            return;
        }

        double totalSkill = 0;
        int totalMembers = 0;

        for (Team team : teams) {
            System.out.println("\n==========================");
            System.out.println(" Team " + team.getTeam_id());
            System.out.println("==========================");
            for (Participant p : team.getParticipantList()) {
                System.out.printf(
                        "ID: %-5s | Name: %-15s | Email: %-25s | Role: %-10s | Game: %-10s | Skill: %-2d | Personality Score: %-3d | Personality Type: %-12s%n",
                        p.getId(),
                        p.getName(),
                        p.getEmail(),
                        p.getPreferredRole(),
                        p.getPreferredGame(),
                        p.getSkillLevel(),
                        p.getPersonalityScore(),
                        p.getPersonalityType()
                );
                totalSkill += p.getSkillLevel();
                totalMembers++;
            }
            System.out.println("Team Average Skill:"+ team.CalculateAvgSkill());
        }

        double globalAvg = totalMembers == 0 ? 0 : totalSkill / totalMembers;
        System.out.printf("\nGlobal Average Skill: %.2f%n", globalAvg);
    }

    // ------------------ Utility Methods ------------------

    private boolean canAddStrict(Team team, Participant candidate) {
        if (team.getParticipantList().size() >= MAX_TEAM_MEMBERS) return false;

        long sameGameCount = team.getParticipantList().stream()
                .filter(p -> Objects.equals(p.getPreferredGame(), candidate.getPreferredGame()))
                .count();
        if (sameGameCount >= MAX_SAME_GAME) return false;

        long leaderCount = countPersonality(team, PersonalityType.LEADER);
        long thinkerCount = countPersonality(team, PersonalityType.THINKER);
        long socialCount = countPersonality(team, PersonalityType.SOCIALIZER);

        switch (candidate.getPersonalityType()) {
            case LEADER -> { if (leaderCount >= MAX_LEADERS) return false; }
            case THINKER -> { if (thinkerCount >= MAX_THINKERS) return false; }
            case SOCIALIZER -> { if (socialCount >= MAX_SOCIALIZERS) return false; }
        }

        return true;
    }

    private long countPersonality(Team team, PersonalityType type) {
        return team.getParticipantList().stream()
                .filter(p -> p.getPersonalityType() == type)
                .count();
    }

    private void balanceSkillLevels(List<Team> teams) {
        if (teams.size() <= 1) return;

        double currentVar = computeTeamAverageVariance(teams);
        boolean improved;

        do {
            improved = false;
            double bestVar = currentVar;
            Team bestTeamA = null, bestTeamB = null;
            Participant bestPa = null, bestPb = null;

            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Team a = teams.get(i);
                    Team b = teams.get(j);

                    for (Participant pa : a.getParticipantList()) {
                        for (Participant pb : b.getParticipantList()) {
                            if (pa.getSkillLevel() == null || pb.getSkillLevel() == null) continue;
                            if (!canSwapRespectingCaps(a, b, pa, pb)) continue;

                            swapParticipants(a, b, pa, pb);
                            double newVar = computeTeamAverageVariance(teams);
                            if (newVar + 1e-6 < bestVar) {
                                bestVar = newVar;
                                bestTeamA = a;
                                bestTeamB = b;
                                bestPa = pa;
                                bestPb = pb;
                                improved = true;
                            }
                            swapParticipants(a, b, pb, pa); // Undo swap
                        }
                    }
                }
            }

            if (improved) {
                swapParticipants(bestTeamA, bestTeamB, bestPa, bestPb);
                currentVar = bestVar;
            } else {
            }

        } while (improved);
    }

    private double computeTeamAverageVariance(List<Team> teams) {
        DoubleStream avgStream = teams.stream().mapToDouble(Team::CalculateAvgSkill);
        double mean = avgStream.average().orElse(0.0);
        return teams.stream()
                .mapToDouble(Team::CalculateAvgSkill)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
    }

    private boolean canSwapRespectingCaps(Team a, Team b, Participant pa, Participant pb) {
        return canAddStrict(a, pb) && canAddStrict(b, pa);
    }

    private void swapParticipants(Team a, Team b, Participant pa, Participant pb) {
        a.removeMember(pa);
        b.removeMember(pb);
        a.addMember(pb);
        b.addMember(pa);
    }
}
