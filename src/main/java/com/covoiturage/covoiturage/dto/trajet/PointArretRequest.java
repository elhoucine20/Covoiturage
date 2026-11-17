package com.covoiturage.covoiturage.dto.trajet;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PointArretRequest(

        @NotBlank(message = "La ville est obligatoire")
        @Size(max = 100, message = "La ville ne doit pas depasser 100 caracteres")
        String ville,

        @NotNull(message = "L'heure de passage est obligatoire")
        LocalDateTime heurePassage,

        @NotNull(message = "Le prix depuis le depart est obligatoire")
        @PositiveOrZero(message = "Le prix ne peut pas etre negatif")
        // refuse plus de 2 decimales
        @Digits(integer = 8, fraction = 2, message = "Le prix doit avoir au maximum 2 decimales")
        BigDecimal prixDepuisDepart
) {
}