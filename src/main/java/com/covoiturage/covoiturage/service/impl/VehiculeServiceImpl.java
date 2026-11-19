package com.covoiturage.covoiturage.service.impl;


import com.covoiturage.covoiturage.dto.vehicule.VehiculeRequest;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.vehicule.VehiculeMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.Vehicule;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.repository.VehiculeRepository;
import com.covoiturage.covoiturage.service.VehiculeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class VehiculeServiceImpl implements VehiculeService{

    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final VehiculeMapper vehiculeMapper;

    private final TrajetRepository trajetRepository;

    @Override
    @Transactional
    public VehiculeResponse ajouter(VehiculeRequest request) {

        Conducteur conducteur = chercherConducteur(request.conducteurId());
        String immatriculation = normaliserImmatriculation(request.immatriculation());
        if (vehiculeRepository.existsByImmatriculationIgnoreCase(immatriculation)){
            throw new BusinessRuleException("Un vehicule avec l'immatriculation " + immatriculation + " existe deja!!");
        }
        Vehicule vehicule = vehiculeMapper.toEntity(request);
        vehicule.setImmatriculation(immatriculation);
        vehicule.setConducteur(conducteur);

        Vehicule sauvegarde = vehiculeRepository.save(vehicule);
        log.info("Vehicule cree : id={}", sauvegarde.getId());
        return vehiculeMapper.toResponse(sauvegarde);
    }

    @Override
    public VehiculeResponse trouverParId(Long id) {
        return vehiculeMapper.toResponse(chercher(id));
    }

    @Override
    public List<VehiculeResponse> listerParConducteur(Long conducteurId) {
        chercherConducteur(conducteurId);
        return vehiculeRepository.findByConducteurId(conducteurId).stream().map(vehiculeMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public VehiculeResponse modifier(Long id, VehiculeRequest request) {
        Vehicule vehicule = chercher(id);
        verifierProprietaire(vehicule, request.conducteurId());
        String immatriculation = normaliserImmatriculation(request.immatriculation());

        if (vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(immatriculation, id)) {
            throw new BusinessRuleException("Un autre vehicule utilise deja l'immatriculation " + immatriculation + "!!");
        }

        vehiculeMapper.updateEntity(request, vehicule);
        vehicule.setImmatriculation(immatriculation);

        Vehicule sauvegarde = vehiculeRepository.save(vehicule);
        log.info("Vehicule modifie : id={}", sauvegarde.getId());
        return vehiculeMapper.toResponse(sauvegarde);
    }

    @Override
    @Transactional
    public void supprimer(Long id, Long conducteurId) {
        Vehicule vehicule = chercher(id);
        verifierProprietaire(vehicule, conducteurId);
        // si le vehicule exist dans un trajet
        if (trajetRepository.existsByVehiculeId(id)) {
            throw new BusinessRuleException("Suppression impossible le vehicule est associe a un trajet");
        }
        vehiculeRepository.delete(vehicule);
        log.info("Vehicule supprime : id={}", id);
    }

    // helpers
    private String normaliserImmatriculation(String immatriculation) {
        return immatriculation.trim().toUpperCase(Locale.ROOT);
    }

    private Vehicule chercher(Long id) {
        return vehiculeRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Vehicule introuvable avec l'identifiant " + id));
    }

    private Conducteur chercherConducteur(Long id) {
        return conducteurRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Conducteur introuvable avec l'identifiant " + id));
    }

    // Le conducteur qui agit doit etre le proprietaire, sinon 403
    private void verifierProprietaire(Vehicule vehicule, Long conducteurId) {
        if (!vehicule.getConducteur().getId().equals(conducteurId)) {
            throw new ForbiddenActionException("Ce vehicule n'appartient pas au conducteur " + conducteurId);
        }
    }

}
