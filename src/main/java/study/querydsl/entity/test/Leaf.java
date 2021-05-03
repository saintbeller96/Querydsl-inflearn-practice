package study.querydsl.entity.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Leaf {
    @Id
    @GeneratedValue
    @Column(name = "leaf_id")
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    private Tree tree;


    public Leaf(String name) {
        this.name = name;
    }
}
