package pingpong.counter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "counter")
public class Counter extends PanacheEntityBase {

    @Id
    public Long id;

    public Long value;
}
