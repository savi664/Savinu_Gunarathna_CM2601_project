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

    public void removeMember(Participant p) {
        participantList.remove(p);
    }

    public List<Participant> getParticipantList() {
        return participantList;
    }

    public void addMember(Participant participant){
        participantList.add(participant);
    }

    public Participant containsParticipant(String Id) {
        for (Participant p : participantList) {
            if (p.getId().equalsIgnoreCase(Id)) {
                return p;
            }
        }
        return null; // Return -1 if participant is not found
    }

    public int CalculateAvgSkill() {
        if (participantList.isEmpty()) {
            return 0; // Return 0 for empty teams
        }
        int total = 0;
        for (Participant p : participantList) {
            total += p.getSkillLevel();
        }
        return total / participantList.size();
    }


}
