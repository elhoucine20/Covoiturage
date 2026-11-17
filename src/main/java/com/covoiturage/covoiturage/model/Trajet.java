package com.covoiturage.covoiturage.model;


import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trajets")
@Getter
@Setter
@NoArgsConstructor
public class Trajet {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // conducteur
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conducteur_id", nullable = false)
    private Conducteur conducteur;

    // vehicule
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;


    @Column(name = "ville_depart", nullable = false, length = 100)
    private String villeDepart;

    @Column(name = "ville_arrivee", nullable = false, length = 100)
    private String villeArrivee;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Column(name = "places_proposees", nullable = false)
    private int placesProposees;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTrajet statut;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<PointArret> pointsArret = new ArrayList<>();

    // deux cotes de la relation
    public void ajouterPointArret(PointArret point) {
        point.setTrajet(this);
        this.pointsArret.add(point);
    }


}
