package study.querydsl.entity.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Forest {
    @Id
    @GeneratedValue
    @Column(name = "forest_id")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "forest")
    private List<Tree> trees = new ArrayList<>();

    public void addTree(Tree tree){
        trees.add(tree);
        tree.setForest(this);
    }

    public Forest(String name) {
        this.name = name;
    }
}
