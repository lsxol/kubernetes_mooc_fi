package todobackend;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "todo")
public class Todo extends PanacheEntity {

    public String value;
}
