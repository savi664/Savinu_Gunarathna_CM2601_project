package Service;

import Model.Participant;
import Model.PersonalityType;
import Model.RoleType;
import Model.Team;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Robust TeamBuilder that:
 * - Uses a LinkedList as the participant pool (safe removeFirst/removeLast).
 * - Keeps teams as an instance field (no static shared state).
 * - Assigns participants greedily while respecting constraints:
 *   - Max 1 LEADER, max 2 THINKERs, max 1 SOCIALIZER per team (BALANCED unrestricted).
 *   - Max 2 players per same preferred game per team.
 *   - Aim for at least 3 distinct roles when team size >= 3 (soft during early fill).
 * - Provides a safer skill-balancing phase using pairwise swaps to reduce variance.
 */
public class TeamBuilder {
    private final Deque<Participant> pool;      // LinkedList-backed pool
    private final  int teamSize;
    private final List<Team> teams = new ArrayList<>();
    private int nextTeamId = 1;

    // Configurable caps (constants here but could be constructor parameters)
    private static final int MAX_SAME_GAME = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MAX_LEADERS = 1;
    private static final int MAX_SOCIALIZERS = 1;

    public TeamBuilder(List<Participant> participants, int teamSize) {
        if (teamSize <= 0) throw new IllegalArgumentException("teamSize must be > 0");
        // Shuffle original list to avoid deterministic bias
        List<Participant> shuffled = new ArrayList<>(participants);
        Random random = new Random();
        Collections.shuffle(shuffled, random);
        this.pool = new LinkedList<>(shuffled);
        this.teamSize = teamSize;
    }

    /**
     * Public entry to form teams.
     */
    public List<Team> formTeams() {
        // 1. Create an estimated number of empty teams to distribute into (ceil)
        int total = pool.size();
        int estimatedTeamCount = Math.max(1, (int) Math.ceil((double) total / teamSize));

        // Initialize empty teams
        for (int i = 0; i < estimatedTeamCount; i++) {
            teams.add(new Team(nextTeamId++));
        }

        // 2. First pass: ensure each team gets one Leader if possible, then thinkers/socializers,
        //    then fill balanced / remaining participants while obeying hard caps.
        distributeByPersonalityPriority();

        // 3. Second pass: try to ensure role diversity (>=3 roles) by swapping/picking from pool
        enforceRoleDiversityAcrossTeams();

        // 4. Final fill pass: fill remaining spots greedily respecting hard caps.
        finalFill();

        // 5. Balance skill levels (iterative pairwise swap to reduce variance)
        balanceSkillLevels(teams);

        return teams;
    }

    // -------------------------
    // Distribution helper phases
    // -------------------------
    private void distributeByPersonalityPriority() {
        // priority list: LEADER, THINKER, SOCIALIZER, BALANCED (for filling)
        assignByPersonality(PersonalityType.LEADER, MAX_LEADERS);
        assignByPersonality(PersonalityType.THINKER, MAX_THINKERS);
        assignByPersonality(PersonalityType.SOCIALIZER, MAX_SOCIALIZERS);

        // Now do round-robin fill with BALANCED or any remaining participants (respect caps)
        roundRobinFill();
    }

    private void assignByPersonality(PersonalityType type, int perTeamCap) {
        if (pool.isEmpty()) return;

        for (Team team : teams) {
            for (int c = 0; c < perTeamCap && team.getParticipantList().size() < teamSize; c++) {
                Participant candidate = findAndRemoveFromPool(x ->
                        x.getPersonalityType() == type && canAddWithRules(team, x));
                if (candidate == null) break;
                team.addMember(candidate);
            }
        }
    }

    private void roundRobinFill() {
        if (pool.isEmpty()) return;
        int idx = 0;
        while (!pool.isEmpty()) {
            Team target = teams.get(idx % teams.size());
            if (target.getParticipantList().size() < teamSize) {
                Participant candidate = pool.peekFirst();
                // If candidate cannot be added under current strict rules, search for a different candidate
                Participant found = findAndRemoveFromPool(p -> canAddWithRules(target, p));
                if (found != null) {
                    target.addMember(found);
                } else {
                    // No candidate fits this target under strict rules -> allow a best-effort relaxed add
                    // We relax role diversity requirement and only respect hard caps (games/personality caps)
                    Participant relaxed = findAndRemoveFromPool(p -> respectsHardCaps(target, p));
                    if (relaxed != null) {
                        target.addMember(relaxed);
                    } else {
                        // nothing can fit right now; break to avoid infinite loop
                        break;
                    }
                }
            }
            idx++;
            // safety: if we've cycled many times and nothing changed, break
            if (idx > teams.size() * (teamSize + 5)) break;
        }
    }

