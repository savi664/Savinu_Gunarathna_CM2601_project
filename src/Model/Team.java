package Model;

import java.util.List;

public class Team {
     private Integer team_id;
     private List<Participant> participants;

    public Team(Integer team_id, List<Participant> participants) {
        this.team_id = team_id;
        this.participants = participants;
    }

    public Integer getTeam_id() {
        return team_id;
    }

    public void setTeam_id(Integer team_id) {
        this.team_id = team_id;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public Boolean is_balanced(){
        return false;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }
}
