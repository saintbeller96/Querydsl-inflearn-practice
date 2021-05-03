package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void setUp(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() throws Exception{
        //member1을 찾아라
        Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        Assertions.assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() throws Exception{
        //QMember m = new QMember("m");
        //QMember m = QMember.member;

        Member result = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() throws Exception{
        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        Assertions.assertThat(result.getUsername()).isEqualTo("member1");
    }
    @Test
    void searchAndParam() throws Exception{
        Member result = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30)
                )
                .fetchOne();
        Assertions.assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() throws Exception{
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> content = results.getResults();


        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 나이 내림차순
     * 2. 이름 올림차순
     * 3. 회원 이름이 없으면 마지막에 출력(null last)
     */

    @Test
    void sort() throws Exception{
        //given
        em.persist(Member.builder().age(100).build());
        em.persist(Member.builder().username("member5").age(100).build());
        em.persist(Member.builder().username("member6").age(100).build());

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        //then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    void paging1() throws Exception{
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        //then
        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() throws Exception{
        //given

        //when
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        //then
        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() throws Exception{
        //given

        //when
        List<Tuple> result = queryFactory
                .select(
                        member.count(), member.age.sum(), member.age.max(), member.age.avg()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        //then

        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     * @throws Exception
     */
    @Test
    void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }


    /**
     * TeamA에 소속된 모든 회원
     * @throws Exception
     */
    @Test
    void join() throws Exception{
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //then
        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * select m , t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    void join_on_filtering() throws Exception{
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        result.forEach(
                t -> {
                    System.out.println("tuple = " + t);
                }
        );
        //then
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * @throws Exception
     */
    @Test
    void join_on_no_relation() throws Exception{
        //given
        em.persist(Member.builder().username("teamA").build());
        em.persist(Member.builder().username("teamB").build());
        em.persist(Member.builder().username("teamC").build());

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        //then
        result.forEach(
                t -> { System.out.println("tuple = " + t);}
        );
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() throws Exception{
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoin() throws Exception{
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }
    @Test
    void fetchJoinTeam() throws Exception{
        //given
        em.flush();
        em.clear();
        //when
        Team findTeam = queryFactory
                .select(team).distinct()
                .from(team)
                .join(team.members)
                .where(team.name.eq("teamA"))
                .fetchOne();

        //then
        findTeam.getMembers().forEach(System.out::println);
        Assertions.assertThat(findTeam.getMembers())
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    void fetchJoinTeams() throws Exception{
        //given
        em.flush();
        em.clear();
        //when
        List<Team> teams = queryFactory
                .select(team).distinct()
                .from(team)
                .join(team.members).fetchJoin()
                .fetch();

        //then
        for(Team team : teams){
            team.getMembers().forEach(System.out::println);
            System.out.println();
        }

    }

    /**
     * 나이가 가장 많은 멤버 조회
     * @throws Exception
     */
    @Test
    void subQuery() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //then
        Assertions.assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 멤버 조회
     * @throws Exception
     */
    @Test
    void subQueryGoe() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        //then
        Assertions.assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * In 예시
     * @throws Exception
     */
    @Test
    void subQueryIn() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();
        //then
        Assertions.assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }
    
    @Test
    void selectSubQuery() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        //then
        result.forEach(
                t -> { System.out.println("tuple = " + t);}
        );
    }
    
    @Test
    void basicCase() throws Exception{
        //given
        
        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        result.forEach(s -> System.out.println("s = " + s));
    }

    @Test
    void complexCase() throws Exception{
        //given

        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("청소년")
                        .when(member.age.between(21, 30)).then("청년")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();
        //then
        result.forEach(s -> System.out.println("s = " + s));
    }

    @Test
    void constant() throws Exception{
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        //then
        result.forEach(s -> System.out.println("s = " + s));
    }

    @Test
    void concat() throws Exception{
        //given

        //when
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findDtoByJPQL() throws Exception{
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findDtoBySetter() throws Exception{
        //given

        //when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findDtoByField() throws Exception{
        //given

        //when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findDtoByConstructor() throws Exception{
        //given

        //when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findUserDtoByField() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")))
                .from(member)
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void findDtoByQueryProjection() throws Exception{
        //given

        //when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //then
        result.forEach(System.out::println);
    }

    @Test
    void dynamicQuery_BooleanBuilder() throws Exception{
        //given
        String usernameParam = "member1";
        Integer ageParam = null;
        //when
        List<Member> result = searchMember1(usernameParam, ageParam);

        //then
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = null;
        //when
        List<Member> result = searchMember2(usernameParam, ageParam);

        //then
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate ageEq(Integer ageCond) {
        if(ageCond == null) return null;
        return member.age.eq(ageCond);
    }

    private Predicate usernameEq(String usernameCond) {
        if(usernameCond == null) return null;
        return member.username.eq(usernameCond);
    }


    //////////////////////////////////////////////////////////////////////

    @Test
    void bulkUpdate() throws Exception{
        //given

        //when
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //then
        /**
         * 벌크 연산은 영속성 컨텍스트와 상관 없이 바로 DB에 쿼리를 날린다.
         * 그래서 영속성 컨텍스트와 DB의 데이터가 서로 달라질 수 있다
         * 만약 이 상태에서 데이터를 조회하는 경우 DB보다 영속성 컨텍스트에 있는 데이터가 우선권을 가진다.
         * 따라서 벌크 연산을 하고나서는 영속성 컨텍스트를 비워두자
         */
        em.flush();
        em.clear();
    }

    @Test
    void bulkAdd() throws Exception{
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() throws Exception{
        queryFactory
                .delete(member)
                .where(member.age.gt(10))
                .execute();
    }

    @Test
    void sqlFunction() throws Exception{
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('regexp_replace', {0}, {1}, {2})",
                        member.username,
                        "member",
                        "M"))
                .from(member)
                .fetch();
        result.forEach(System.out::println);
    }

    @Test
    void sqlFunction2() throws Exception{
        //given

        //when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)
                ))
                .fetch();
        //then
        result.forEach(System.out::println);
    }

}
