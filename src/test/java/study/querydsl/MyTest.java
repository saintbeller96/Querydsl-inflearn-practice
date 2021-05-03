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
import study.querydsl.entity.Team;
import study.querydsl.entity.test.Forest;
import study.querydsl.entity.test.Leaf;
import study.querydsl.entity.test.QTree;
import study.querydsl.entity.test.Tree;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;
import static study.querydsl.entity.test.QTree.tree;

@SpringBootTest
@Transactional
public class MyTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void setUp() {
        queryFactory = new JPAQueryFactory(em);
        Forest forestA = new Forest("forestA");
        Forest forestB = new Forest("forestB");
        em.persist(forestA);
        em.persist(forestB);

        for(int i = 0; i<4; i++){
            Tree tree = new Tree("tree"+(i+1));
            em.persist(tree);
            if(i%2 == 0) forestA.addTree(tree);
            else forestB.addTree(tree);

            for(int j = 0; j<6; j++){
                Leaf leaf = new Leaf("leaf"+i+j);
                em.persist(leaf);
                tree.addLeaf(leaf);
            }
        }
    }

    @Test
    void test() throws Exception{
        List<Tree> fetch = queryFactory
                .selectFrom(tree).distinct()
                .join(tree.forest).fetchJoin()
                .join(tree.leaves).fetchJoin()
                .fetch();

        for(Tree t : fetch){
            System.out.println(t.getForest());
            t.getLeaves().forEach(System.out::println);
            System.out.println();
        }
    }

}