package com.covoiturage.covoiturage.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_arret")
@Getter
@Setter
@NoArgsConstructor
public class PointArret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false)
    private int ordre; // 0 = depart

    @Column(name = "heure_passage", nullable = false)
    private LocalDateTime heurePassage;

    @Column(name = "prix_depuis_depart", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixDepuisDepart;
}