    private void enforceRoleDiversityAcrossTeams() {
        // Aim for at least 3 unique roles for teams that have size >= 3 but < 3 unique roles.
        for (Team team : teams) {
            Set<RoleType> roles = team.getParticipantList().stream()
                    .map(Participant::getPreferredRole)
                    .collect(Collectors.toSet());
            if (team.getParticipantList().size() >= 3 && roles.size() < 3) {
                // Try to pull from pool a participant with a missing role that can be added
                List<RoleType> missing = Arrays.stream(RoleType.values())
                        .filter(r -> !roles.contains(r))
                        .toList();
                for (RoleType missingRole : missing) {
                    Participant candidate = findAndRemoveFromPool(p -> p.getPreferredRole() == missingRole && canAddWithRules(team, p));
                    if (candidate != null) {
                        team.addMember(candidate);
                        roles.add(missingRole);
                        break; // re-evaluate team after change
                    }
                }
            }
        }
    }

    private void finalFill() {
        // Fill any remaining open spots greedily with any participants respecting hard caps
        int safetyCounter = 0;
        while (!pool.isEmpty() && safetyCounter < teams.size() * 10) {
            boolean placedAny = false;
            for (Team team : teams) {
                if (team.getParticipantList().size() >= teamSize) continue;
                Participant p = findAndRemoveFromPool(candidate -> respectsHardCaps(team, candidate));
                if (p != null) {
                    team.addMember(p);
                    placedAny = true;
                }
            }
            if (!placedAny) break;
            safetyCounter++;
        }

        // If there are still participants remaining, create new teams and put them there
        while (!pool.isEmpty()) {
            Team newTeam = new Team(nextTeamId++);
            teams.add(newTeam);
            while (newTeam.getParticipantList().size() < teamSize && !pool.isEmpty()) {
                Participant p = pool.removeFirst();
                newTeam.addMember(p);
            }
        }
    }

    // -------------------------
    // Low-level utility methods
    // -------------------------
    private Participant findAndRemoveFromPool(java.util.function.Predicate<Participant> predicate) {
        Iterator<Participant> it = pool.iterator();
        while (it.hasNext()) {
            Participant p = it.next();
            if (predicate.test(p)) {
                it.remove();
                return p;
            }
        }
        return null;
    }

    /**
     * Strict check used during main assignment: enforces personality caps, game cap, and requires
     * resulting role diversity if team size >= 3. This is conservative; callers may use relaxed checks.
     */
    private boolean canAddWithRules(Team team, Participant candidate) {
        // Game cap
        long sameGameCount = team.getParticipantList().stream()
                .filter(p -> Objects.equals(p.getPreferredGame(), candidate.getPreferredGame()))
                .count();
        if (sameGameCount >= MAX_SAME_GAME) return false;

        // Personality caps
        long leaderCount = countPersonality(team, PersonalityType.LEADER);
        long thinkerCount = countPersonality(team, PersonalityType.THINKER);
        long socialCount = countPersonality(team, PersonalityType.SOCIALIZER);

        switch (candidate.getPersonalityType()) {
            case LEADER -> { if (leaderCount >= MAX_LEADERS) return false; }
            case THINKER -> { if (thinkerCount >= MAX_THINKERS) return false; }
            case SOCIALIZER -> { if (socialCount >= MAX_SOCIALIZERS) return false; }
            default -> {}
        }

        // Role diversity: if team will have >= 3 members after adding, ensure >= 3 unique roles
        Set<RoleType> roles = team.getParticipantList().stream()
                .map(Participant::getPreferredRole)
                .collect(Collectors.toSet());
        roles.add(candidate.getPreferredRole());
        if (team.getParticipantList().size() + 1 >= 3 && roles.size() < 3) return false;

        return true;
    }

