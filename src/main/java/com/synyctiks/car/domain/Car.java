package com.synyctiks.car.domain;

import java.io.Serializable;
import javax.persistence.*;

/**
 * A Car.
 */
@Entity
@Table(name = "car")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Car implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "colour")
    private String colour;

    @Column(name = "price")
    private Integer price;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Car id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Car name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColour() {
        return this.colour;
    }

    public Car colour(String colour) {
        this.setColour(colour);
        return this;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Integer getPrice() {
        return this.price;
    }

    public Car price(Integer price) {
        this.setPrice(price);
        return this;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Car)) {
            return false;
        }
        return id != null && id.equals(((Car) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Car{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", colour='" + getColour() + "'" +
            ", price=" + getPrice() +
            "}";
    }
}
