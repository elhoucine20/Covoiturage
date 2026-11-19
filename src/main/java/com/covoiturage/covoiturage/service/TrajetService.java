package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.trajet.TrajetRechercheResponse;
import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TrajetService {

    TrajetResponse publier(TrajetRequest request);
    TrajetResponse trouverParId(Long id);

    // recherche
    Page<TrajetRechercheResponse> rechercher(String villeDepart, String villeArrivee, LocalDate date, BigDecimal prixMax, int page, int size);

}
