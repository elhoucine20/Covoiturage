package com.covoiturage.covoiturage.dto.vehicule;

import jakarta.validation.constraints.*;

public record VehiculeRequest(

        @NotBlank(message = "La marque est obligatoire")
        @Size(max = 50, message = "La marque ne doit pas depasser 50 caracteres")
        String marque,

        @NotBlank(message = "Le modele est obligatoire")
        @Size(max = 50, message = "Le modele ne doit pas depasser 50 caracteres")
        String modele,

        @NotBlank(message = "L'immatriculation est obligatoire")
        @Size(max = 20, message = "L'immatriculation ne doit pas depasser 20 caracteres")
        String immatriculation,

        @NotBlank(message = "La couleur est obligatoire")
        @Size(max = 30, message = "La couleur ne doit pas depasser 30 caracteres")
        String couleur,

        @Min(value = 1, message = "La capacite minimale est 1")
        @Max(value = 8, message = "La capacite maximale est 8")
        int capacite,

        @NotNull(message = "L'identifiant du conducteur est obligatoire")
        Long conducteurId
) {
}