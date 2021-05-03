package study.querydsl.dto;

import lombok.Builder;
import lombok.Data;

@Data
public class MemberSearchCondition {
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

    public MemberSearchCondition(){}

    @Builder
    public MemberSearchCondition(String username, String teamName, Integer ageGoe, Integer ageLoe) {
        this.username = username;
        this.teamName = teamName;
        this.ageGoe = ageGoe;
        this.ageLoe = ageLoe;
    }
}
