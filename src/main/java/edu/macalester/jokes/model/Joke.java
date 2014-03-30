package edu.macalester.jokes.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Time;

@Entity
public class Joke {
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;

    @NotNull
    @Size(min = 2)
    private String setup;

    @NotNull
    @Size(min = 2)
    private String punchline;

    private Time createdAt, updatedAt;

    public Long getId() {
        return id;
    }

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
    }

    public String getPunchline() {
        return punchline;
    }

    public void setPunchline(String punchline) {
        this.punchline = punchline;
    }

    public Time getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Time createdAt) {
        this.createdAt = createdAt;
    }

    public Time getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Time updatedAt) {
        this.updatedAt = updatedAt;
    }
}
