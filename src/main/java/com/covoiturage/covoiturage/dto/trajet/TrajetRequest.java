package com.covoiturage.covoiturage.dto.trajet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrajetRequest(

        @NotNull(message = "L'identifiant du conducteur est obligatoire")
        Long conducteurId,

        @NotNull(message = "L'identifiant du vehicule est obligatoire")
        Long vehiculeId,

        @Min(value = 1, message = "Il faut proposer au moins 1 place")
        int placesProposees,

        @NotNull(message = "Les points d'arret sont obligatoires")
        @Size(min = 2, message = "Un trajet doit avoir au moins 2 points (depart et arrivee)")
        @Valid
        List<@NotNull(message = "Un point d'arret ne peut pas etre vide") PointArretRequest> pointsArret
) {
}