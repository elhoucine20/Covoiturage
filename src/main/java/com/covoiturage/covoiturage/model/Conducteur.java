package com.covoiturage.covoiturage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SuperBuilder;


@Entity
@Table(name = "conducteurs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Conducteur extends Personne{

    @Column(name = "numero_permis", nullable = false, unique = true, length = 30)
    private String numeroPermis;

}
