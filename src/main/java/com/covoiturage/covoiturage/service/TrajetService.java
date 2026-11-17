package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;

public interface TrajetService {

    TrajetResponse publier(TrajetRequest request);
    TrajetResponse trouverParId(Long id);

}
