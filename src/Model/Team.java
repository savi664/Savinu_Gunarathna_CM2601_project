package Model;

import java.util.ArrayList;
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

    public List<Participant> getParticipantList() {
        System.out.println("Team " + team_id + ":");
        List<Participant> participants = new ArrayList<>();
        for (Participant p : participantList) {
            System.out.println(p);
            participants.add(p);
        }
        return participants;
    }

    public void addMember(Participant participant){
        participantList.add(participant);
    }

    public boolean containsParticipant(String Id, Team team){
        List<Participant> participantList = team.getParticipantList();
        for(Participant p: participantList){
            if(p.getId().equalsIgnoreCase(Id)){
                return true;
            }
        }
        return false;
    }

    public Boolean is_balanced(Team team){
        int lengthOfTeam = team.participantList.size();
        return false;
    }

}
