package com.covoiturage.covoiturage.dto.trajet;

import com.covoiturage.covoiturage.dto.conducteur.ConducteurResponse;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.List;

public record TrajetResponse(
        Long id,
        ConducteurResponse conducteur,
        VehiculeResponse vehicule,
        String villeDepart,
        String villeArrivee,
        LocalDateTime dateDepart,
        int placesProposees,
        int placesRestantes,
        StatutTrajet statut,
        List<PointArretResponse> pointsArret
) {
}