    /** Hard caps only (games + personality caps) — used as relaxed test during filling. */
    private boolean respectsHardCaps(Team team, Participant candidate) {
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
            default -> {}
        }
        return true;
    }

    private long countPersonality(Team team, PersonalityType type) {
        return team.getParticipantList().stream()
                .filter(p -> p.getPersonalityType() == type)
                .count();
    }

    // -------------------------
    // Skill balancing
    // -------------------------
    private void balanceSkillLevels(List<Team> teams) {
        if (teams.size() <= 1) return;

        // Compute initial variance
        double currentVar = computeTeamAverageVariance(teams);

        // Iteratively attempt pairwise swaps between teams to reduce variance
        boolean improved;
        int iterations = 0;
        do {
            improved = false;
            iterations++;
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Team a = teams.get(i);
                    Team b = teams.get(j);

                    // Try all candidate swaps (stronger in a with weaker in b)
                    Participant strongestA = a.getStrongestPlayer();
                    Participant weakestB = b.getWeakestPlayer();
                    if (strongestA == null || weakestB == null) continue;

                    // Ensure swaps respect hard caps
                    if (!canSwapRespectingCaps(a, b, strongestA, weakestB)) continue;

                    // Evaluate variance if swapped
                    swapParticipants(a, b, strongestA, weakestB);
                    double newVar = computeTeamAverageVariance(teams);
                    if (newVar + 1e-6 < currentVar) {
                        currentVar = newVar;
                        improved = true;
                    } else {
                        // revert
                        swapParticipants(a, b, weakestB, strongestA);
                    }
                }
            }
        } while (improved && iterations < 50); // small iteration cap
    }

    private double computeTeamAverageVariance(List<Team> teams) {
        DoubleStream avgStream = teams.stream()
                .mapToDouble(Team::CalculateAvgSkill);

        double mean = avgStream.average().orElse(0.0);

        double variance = teams.stream()
                .mapToDouble(Team::CalculateAvgSkill)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);

        return variance;
    }

    private boolean canSwapRespectingCaps(Team a, Team b, Participant pa, Participant pb) {
        // simulate removing pa from a, adding pb to a, and vice versa, and check hard caps
        // For safety, check game caps and personality caps only
        // Check a after swap
        Map<String, Integer> gameCountA = new HashMap<>();
        for (Participant p : a.getParticipantList()) {
            if (p == pa) continue;
            gameCountA.merge(p.getPreferredGame(), 1, Integer::sum);
        }
        gameCountA.merge(pb.getPreferredGame(), 1, Integer::sum);
        if (gameCountA.getOrDefault(pb.getPreferredGame(), 0) > MAX_SAME_GAME) return false;

        Map<PersonalityType, Integer> persA = new HashMap<>();
        for (Participant p : a.getParticipantList()) {
            if (p == pa) continue;
            persA.merge(p.getPersonalityType(), 1, Integer::sum);
        }
        persA.merge(pb.getPersonalityType(), 1, Integer::sum);
        if (persA.getOrDefault(PersonalityType.LEADER, 0) > MAX_LEADERS) return false;
        if (persA.getOrDefault(PersonalityType.THINKER, 0) > MAX_THINKERS) return false;
        if (persA.getOrDefault(PersonalityType.SOCIALIZER, 0) > MAX_SOCIALIZERS) return false;

        // Check b after swap
        Map<String, Integer> gameCountB = new HashMap<>();
        for (Participant p : b.getParticipantList()) {
            if (p == pb) continue;
            gameCountB.merge(p.getPreferredGame(), 1, Integer::sum);
        }
        gameCountB.merge(pa.getPreferredGame(), 1, Integer::sum);
        if (gameCountB.getOrDefault(pa.getPreferredGame(), 0) > MAX_SAME_GAME) return false;

        Map<PersonalityType, Integer> persB = new HashMap<>();
        for (Participant p : b.getParticipantList()) {
            if (p == pb) continue;
            persB.merge(p.getPersonalityType(), 1, Integer::sum);
        }
        persB.merge(pa.getPersonalityType(), 1, Integer::sum);
        if (persB.getOrDefault(PersonalityType.LEADER, 0) > MAX_LEADERS) return false;
        if (persB.getOrDefault(PersonalityType.THINKER, 0) > MAX_THINKERS) return false;
        if (persB.getOrDefault(PersonalityType.SOCIALIZER, 0) > MAX_SOCIALIZERS) return false;

        return true;
    }

    private void swapParticipants(Team a, Team b, Participant pa, Participant pb) {
        // remove pa from a, pb from b; add pb to a, pa to b
        a.removeMember(pa);
        b.removeMember(pb);
        a.addMember(pb);
        b.addMember(pa);
    }

    // -------------------------
    // Mutating utilities: add/remove external operations
    // -------------------------
    public boolean removeMemberFromTeams(String participantId) {
        for (Team t : teams) {
            int idx = t.containsParticipant(participantId);
            if (idx != 0) { // original API semantics assumed non-zero as found index
                Participant removed = t.getParticipantList().get(idx);
                t.removeMember(removed);
                // push removed back to pool for re-assignment (or not — choose behavior)
                pool.addLast(removed);
                return true;
            }
        }
        return false;
    }

    public int addMemberToTeam(Participant candidate) {
        // Try to find best team respecting hard caps and minimizing skill difference
        Team best = null;
        double bestDiff = Double.MAX_VALUE;
        for (Team t : teams) {
            if (t.getParticipantList().size() >= teamSize + 2) continue; // don't overfill
            if (!respectsHardCaps(t, candidate)) continue;
            double diff = Math.abs(t.CalculateAvgSkill() - candidate.getSkillLevel());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = t;
            }
        }

        if (best == null) {
            Team newT = new Team(nextTeamId++);
            newT.addMember(candidate);
            teams.add(newT);
            return newT.getTeam_id();
        } else {
            best.addMember(candidate);
            return best.getTeam_id();
        }
    }

    public void printTeams() {
        if (teams.isEmpty()) {
            System.out.println("No teams created.");
            return;
        }
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
            }
        }
    }
}
