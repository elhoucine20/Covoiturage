package com.covoiturage.covoiturage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SuperBuilder;

@MappedSuperclass // ??
@Getter
@Setter
@SuperBuilder
public abstract class Personne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String telephone;

}
