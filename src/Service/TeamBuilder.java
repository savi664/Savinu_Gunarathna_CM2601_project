package Service;

import Model.*;
import Exception.SkillLevelOutOfBoundsException;

import java.util.*;
import java.util.regex.Pattern;

public class TeamBuilder {
    private final List<Participant> participantPool;
    private final List<Team> teams = new ArrayList<>();
    private final int teamSize;
    private int nextTeamId = 1;

    private static final int MAX_SAME_GAME = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MAX_LEADERS = 1;
    private static final int MAX_SOCIALIZERS = 1;
    private static final int MIN_ROLE_DIVERSITY = 3;

    public TeamBuilder(List<Participant> participants, int teamSize) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be empty");
        }
        if (teamSize < 2 || teamSize > 10) {
            throw new IllegalArgumentException("Team size must be between 2 and 10");
        }
        this.teamSize = teamSize;
        this.participantPool = new ArrayList<>(participants);
        Collections.shuffle(this.participantPool);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    public List<Team> formTeams() {
        int totalParticipants = participantPool.size();

        // Calculate minimum number of teams needed
        // We want to fill teams to teamSize first
        int minTeamCount = totalParticipants / teamSize;
        int remainingParticipants = totalParticipants % teamSize;

        // If there are remaining participants, we need one more team
        int teamCount = minTeamCount;
        if (remainingParticipants > 0) {
            teamCount++;
        }

        // Ensure at least one team
        if (teamCount < 1) {
            teamCount = 1;
        }

        createEmptyTeams(teamCount);
        sortParticipantsBySkill();
        distributeParticipantsToTeams();
        balanceTeamSkills();

        return teams;
    }

    public void removeMemberFromTeams(String participantId) {
        for (Team team : teams) {
            Participant participant = team.containsParticipant(participantId);
            if (participant != null) {
                team.removeMember(participant);
                System.out.println("Removed " + participant.getName() + " from Team " + team.getTeam_id());
                return;
            }
        }
        System.out.println("Participant ID " + participantId + " not found.");
    }

    public void updateParticipantAttribute(Participant participant, String attributeName, Object newValue)
            throws SkillLevelOutOfBoundsException {

        if (participant == null) {
            throw new IllegalArgumentException("Participant cannot be null");
        }

        String attribute = attributeName.toLowerCase().trim();

        if (attribute.equals("email")) {
            updateEmail(participant, newValue);
        } else if (attribute.equals("preferred game")) {
            updateGame(participant, newValue);
        } else if (attribute.equals("skill level")) {
            updateSkillLevel(participant, newValue);
        } else if (attribute.equals("preferred role")) {
            updateRole(participant, newValue);
        } else {
            throw new IllegalArgumentException(
                    "Invalid attribute '" + attributeName + "'. Valid options: email, preferred game, skill level, preferred role");
        }
    }

    public boolean doesAttributeAffectBalance(String attributeName) {
        String attribute = attributeName.toLowerCase().trim();

        if (attribute.equals("preferred game")) {
            return true;
        }
        if (attribute.equals("skill level")) {
            return true;
        }
        if (attribute.equals("preferred role")) {
            return true;
        }

        return false;
    }

    public void printTeams() {
        if (teams.isEmpty()) {
            System.out.println("No teams yet.");
            return;
        }

        double totalSkill = 0;
        int participantCount = 0;

        for (Team team : teams) {
            System.out.println("\n==========================");
            System.out.println(" Team " + team.getTeam_id());
            System.out.println("==========================");

            for (Participant participant : team.getParticipantList()) {
                System.out.printf(
                        "ID: %-5s | Name: %-15s | Email: %-25s | Role: %-10s | Game: %-10s | Skill: %-2d | Score: %-3d | Type: %-12s%n",
                        participant.getId(), participant.getName(), participant.getEmail(), participant.getPreferredRole(),
                        participant.getPreferredGame(), participant.getSkillLevel(), participant.getPersonalityScore(), participant.getPersonalityType()
                );
                totalSkill += participant.getSkillLevel();
                participantCount++;
            }
            System.out.println("Team Avg Skill: " + team.CalculateAvgSkill());
            System.out.println("Team Size: " + team.getParticipantList().size() + "/" + teamSize);
        }

        double globalAvg = 0;
        if (participantCount > 0) {
            globalAvg = totalSkill / participantCount;
        }
        System.out.printf("\nGlobal Average Skill: %.2f%n", globalAvg);
    }

    public Team findFittingTeam(Participant participant) {
        Team bestTeam = null;
        int smallestSize = Integer.MAX_VALUE;

        // First, try to find teams that are not yet full
        for (Team team : teams) {
            int currentSize = team.getParticipantList().size();
            if (currentSize < teamSize && canAddParticipantToTeam(team, participant)) {
                if (currentSize < smallestSize) {
                    smallestSize = currentSize;
                    bestTeam = team;
                }
            }
        }

        return bestTeam;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void createEmptyTeams(int numberOfTeams) {
        teams.clear();
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new Team(nextTeamId++));
        }
    }

    private void sortParticipantsBySkill() {
        participantPool.sort((p1, p2) -> Integer.compare(p2.getSkillLevel(), p1.getSkillLevel()));
    }

    private void distributeParticipantsToTeams() {
        // Distribute participants one by one, trying to fill teams sequentially
        for (Participant participant : participantPool) {
            Team bestTeam = findBestTeamForParticipant(participant);
            bestTeam.addMember(participant);
        }
    }

    private Team findBestTeamForParticipant(Participant participant) {
        Team bestTeam = null;
        int bestPriority = Integer.MAX_VALUE;

        for (Team team : teams) {
            if (canAddParticipantToTeam(team, participant)) {
                int currentSize = team.getParticipantList().size();

                // Priority system:
                // 1. Teams not yet full (lower size = higher priority)
                // 2. Among full teams, pick the smallest
                int priority;
                if (currentSize < teamSize) {
                    // Prioritize filling incomplete teams
                    // Lower size = higher priority (lower number)
                    priority = currentSize;
                } else {
                    // If all constraint-satisfying teams are full, pick smallest
                    priority = 1000 + currentSize;
                }

                if (priority < bestPriority) {
                    bestPriority = priority;
                    bestTeam = team;
                }
            }
        }

        // If no team can accept the participant due to constraints,
        // add to the smallest team regardless of constraints
        if (bestTeam == null) {
            bestTeam = teams.get(0);
            int smallestSize = bestTeam.getParticipantList().size();

            for (int i = 1; i < teams.size(); i++) {
                Team currentTeam = teams.get(i);
                int currentSize = currentTeam.getParticipantList().size();

                if (currentSize < smallestSize) {
                    smallestSize = currentSize;
                    bestTeam = currentTeam;
                }
            }
        }

        return bestTeam;
    }

    private boolean canAddParticipantToTeam(Team team, Participant participant) {
        List<Participant> members = team.getParticipantList();

        // Allow teams to exceed teamSize only if absolutely necessary
        // (all constraints would be violated otherwise)
        // But prefer to stay within teamSize
        if (members.size() >= teamSize) {
            // Check if there are any unfilled teams
            boolean hasUnfilledTeams = false;
            for (Team t : teams) {
                if (t.getParticipantList().size() < teamSize) {
                    hasUnfilledTeams = true;
                    break;
                }
            }

            // If there are unfilled teams, don't add to this full team
            if (hasUnfilledTeams) {
                return false;
            }
        }

        if (!checkGameConstraint(team, participant)) {
            return false;
        }

        if (!checkPersonalityConstraint(team, participant)) {
            return false;
        }

        if (!checkRoleDiversityConstraint(team, participant)) {
            return false;
        }

        return checkSkillBalanceConstraint(team, participant);
    }

    private boolean checkGameConstraint(Team team, Participant participant) {
        int sameGameCount = 0;
        for (Participant member : team.getParticipantList()) {
            if (Objects.equals(member.getPreferredGame(), participant.getPreferredGame())) {
                sameGameCount++;
            }
        }
        return sameGameCount < MAX_SAME_GAME;
    }

    private boolean checkPersonalityConstraint(Team team, Participant participant) {
        PersonalityType participantType = participant.getPersonalityType();
        int personalityCount = 0;

        for (Participant member : team.getParticipantList()) {
            if (member.getPersonalityType() == participantType) {
                personalityCount++;
            }
        }

        if (participantType == PersonalityType.LEADER && personalityCount >= MAX_LEADERS) {
            return false;
        }
        if (participantType == PersonalityType.THINKER && personalityCount >= MAX_THINKERS) {
            return false;
        }
        if (participantType == PersonalityType.SOCIALIZER && personalityCount >= MAX_SOCIALIZERS) {
            return false;
        }

        return true;
    }

    private boolean checkRoleDiversityConstraint(Team team, Participant participant) {
        List<Participant> members = team.getParticipantList();

        if (members.size() < MIN_ROLE_DIVERSITY) {
            return true;
        }

        Set<RoleType> uniqueRoles = new HashSet<>();
        boolean participantRoleExists = false;

        for (Participant member : members) {
            uniqueRoles.add(member.getPreferredRole());
            if (member.getPreferredRole() == participant.getPreferredRole()) {
                participantRoleExists = true;
            }
        }

        if (uniqueRoles.size() < MIN_ROLE_DIVERSITY && !participantRoleExists) {
            return true;
        }

        return true;
    }

    private boolean checkSkillBalanceConstraint(Team team, Participant participant) {
        if (team.getParticipantList().isEmpty()) {
            return true;
        }

        double teamAverage = team.CalculateAvgSkill();
        double skillDifference = Math.abs(participant.getSkillLevel() - teamAverage);

        return skillDifference <= 2.0;
    }

    private void balanceTeamSkills() {
        if (teams.size() <= 1) {
            return;
        }

        for (int iteration = 0; iteration < 50; iteration++) {
            sortTeamsBySkill();

            Team weakestTeam = teams.get(0);
            Team strongestTeam = teams.get(teams.size() - 1);

            double skillGap = strongestTeam.CalculateAvgSkill() - weakestTeam.CalculateAvgSkill();
            if (skillGap < 1.0) {
                break;
            }

            boolean somethingChanged = false;

            if (tryMoveParticipant(strongestTeam, weakestTeam)) {
                somethingChanged = true;
            } else if (tryMoveParticipant(weakestTeam, strongestTeam)) {
                somethingChanged = true;
            } else if (trySwapParticipants()) {
                somethingChanged = true;
            }

            if (!somethingChanged) {
                break;
            }
        }
    }

    private void sortTeamsBySkill() {
        teams.sort(new Comparator<Team>() {
            @Override
            public int compare(Team t1, Team t2) {
                return Double.compare(t1.CalculateAvgSkill(), t2.CalculateAvgSkill());
            }
        });
    }

    private boolean tryMoveParticipant(Team fromTeam, Team toTeam) {
        List<Participant> fromMembers = new ArrayList<>(fromTeam.getParticipantList());

        for (Participant participant : fromMembers) {
            // Don't move if it would make toTeam exceed teamSize
            if (toTeam.getParticipantList().size() >= teamSize) {
                continue;
            }

            if (canAddParticipantToTeam(toTeam, participant)) {
                fromTeam.removeMember(participant);
                toTeam.addMember(participant);
                return true;
            }
        }

        return false;
    }

    private boolean trySwapParticipants() {
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                Team team1 = teams.get(i);
                Team team2 = teams.get(j);

                List<Participant> team1Members = new ArrayList<>(team1.getParticipantList());
                List<Participant> team2Members = new ArrayList<>(team2.getParticipantList());

                for (Participant p1 : team1Members) {
                    for (Participant p2 : team2Members) {
                        team1.removeMember(p1);
                        team2.removeMember(p2);

                        boolean canSwap = canAddParticipantToTeam(team1, p2) &&
                                canAddParticipantToTeam(team2, p1);

                        if (canSwap) {
                            team1.addMember(p2);
                            team2.addMember(p1);
                            return true;
                        }

                        team1.addMember(p1);
                        team2.addMember(p2);
                    }
                }
            }
        }

        return false;
    }

    // ==================== ATTRIBUTE UPDATE METHODS ====================

    private void updateEmail(Participant participant, Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Email must be a String");
        }

        String email = (String) value;
        Pattern emailPattern = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");

        if (!emailPattern.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format. Use format: example@domain.com");
        }

        participant.setEmail(email);
    }

    private void updateGame(Participant participant, Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Game must be a String");
        }

        String game = (String) value;
        participant.setPreferredGame(game);
    }

    private void updateSkillLevel(Participant participant, Object value) throws SkillLevelOutOfBoundsException {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Skill level must be an Integer");
        }

        Integer skillLevel = (Integer) value;

        if (skillLevel < 1 || skillLevel > 10) {
            throw new SkillLevelOutOfBoundsException("Skill level must be between 1 and 10");
        }

        participant.setSkillLevel(skillLevel);
    }

    private void updateRole(Participant participant, Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Role must be a String");
        }

        String roleString = (String) value;

        try {
            RoleType role = RoleType.valueOf(roleString.trim().toUpperCase());
            participant.setPreferredRole(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role. Valid options: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR");
        }
    }
}