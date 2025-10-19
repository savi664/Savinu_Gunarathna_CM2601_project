package Model;

import Service.TeamBuilder;

import java.util.ArrayList;
import java.util.List;

public class Team {
     private Integer team_id;
     private final List<Participant> participantList;

    public Team(int teamId) {
        this.team_id = teamId;
        this.participantList = new ArrayList<>();
    }

    public Integer getTeam_id() {
        return team_id;
    }

    public void setTeam_id(Integer team_id) {
        this.team_id = team_id;
    }

    public void removeMember(Participant p) {
        participantList.remove(p);
    }

    public List<Participant> getParticipantList() {
        List<Participant> participants = new ArrayList<>();
        for (Participant p : participantList) {
            System.out.println(p);
            participants.add(p);
        }
        return participants;
    }

    public void printParticipants(){
        for (Participant p : participantList) {
            System.out.println("Participant: "+ p);
        }
    }
    public void addMember(Participant participant){
        participantList.add(participant);
    }

    public boolean containsParticipant(String Id){
        for(Participant p: participantList){
            if(p.getId().equalsIgnoreCase(Id)){
                return true;
            }
        }
        return false;
    }

    public int CalculateAvgSkill(){
        int total = 0;
        for(Participant p: participantList){
            total +=  p.getSkillLevel();
        }
        return total/participantList.size();
    }

    // Returns the player with the highest skill level in the team
    public Participant getStrongestPlayer() {
        if (participantList.isEmpty()) return null;

        Participant strongest = participantList.getFirst();
        for (Participant p : participantList) {
            if (p.getSkillLevel() > strongest.getSkillLevel()) {
                strongest = p;
            }
        }
        return strongest;
    }

    // Returns the player with the lowest skill level in the team
    public Participant getWeakestPlayer() {
        if (participantList.isEmpty()) return null;

        Participant weakest = participantList.getFirst();
        for (Participant p : participantList) {
            if (p.getSkillLevel() < weakest.getSkillLevel()) {
                weakest = p;
            }
        }
        return weakest;
    }

    public void swapMember(Participant fromThisTeam, Participant fromOtherTeam, Team other) {
        if (fromThisTeam == null || fromOtherTeam == null || other == null) return;

        // Remove players from their original teams
        this.participantList.remove(fromThisTeam);
        other.participantList.remove(fromOtherTeam);

        // Add them to the opposite teams
        this.addMember(fromOtherTeam);
        other.addMember(fromThisTeam);
    }

    public void printTeams() {
        if (TeamBuilder.teams.isEmpty()){
            System.out.println("Their are no exiting teams that have been created.");
            return;
        }
        for (Team team : TeamBuilder.teams) {
            System.out.println("\n==========================");
            System.out.println(" Team " + team.getTeam_id());
            System.out.println("==========================");

            for (Participant p : team.getParticipantList()) {
                System.out.printf(
                        "Name: %-15s | Role: %-10s | Personality: %-12s | Game: %-10s | Skill: %d%n",
                        p.getName(),
                        p.getPreferredRole(),
                        p.getPersonalityType(),
                        p.getPreferredGame(),
                        p.getSkillLevel()
                );
            }
        }
    }



}
