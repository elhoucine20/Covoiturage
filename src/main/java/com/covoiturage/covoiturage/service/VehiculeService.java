package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.vehicule.VehiculeRequest;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;

import java.util.List;

public interface VehiculeService {

    VehiculeResponse ajouter(VehiculeRequest request);
    VehiculeResponse trouverParId(Long id);
    List<VehiculeResponse> listerParConducteur(Long conducteurId);
    VehiculeResponse modifier(Long id, VehiculeRequest request);
    void supprimer(Long id, Long conducteurId);

}
