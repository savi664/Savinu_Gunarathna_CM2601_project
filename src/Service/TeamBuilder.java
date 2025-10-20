package Service;

import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;

import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {
    private final List<Participant> participants;
    private final int teamSize;
    private final Random random = new Random();

    //This is set to public because it is not a attribute but a placeholder
    public static List<Team> teams;

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

        int totalParticipants = participants.size();
        int estimatedTeams = Math.max(1, totalParticipants / teamSize);

        // Calculate the target min and max size per team
        int minSize = teamSize;
        int maxSize = teamSize + 2; // up to 2 more members allowed

        int teamId = 1;

        // Create teams until all participants are assigned
        while (!participants.isEmpty()) {
            Team team = new Team(teamId);

            // Build team following defined rules
            ensurePersonalityMix(team);
            ensureRoleDiversity(team);
            enforceGameCap(team);

            // Ensure team doesn’t exceed allowed upper range
            while (team.getParticipantList().size() > maxSize && !participants.isEmpty()) {
                Participant last = team.getParticipantList().removeLast();
                participants.add(last); // push back to pool
            }

            teams.add(team);
            teamId++;

            // Stop forming if remaining participants can be distributed fairly
            if (participants.size() <= estimatedTeams && participants.size() <= maxSize) break;
        }

        // If some participants remain, add them fairly to existing teams
        int index = 0;
        while (!participants.isEmpty()) {
            Team target = teams.get(index % teams.size());
            if (target.getParticipantList().size() < maxSize) {
                Participant next = participants.removeFirst();
                if (canAddWithRules(target, next)) {
                    target.addMember(next);
                }
            }
            index++;
        }

        // Adjust for skill fairness
        balanceSkillLevels(teams);

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

        // Get 1 Socializer
        addPersonalityType(team, PersonalityType.SOCIALIZER, 1);

        // Fill remaining slots with Balanced
        while (team.getParticipantList().size() < teamSize && !participants.isEmpty()) {
            Participant next = participants.removeFirst();
            if (next.getPersonalityType() == PersonalityType.BALANCED) {
                team.addMember(next);
            }
        }
    }

    /*
     Helper method that adds up to 'maxCount' participants
     with the given personality type to the team.
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
    private void enforceGameCap(Team team) {
        Map<String, Integer> gameCount = new HashMap<>();
        List<Participant> toRemove = new ArrayList<>();

        // Count game occurrences in the team
        for (Participant p : team.getParticipantList()) {
            String game = p.getPreferredGame();
            int count = gameCount.getOrDefault(game, 0);

            if (count >= 2) {
                // Too many players from this game — move back to pool
                toRemove.add(p);
                participants.add(p);
            } else {
                gameCount.put(game, count + 1);
            }
        }

        // Remove excess players from the team
        team.getParticipantList().removeAll(toRemove);

        // Fill remaining spots if needed
        while (team.getParticipantList().size() < teamSize && !participants.isEmpty()) {
            Participant next = participants.removeFirst();
            String game = next.getPreferredGame();
            int count = gameCount.getOrDefault(game, 0);

            if (count < 2) {
                team.addMember(next);
                gameCount.put(game, count + 1);
            } else {
                // Skip this one for now, place back in pool
                participants.add(next);
            }

            // Safety break to avoid infinite loops
            if (participants.size() <= team.getParticipantList().size()) break;
        }
    }

    /*
      Balances the skill levels across teams.
      Swaps high-skill and low-skill players between teams
      to keep averages close to the global mean.
     */
    private void balanceSkillLevels(List<Team> teams) {
        // Step 1: Calculate global average skill across all teams
        double globalAvg = 0;
        for (Team team : teams) {
            globalAvg += team.CalculateAvgSkill();
        }
        globalAvg /= teams.size();

        // Step 2: Compare each team to the next one and balance if needed
        for (int i = 0; i < teams.size() - 1; i++) {
            Team teamA = teams.get(i);
            Team teamB = teams.get(i + 1);

            double diffA = teamA.CalculateAvgSkill() - globalAvg;
            double diffB = teamB.CalculateAvgSkill() - globalAvg;

            // Step 3: If difference between their averages is too high, swap
            if (Math.abs(diffA - diffB) > 10) {
                // Find the strongest player in A and weakest in B
                Participant strongest;
                Participant weakest;

                strongest = teamA.getStrongestPlayer();
                weakest = teamB.getWeakestPlayer();
                teamA.swapMember(strongest, weakest, teamB);
            }
        }
    }

    public void RemoveMember(int TeamID, String ParticipantID){
        for (Team team: teams) {
            if (team.getTeam_id() == TeamID) {
                // Find and remove participant
                Participant toRemove = null;
                for (Participant p : team.getParticipantList()) {
                    if (p.getId().equalsIgnoreCase(ParticipantID)) {
                        toRemove = p;
                        team.removeMember(toRemove);
                    }
                }
            }

        }

    }

    public boolean addMember(Participant candidate) {
        Team bestTeam = null;
        double minSkillDiff = Double.MAX_VALUE;

        for (Team team : teams) {
            if (canAddWithRules(team, candidate)) {
                // Calculate difference between candidate skill and team average
                double diff = Math.abs(team.CalculateAvgSkill() - candidate.getSkillLevel());

                // Pick the team with closest skill average
                if (diff < minSkillDiff) {
                    minSkillDiff = diff;
                    bestTeam = team;
                }
            }
        }

        if (bestTeam != null) {
            bestTeam.addMember(candidate);
            return true;
        }

        return false; // no suitable team found
    }



    private boolean canAddWithRules(Team team, Participant candidate) {

        // ---- Game Cap Rule (Max 2 per game) ----
        long sameGameCount = team.getParticipantList().stream()
                .filter(p -> p.getPreferredGame().equals(candidate.getPreferredGame()))
                .count();
        if (sameGameCount >= 2) return false;

        // ---- Personality Rule ----
        long leaderCount  = countPersonality(team, PersonalityType.LEADER);
        long thinkerCount = countPersonality(team, PersonalityType.THINKER);
        long socialCount  = countPersonality(team, PersonalityType.SOCIALIZER);

        switch (candidate.getPersonalityType()) {
            case LEADER -> { if (leaderCount >= 1) return false; }
            case THINKER -> { if (thinkerCount >= 2) return false; }
            case SOCIALIZER -> { if (socialCount >= 1) return false; }
            // Not checking balanced because it is a mix of the personalities
        }

        // ---- Role Diversity Rule (At least 3 roles total) ----
        Set<RoleType> roles = team.getParticipantList().stream()
                .map(Participant::getPreferredRole)
                .collect(Collectors.toSet());
        roles.add(candidate.getPreferredRole());

        return roles.size() >= 3;
    }


    private long countPersonality(Team team, PersonalityType type) {
        return team.getParticipantList().stream()
                .filter(p -> p.getPersonalityType() == type)
                .count();
    }

}

