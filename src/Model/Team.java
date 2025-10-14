package Model;

import java.util.List;

public class Team {
     private Integer team_id;
     private final List<Participant> participantList;

    public Team(Integer team_id, List<Participant> participants) {
        this.team_id = team_id;
        this.participantList = participants;
    }

    public Integer getTeam_id() {
        return team_id;
    }

    public void setTeam_id(Integer team_id) {
        this.team_id = team_id;
    }

    public void getParticipantList() {
        System.out.println("Team " + team_id + ":");
        for (Participant p : participantList) System.out.println(p);;
    }

    public void addMember(Participant participant){
        participantList.add(participant);
    }

    public boolean containsParticipant(Participant participant){
        return participantList.contains(participant);
    }

    public Boolean is_balanced(Team team){
        int lengthOfTeam = team.participantList.size();
        return false;
    }

}
