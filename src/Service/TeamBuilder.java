package Service;

import Model.*;
import Exception.SkillLevelOutOfBoundsException;

import java.util.*;
import java.util.regex.Pattern;

public class TeamBuilder {
    private final Deque<Participant> participantPool;
    private final List<Team> teams = new ArrayList<>();
    private int nextTeamId = 1;

    private static final int MAX_SAME_GAME = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MAX_LEADERS = 1;
    private static final int MAX_SOCIALIZERS = 1;
    private static final int MAX_TEAM_SIZE = 6;

    public TeamBuilder(List<Participant> participants) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be empty");
        }
        List<Participant> shuffledParticipants = new ArrayList<>(participants);
        Collections.shuffle(shuffledParticipants, new Random());
        this.participantPool = new LinkedList<>(shuffledParticipants);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    public List<Team> formTeams() {
        try {
            if (participantPool == null || participantPool.isEmpty()) {
                throw new IllegalStateException("Participant pool is empty");
            }

            int teamCount = Math.max(1, (int) Math.ceil(participantPool.size() / (double) MAX_TEAM_SIZE));
            initTeams(teamCount);
            placeEveryone();
            balanceSkills();
            return teams;
        } catch (Exception e) {
            System.err.println("Error in formTeams: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void removeMemberFromTeams(String participantId) {
        for (Team team : teams) {
            Participant participant = team.containsParticipant(participantId);
            if (participant != null) {
                team.removeMember(participant);
                System.out.println("Removed " + participant.getName() + " (ID: " + participantId + ") from Team " + team.getTeam_id());
                return;
            }
        }
        System.out.println("Participant ID " + participantId + " not found.");
    }

    public void updateAttribute(String participantId, String attributeName, Object newValue)
            throws IllegalArgumentException, SkillLevelOutOfBoundsException {

        Participant participant = findInTeams(participantId);
        if (participant == null) {
            System.out.println("Participant ID " + participantId + " not found.");
            return;
        }

        String attribute = attributeName.toLowerCase().trim();
        switch (attribute) {
            case "id" -> setId(participant, newValue);
            case "email" -> setEmail(participant, newValue);
            case "preferred game" -> setGame(participant, newValue);
            case "skill level" -> setSkill(participant, newValue);
            case "preferred role" -> setRole(participant, newValue);
            default -> throw new IllegalArgumentException(
                    "Can't change '" + attributeName + "'. Use: id, email, preferred game, skill level, preferred role");
        }

        System.out.println("Updated " + participant.getName() + " (ID: " + participantId + ")");
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
        }

        double globalAvg = participantCount == 0 ? 0 : totalSkill / participantCount;
        System.out.printf("\nGlobal Average Skill: %.2f%n", globalAvg);
    }

    private void initTeams(int numberOfTeams) {
        teams.clear();
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new Team(nextTeamId++));
        }
    }

    private void placeEveryone() {
        if (participantPool.isEmpty()) return;

        List<Participant> sortedBySkill = new ArrayList<>(participantPool);
        sortedBySkill.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        int currentTeamIndex = 0;
        boolean movingForward = true;

        for (Participant participant : sortedBySkill) {
            if (participant == null) continue;

            Team selectedTeam = findTeamForPlacement(participant, currentTeamIndex);
            if (selectedTeam == null) continue;

            selectedTeam.addMember(participant);

            if (movingForward) {
                currentTeamIndex++;
                if (currentTeamIndex >= teams.size()) {
                    currentTeamIndex = teams.size() - 1;
                    movingForward = false;
                }
            } else {
                currentTeamIndex--;
                if (currentTeamIndex < 0) {
                    currentTeamIndex = 0;
                    movingForward = true;
                }
            }
        }
        participantPool.clear();
    }

    private Team findTeamForPlacement(Participant participant, int startIndex) {
        for (int offset = 0; offset < teams.size(); offset++) {
            Team team = teams.get((startIndex + offset) % teams.size());
            if (canAddDuringPlacement(team, participant)) return team;
        }
        return teams.stream()
                .min(Comparator.comparingInt(t -> t.getParticipantList().size()))
                .orElse(teams.get(0));
    }

    private boolean canAddDuringPlacement(Team team, Participant participant) {
        if (team.getParticipantList().size() >= MAX_TEAM_SIZE) return false;
        if (countGame(team, participant.getPreferredGame()) >= MAX_SAME_GAME) return false;
        if (participant.getPersonalityType() == PersonalityType.LEADER && countPersonality(team, PersonalityType.LEADER) >= MAX_LEADERS)
            return false;
        if (participant.getPersonalityType() == PersonalityType.THINKER && countPersonality(team, PersonalityType.THINKER) >= MAX_THINKERS)
            return false;
        if (participant.getPersonalityType() == PersonalityType.SOCIALIZER && countPersonality(team, PersonalityType.SOCIALIZER) >= MAX_SOCIALIZERS)
            return false;
        return true;
    }

    public Team findFittingTeam(Participant participant) {
        return teams.stream()
                .filter(team -> canAdd(team, participant))
                .min(Comparator.comparingInt(t -> t.getParticipantList().size()))
                .orElse(null);
    }

    public Participant findInTeams(String participantId) {
        synchronized (teams) {
            for (Team team : teams) {
                Participant participant = team.containsParticipant(participantId);
                if (participant != null) return participant;
            }
            return null;
        }
    }

    private void setId(Participant participant, Object value) {
        if (!(value instanceof String newId)) throw new IllegalArgumentException("ID must be String");
        participant.setId(newId);
    }

    private void setEmail(Participant participant, Object value) {
        Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
        if (!(value instanceof String newEmail)) throw new IllegalArgumentException("Email must be String");
        if (!EMAIL_PATTERN.matcher(newEmail).matches()) throw new IllegalArgumentException("Email not in the correct format");
        participant.setEmail(newEmail);
    }

    private void setGame(Participant participant, Object value) {
        if (!(value instanceof String newGame)) throw new IllegalArgumentException("Game must be String");
        participant.setPreferredGame(newGame);
    }

    private void setSkill(Participant participant, Object value) throws SkillLevelOutOfBoundsException {
        if (!(value instanceof Integer newSkillLevel)) throw new IllegalArgumentException("Skill must be Integer");
        if (newSkillLevel < 1 || newSkillLevel > 10) throw new SkillLevelOutOfBoundsException("Skill 1-10 only");
        participant.setSkillLevel(newSkillLevel);
    }

    private void setRole(Participant participant, Object value) {
        if (!(value instanceof String roleString)) throw new IllegalArgumentException("Role must be String");
        try {
            RoleType newRole = RoleType.valueOf(roleString.trim().toUpperCase());
            participant.setPreferredRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be one of: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR");
        }
    }

    private boolean canAdd(Team team, Participant participant) {
        if (team.getParticipantList().size() >= MAX_TEAM_SIZE) return false;
        if (countGame(team, participant.getPreferredGame()) >= MAX_SAME_GAME) return false;
        if (participant.getPersonalityType() == PersonalityType.LEADER && countPersonality(team, PersonalityType.LEADER) >= MAX_LEADERS)
            return false;
        if (participant.getPersonalityType() == PersonalityType.THINKER && countPersonality(team, PersonalityType.THINKER) >= MAX_THINKERS)
            return false;
        if (participant.getPersonalityType() == PersonalityType.SOCIALIZER && countPersonality(team, PersonalityType.SOCIALIZER) >= MAX_SOCIALIZERS)
            return false;

        if (!team.getParticipantList().isEmpty()) {
            double teamAvgSkill = team.CalculateAvgSkill();
            return Math.abs(participant.getSkillLevel() - teamAvgSkill) <= 2.0;
        }
        return true;
    }

    private long countGame(Team team, String gameName) {
        return team.getParticipantList().stream()
                .filter(member -> Objects.equals(member.getPreferredGame(), gameName))
                .count();
    }

    private long countPersonality(Team team, PersonalityType personalityType) {
        return team.getParticipantList().stream()
                .filter(member -> member.getPersonalityType() == personalityType)
                .count();
    }

    private void balanceSkills() {
        if (teams.size() <= 1) return;

        for (int iteration = 0; iteration < 50; iteration++) {
            List<Team> sortedTeams = new ArrayList<>(teams);
            sortedTeams.sort(Comparator.comparingDouble(Team::CalculateAvgSkill));

            Team lowestSkillTeam = sortedTeams.get(0);
            Team highestSkillTeam = sortedTeams.get(sortedTeams.size() - 1);

            if (highestSkillTeam.CalculateAvgSkill() - lowestSkillTeam.CalculateAvgSkill() < 1.0) break;

            boolean participantMoved = tryMoveParticipant(highestSkillTeam, lowestSkillTeam);
            if (!participantMoved) participantMoved = tryMoveParticipant(lowestSkillTeam, highestSkillTeam);
            if (!participantMoved) participantMoved = trySwapParticipants(sortedTeams);

            if (!participantMoved) break;
        }
    }

    private boolean tryMoveParticipant(Team sourceTeam, Team destinationTeam) {
        List<Participant> sourceTeamCopy = new ArrayList<>(sourceTeam.getParticipantList());
        for (Participant participant : sourceTeamCopy) {
            if (canAdd(destinationTeam, participant)) {
                sourceTeam.removeMember(participant);
                destinationTeam.addMember(participant);
                return true;
            }
        }
        return false;
    }

    private boolean trySwapParticipants(List<Team> sortedTeams) {
        for (int firstTeamIndex = 0; firstTeamIndex < sortedTeams.size(); firstTeamIndex++) {
            for (int secondTeamIndex = firstTeamIndex + 1; secondTeamIndex < sortedTeams.size(); secondTeamIndex++) {
                Team firstTeam = sortedTeams.get(firstTeamIndex);
                Team secondTeam = sortedTeams.get(secondTeamIndex);

                List<Participant> firstTeamCopy = new ArrayList<>(firstTeam.getParticipantList());
                List<Participant> secondTeamCopy = new ArrayList<>(secondTeam.getParticipantList());

                for (Participant participantFromFirst : firstTeamCopy) {
                    for (Participant participantFromSecond : secondTeamCopy) {
                        if (canSwap(firstTeam, secondTeam, participantFromFirst, participantFromSecond)) {
                            swap(firstTeam, secondTeam, participantFromFirst, participantFromSecond);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean canSwap(Team firstTeam, Team secondTeam, Participant participantFromFirst, Participant participantFromSecond) {
        firstTeam.removeMember(participantFromFirst);
        secondTeam.removeMember(participantFromSecond);
        boolean firstTeamCanAdd = canAdd(firstTeam, participantFromSecond);
        boolean secondTeamCanAdd = canAdd(secondTeam, participantFromFirst);
        firstTeam.addMember(participantFromFirst);
        secondTeam.addMember(participantFromSecond);
        return firstTeamCanAdd && secondTeamCanAdd;
    }

    private void swap(Team firstTeam, Team secondTeam, Participant participantFromFirst, Participant participantFromSecond) {
        firstTeam.removeMember(participantFromFirst);
        secondTeam.removeMember(participantFromSecond);
        firstTeam.addMember(participantFromSecond);
        secondTeam.addMember(participantFromFirst);
    }
}