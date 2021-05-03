package study.querydsl.entity.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Tree {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "tree")
    private List<Leaf> leaves = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forest_id")
    private Forest forest;

    public void addLeaf(Leaf leaf){
        leaves.add(leaf);
        leaf.setTree(this);
    }

    public Tree(String name) {
        this.name = name;
    }
}
