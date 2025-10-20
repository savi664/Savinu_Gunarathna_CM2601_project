package Model;

public class Participant {

    private String id;
    private String email;
    private String name;
    private String preferredGame;
    private Integer skillLevel;
    private RoleType preferredRole;
    private Integer personalityScore;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private PersonalityType personalityType;

    public Participant(String id,String name, String email, String preferredGame, Integer skillLevel, RoleType preferredRole, Integer personalityScore, PersonalityType personalityType) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreferredGame() {
        return preferredGame;
    }

    public void setPreferredGame(String preferredGame) {
        this.preferredGame = preferredGame;
    }

    public Integer getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(Integer skillLevel) {
        this.skillLevel = skillLevel;
    }

    public RoleType getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(RoleType preferredRole) {
        this.preferredRole = preferredRole;
    }

    public Integer getPersonalityScore() {
        return personalityScore;
    }

    public void setPersonalityScore(Integer personalityScore) {
        this.personalityScore = personalityScore;
    }

    public PersonalityType getPersonalityType() {
        return personalityType;
    }

    public void setPersonalityType(PersonalityType personalityType) {
        this.personalityType = personalityType;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", preferredGame='" + preferredGame + '\'' +
                ", skillLevel=" + skillLevel +
                ", preferredRole=" + preferredRole +
                ", personalityScore=" + personalityScore +
                ", personalityType=" + personalityType +
                '}';
    }
}
