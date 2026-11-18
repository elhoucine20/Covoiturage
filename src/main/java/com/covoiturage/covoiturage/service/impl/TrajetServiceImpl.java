package com.covoiturage.covoiturage.service.impl;


import com.covoiturage.covoiturage.dto.trajet.PointArretRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.trajet.TrajetMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.PointArret;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.model.Vehicule;
import com.covoiturage.covoiturage.model.enums.StatutReservation;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.repository.ReservationRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.repository.VehiculeRepository;
import com.covoiturage.covoiturage.service.TrajetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final ConducteurRepository conducteurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final TrajetMapper trajetMapper;

    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public TrajetResponse publier(TrajetRequest request) {
        Conducteur conducteur = chercherConducteur(request.conducteurId());
        Vehicule vehicule = chercherVehicule(request.vehiculeId());

        // 403
        if (!vehicule.getConducteur().getId().equals(conducteur.getId())){
            throw new ForbiddenActionException("Le vehicule " + vehicule.getId() + " n'appartient pas au conducteur " + conducteur.getId());
        }

        // 400 places proposees de 1 a la capacite du vehicule
        if (request.placesProposees() > vehicule.getCapacite()){
            throw new InvalidRequestException("Les places proposees ne peuvent pas depasser la capacite du vehicule (" + vehicule.getCapacite() + ")");
        }

        List<PointArretRequest> pointes = request.pointsArret();
        validerParcours(pointes);
        PointArretRequest premier = pointes.get(0);
        PointArretRequest dernier = pointes.get(pointes.size()-1);
        Trajet trajet = new Trajet();
        trajet.setConducteur(conducteur);
        trajet.setVehicule(vehicule);
        trajet.setVilleDepart(premier.ville().trim());
        trajet.setVilleArrivee(dernier.ville().trim());
        trajet.setDateDepart(premier.heurePassage());
        trajet.setPlacesProposees(request.placesProposees());
        trajet.setStatut(StatutTrajet.PUBLIE);

        // L'ordre d'un point depend a sa position dans la list (0 = depart)
        for (int i = 0; i < pointes.size(); i++) {
            trajet.ajouterPointArret(construirePointArret(pointes.get(i), i));
        }

        Trajet sauvegarde = trajetRepository.save(trajet);
        log.info("Trajet publie : id={}, {} -> {}", sauvegarde.getId(),
                sauvegarde.getVilleDepart(), sauvegarde.getVilleArrivee());
        return trajetMapper.toResponse(sauvegarde, calculerPlacesRestantes(sauvegarde));
    }

    @Override
    public TrajetResponse trouverParId(Long id) {
        Trajet trajet = trajetRepository.findDetailById(id).orElseThrow(() ->
                new ResourceNotFoundException("Trajet introuvable avec l'identifiant " + id));
        return trajetMapper.toResponse(trajet, calculerPlacesRestantes(trajet));
    }


    // helpers

    private void validerParcours(List<PointArretRequest> points) {
        PointArretRequest premier = points.get(0);

        if (!premier.heurePassage().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Le depart doit etre dans le futur");
        }
        //
        if (premier.prixDepuisDepart().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidRequestException("Le prix du point de depart doit etre 0");
        }

        // Set.add(...) renvoie false si l'element existe deja : pratique pour detecter les doublons
        Set<String> villesVues = new HashSet<>();

        for (int i = 0; i < points.size(); i++) {
            PointArretRequest courant = points.get(i);

            String villeNormalisee = courant.ville().trim().toLowerCase(Locale.ROOT);
            if (!villesVues.add(villeNormalisee)) {
                throw new InvalidRequestException("La ville " + courant.ville().trim() + " apparait plusieurs fois sur le parcours");
            }

            if (i > 0) {
                PointArretRequest precedent = points.get(i - 1);
                if (!courant.heurePassage().isAfter(precedent.heurePassage())) {
                    throw new InvalidRequestException("Les heures de passage doivent etre strictement croissantes");
                }
                if (courant.prixDepuisDepart().compareTo(precedent.prixDepuisDepart()) <= 0) {
                    throw new InvalidRequestException("Les prix depuis le depart doivent etre strictement croissants");
                }
            }
        }
    }

    // Transforme un point recu (DTO) en entite, avec son ordre
    private PointArret construirePointArret(PointArretRequest request, int ordre) {
        PointArret point = new PointArret();
        point.setVille(request.ville().trim());
        point.setOrdre(ordre);
        point.setHeurePassage(request.heurePassage());

        point.setPrixDepuisDepart(request.prixDepuisDepart().setScale(2, RoundingMode.HALF_UP));
        return point;
    }

    // les places restantes
    private int calculerPlacesRestantes(Trajet trajet) {
        Long reservees = reservationRepository.sommePlacesParTrajetEtStatut(trajet.getId(), StatutReservation.CONFIRMEE);
        return Math.toIntExact(trajet.getPlacesProposees() - reservees);
    }

    private Conducteur chercherConducteur(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur introuvable avec l'identifiant " + id));
    }

    private Vehicule chercherVehicule(Long id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule introuvable avec l'identifiant " + id));
    }
}

