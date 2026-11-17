package com.covoiturage.covoiturage.dto.trajet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PointArretResponse(
        Long id,
        String ville,
        int ordre,
        LocalDateTime heurePassage,
        BigDecimal prixDepuisDepart
) {
}