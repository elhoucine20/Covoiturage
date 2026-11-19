package com.covoiturage.covoiturage.dto.trajet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrajetRechercheResponse(

        Long trajetId,
        String conducteurName,
        String villeMontee,
        String villeDescente,
        LocalDateTime heureMontee,
        LocalDateTime heureDescente,
        BigDecimal prixSegment,
        Long placesRestantes

) {
}
