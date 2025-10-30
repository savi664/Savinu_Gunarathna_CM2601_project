package Service;

import Model.*;
import Exception.SkillLevelOutOfBoundsException;

import java.util.*;
import java.util.regex.Pattern;

public class TeamBuilder {
    private final Deque<Participant> pool;
    private final List<Team> teams = new ArrayList<>();
    private int nextTeamId = 1;

    // caps we don't want to break
    private static final int MAX_SAME_GAME = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MAX_LEADERS = 1;
    private static final int MAX_SOCIALIZERS = 1;
    private static final int MAX_TEAM_SIZE = 6;



    public TeamBuilder(List<Participant> participants) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be empty");
        }
        List<Participant> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled, new Random());
        this.pool = new LinkedList<>(shuffled);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    public List<Team> formTeams() {
        int teamCount = Math.max(1, (int) Math.ceil(pool.size() / (double) MAX_TEAM_SIZE));
        initTeams(teamCount);

        placeEveryone();
        balanceSkills();

        return teams;
    }

    public void removeMemberFromTeams(String participantId) {
        for (Team team : teams) {
            Participant p = team.containsParticipant(participantId);
            if (p != null) {
                team.removeMember(p);
                pool.addLast(p);
                System.out.println("Removed " + p.getName() + " (ID: " + participantId + ") from Team " + team.getTeam_id());
                return;
            }
        }
        System.out.println("Participant ID " + participantId + " not found.");
    }

    public void updateAttribute(String participantId, String what, Object newValue)
            throws IllegalArgumentException, SkillLevelOutOfBoundsException {

        Participant p = findInTeams(participantId);
        if (p == null) {
            System.out.println("Participant ID " + participantId + " not found.");
            return;
        }

        String key = what.toLowerCase().trim();
        switch (key) {
            case "id" -> setId(p, newValue);
            case "email" -> setEmail(p, newValue);
            case "preferred game" -> setGame(p, newValue);
            case "skill level" -> setSkill(p, newValue);
            case "preferred role" -> setRole(p, newValue);
            default -> throw new IllegalArgumentException(
                    "Can't change '" + what + "'. Use: id, email, preferred game, skill level, preferred role");
        }

        System.out.println("Updated " + p.getName() + " (ID: " + participantId + ")");
    }

    public void printTeams() {
        if (teams.isEmpty()) {
            System.out.println("No teams yet.");
            return;
        }

        double totalSkill = 0;
        int count = 0;

        for (Team team : teams) {
            System.out.println("\n==========================");
            System.out.println(" Team " + team.getTeam_id());
            System.out.println("==========================");

            for (Participant p : team.getParticipantList()) {
                System.out.printf(
                        "ID: %-5s | Name: %-15s | Email: %-25s | Role: %-10s | Game: %-10s | Skill: %-2d | Score: %-3d | Type: %-12s%n",
                        p.getId(), p.getName(), p.getEmail(), p.getPreferredRole(),
                        p.getPreferredGame(), p.getSkillLevel(), p.getPersonalityScore(), p.getPersonalityType()
                );
                totalSkill += p.getSkillLevel();
                count++;
            }
            System.out.println("Team Avg Skill: " + team.CalculateAvgSkill());
        }

        double globalAvg = count == 0 ? 0 : totalSkill / count;
        System.out.printf("\nGlobal Average Skill: %.2f%n", globalAvg);
    }

    // -----------------------------------------------------------------

    private void initTeams(int n) {
        teams.clear();
        for (int i = 0; i < n; i++) {
            teams.add(new Team(nextTeamId++));
        }
    }

    private void placeEveryone() {
        int idx = 0;
        while (!pool.isEmpty()) {
            Participant p = pool.removeFirst();
            Team team = nextValidTeam(p, idx);
            team.addMember(p);
            idx = (idx + 1) % teams.size();
        }
    }

    private Team nextValidTeam(Participant p, int start) {
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get((start + i) % teams.size());
            if (canAdd(t, p)) return t;
        }
        // fallback: smallest team
        return teams.stream()
                .min(Comparator.comparingInt(t -> t.getParticipantList().size()))
                .orElse(teams.get(0));
    }

    private Team findFittingTeam(Participant p) {
        return teams.stream()
                .filter(team -> canAdd(team, p))
                .min(Comparator.comparingInt(t -> t.getParticipantList().size()))
                .orElse(null);
    }

    public Participant findInTeams(String id) {
        synchronized (teams) {
            for (Team t : teams) {
                Participant p = t.containsParticipant(id);
                if (p != null) return p;
            }
            return null;
        }
    }

    // --- update helpers ---

    private void setId(Participant p, Object v) {
        if (!(v instanceof String s)) throw new IllegalArgumentException("ID must be String");
        p.setId(s);
    }

    private void setEmail(Participant p, Object v) {
        Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
        if (!(v instanceof String s)) throw new IllegalArgumentException("Email must be String");
        if (!EMAIL_PATTERN.matcher(s).matches()) throw new IllegalArgumentException("Email not in the correct format");
        p.setEmail(s);
    }

    private void setGame(Participant p, Object v) {
        if (!(v instanceof String s)) throw new IllegalArgumentException("Game must be String");
        p.setPreferredGame(s);
    }

    private void setSkill(Participant p, Object v) throws SkillLevelOutOfBoundsException {
        if (!(v instanceof Integer i)) throw new IllegalArgumentException("Skill must be Integer");
        if (i < 1 || i > 10) throw new SkillLevelOutOfBoundsException("Skill 1-10 only");
        p.setSkillLevel(i);
    }

    private void setRole(Participant p, Object v) {
        if (!(v instanceof String s)) throw new IllegalArgumentException("Role must be String");
        try {
            RoleType role = RoleType.valueOf(s.trim().toUpperCase());
            p.setPreferredRole(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be one of: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR");
        }
    }

    // --- constraints ---

    private boolean canAdd(Team team, Participant p) {
        // 1. Size limit
        if (team.getParticipantList().size() >= MAX_TEAM_SIZE) return false;
        if (countGame(team, p.getPreferredGame()) >= MAX_SAME_GAME) return false;

        if (p.getPersonalityType() == PersonalityType.LEADER && countPersonality(team, PersonalityType.LEADER) >= MAX_LEADERS)
            return false;
        if (p.getPersonalityType() == PersonalityType.THINKER && countPersonality(team, PersonalityType.THINKER) >= MAX_THINKERS)
            return false;
        if (p.getPersonalityType() == PersonalityType.SOCIALIZER && countPersonality(team, PersonalityType.SOCIALIZER) >= MAX_SOCIALIZERS)
            return false;

        if (!team.getParticipantList().isEmpty()) {
            double avg = team.getParticipantList().stream()
                    .mapToInt(Participant::getSkillLevel)
                    .average()
                    .orElse(0.0);
            return !(Math.abs(p.getSkillLevel() - avg) > 2.0); // Too big a jump — reject
        }

        return true;
    }

    private long countGame(Team team, String game) {
        return team.getParticipantList().stream()
                .filter(m -> Objects.equals(m.getPreferredGame(), game))
                .count();
    }

    private long countPersonality(Team team, PersonalityType type) {
        return team.getParticipantList().stream()
                .filter(m -> m.getPersonalityType() == type)
                .count();
    }

    // --- skill balancing ---

    private void balanceSkills() {
        if (teams.size() <= 1) return;

        boolean changed;
        do {
            changed = false;
            double bestVar = variance();
            Team bestA = null, bestB = null;
            Participant bestPa = null, bestPb = null;

            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Team a = teams.get(i), b = teams.get(j);
                    for (Participant pa : a.getParticipantList()) {
                        for (Participant pb : b.getParticipantList()) {
                            if (!canSwap(a, b, pa, pb)) continue;

                            swap(a, b, pa, pb);
                            double newVar = variance();
                            if (newVar < bestVar - 1e-6) {
                                bestVar = newVar;
                                bestA = a; bestB = b; bestPa = pa; bestPb = pb;
                                changed = true;
                            }
                            swap(a, b, pb, pa); // undo
                        }
                    }
                }
            }

            if (changed) {
                swap(bestA, bestB, bestPa, bestPb);
            }
        } while (changed);
    }

    private double variance() {
        double[] avgs = teams.stream().mapToDouble(Team::CalculateAvgSkill).toArray();
        double mean = Arrays.stream(avgs).average().orElse(0.0);
        return Arrays.stream(avgs)
                .map(v -> (v - mean) * (v - mean))
                .average().orElse(0.0);
    }

    private boolean canSwap(Team a, Team b, Participant pa, Participant pb) {
        return canAdd(a, pb) && canAdd(b, pa);
    }

    private void swap(Team a, Team b, Participant pa, Participant pb) {
        a.removeMember(pa);
        b.removeMember(pb);
        a.addMember(pb);
        b.addMember(pa);
    }
}