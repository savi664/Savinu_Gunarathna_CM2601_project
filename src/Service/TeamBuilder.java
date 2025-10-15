package Service;

import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;

import java.util.*;

public class TeamBuilder {
    private List<Participant> participants;
    private int teamSize;
    private Random random = new Random();

    public TeamBuilder(List<Participant> participants, int teamSize) {
        // Copy the list to avoid modifying the original reference
        this.participants = new ArrayList<>(participants);
        this.teamSize = teamSize;
    }

    /**
     * Main method that builds all the teams according to the defined rules.
     */
    public List<Team> formTeams() {
        List<Team> teams = new ArrayList<>();

        // Randomize participants to avoid selection bias
        Collections.shuffle(participants, random);

        int teamId = 1;

        // Keep building teams until we run out of participants
        while (!participants.isEmpty()) {
            Team team = new Team(teamId);

            // Build team step by step using our constraint rules
            ensurePersonalityMix(team);   // Rule 3
            ensureRoleDiversity(team);    // Rule 2
            enforceGameCap(team, 2);      // Rule 1 (max 2 per game)

            teams.add(team);
            teamId++;
        }

        // After forming all teams, adjust for skill fairness
        balanceSkillLevels(teams); // Rule 4

        return teams;
    }

    /**
     * Tries to assign personalities following the mix rule:
     * 1 Leader, up to 2 Thinkers, rest Balanced.
     */
    private void ensurePersonalityMix(Team team) {
        // Get 1 Leader
        addPersonalityType(team, PersonalityType.LEADER, 1);

        // Get up to 2 Thinkers
        addPersonalityType(team, PersonalityType.THINKER, 2);

        // Fill remaining slots with Balanced
        while (team.getParticipantList().size() < teamSize && !participants.isEmpty()) {
            Participant next = participants.removeFirst();
            if (next.getPersonalityType() == PersonalityType.BALANCED) {
                team.addMember(next);
            }
        }
    }

    /**
     * Helper method that adds up to 'maxCount' participants
     * with the given personality type to the team.
     */
    private void addPersonalityType(Team team, PersonalityType type, int maxCount) {
        Iterator<Participant> iterator = participants.iterator();
        int count = 0;

        while (iterator.hasNext() && count < maxCount && team.getParticipantList().size() < teamSize) {
            Participant p = iterator.next();
            if (p.getPersonalityType() == type) {
                team.addMember(p);
                iterator.remove();
                count++;
            }
        }
    }

    /**
     * Ensures the team has at least 3 different roles.
     * If not, it tries to fill missing roles from remaining participants.
     */
    private void ensureRoleDiversity(Team team) {
        Set<RoleType> usedRoles = new HashSet<>();

        // Track existing roles in the team
        for (Participant p : team.getParticipantList()) {
            usedRoles.add(p.getPreferredRole());
        }

        // If we have fewer than 3 unique roles, try to fill the gap
        while (usedRoles.size() < 3 && !participants.isEmpty() && team.getParticipantList().size() < teamSize) {
            Participant candidate = participants.removeFirst();
            if (!usedRoles.contains(candidate.getPreferredRole())) {
                team.addMember(candidate);
                usedRoles.add(candidate.getPreferredRole());
            }
        }
    }

    /**
     * Enforces a cap on how many players from the same game can be in one team.
     * Default cap is 2 players per game.
     */
    private void enforceGameCap(Team team, int maxPerGame) {
        Map<String, Integer> gameCount = new HashMap<>();
        List<Participant> toRemove = new ArrayList<>();

        // Count game occurrences in the team
        for (Participant p : team.getMembers()) {
            String game = p.getGame();
            int count = gameCount.getOrDefault(game, 0);

            if (count >= maxPerGame) {
                // Too many players from this game — move back to pool
                toRemove.add(p);
                participants.add(p);
            } else {
                gameCount.put(game, count + 1);
            }
        }

        // Remove excess players from the team
        team.getMembers().removeAll(toRemove);

        // Fill remaining spots if needed
        while (team.getMembers().size() < teamSize && !participants.isEmpty()) {
            Participant next = participants.remove(0);
            String game = next.getGame();
            int count = gameCount.getOrDefault(game, 0);

            if (count < maxPerGame) {
                team.addMember(next);
                gameCount.put(game, count + 1);
            } else {
                // Skip this one for now, place back in pool
                participants.add(next);
            }

            // Safety break to avoid infinite loops
            if (participants.size() <= team.getMembers().size()) break;
        }
    }

    /**
     * Balances the skill levels across teams.
     * Swaps high-skill and low-skill players between teams
     * to keep averages close to the global mean.
     */
    private void balanceSkillLevels(List<Team> teams) {
        double globalAvg = teams.stream()
                .mapToDouble(Team::averageSkill)
                .average()
                .orElse(0);

        // Try to bring team averages closer to global average
        for (int i = 0; i < teams.size() - 1; i++) {
            Team teamA = teams.get(i);
            Team teamB = teams.get(i + 1);

            double diffA = teamA.averageSkill() - globalAvg;
            double diffB = teamB.- globalAvg;

            // If skill gap between teams is too high, swap members
            if (Math.abs(diffA - diffB) > 10) {
                Participant strong = teamA.getParticipantList().stream()
                        .max(Comparator.comparingInt(Participant::getSkillLevel))
                        .orElse(null);

                Participant weak = teamB.getParticipantList().stream()
                        .min(Comparator.comparingInt(Participant::getSkillLevel))
                        .orElse(null);

                if (strong != null && weak != null) {
                    teamA.getParticipantList().remove(strong);
                    teamB.getParticipantList().remove(weak);
                    teamA.addMember(weak);
                    teamB.addMember(strong);
                }
            }
        }
    }
}

