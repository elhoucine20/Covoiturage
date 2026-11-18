package com.covoiturage.covoiturage.service.impl;

import com.covoiturage.covoiturage.dto.reservation.ReservationRequest;
import com.covoiturage.covoiturage.dto.reservation.ReservationResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.reservation.ReservationMapper;
import com.covoiturage.covoiturage.model.Passager;
import com.covoiturage.covoiturage.model.PointArret;
import com.covoiturage.covoiturage.model.Reservation;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.model.enums.StatutReservation;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.repository.PassagerRepository;
import com.covoiturage.covoiturage.repository.ReservationRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    // passager annule jusque a 24 heures avant le depart
    private static final long DELAI_ANNULATION_HEURES = 24;

    private final ReservationRepository reservationRepository;
    private final PassagerRepository passagerRepository;
    private final TrajetRepository trajetRepository;
    private final ReservationMapper reservationMapper;


    @Override
    @Transactional
    public ReservationResponse reserver(ReservationRequest request) {
        Passager passager = chercherPassager(request.passagerId());
        // autre person ne peut reserver ce trajet avant la fin de la transaction
        Trajet trajet = chercherTrajetVerrouille(request.trajetId());

        // ne peut pas reserve sur un trajet PUBLIE
        if (trajet.getStatut() != StatutTrajet.PUBLIE){
            throw new BusinessRuleException("Reservation impossible,le trajet est " + trajet.getStatut());
        }
        // point de depart et d'arrive
        PointArret montee = chercherPoint(trajet, request.pointMonteeId());
        PointArret descente = chercherPoint(trajet, request.pointDescenteId());

        // la montee doit etre avant la descente
        if (montee.getOrdre() >= descente.getOrdre()){
            throw new InvalidRequestException("Le point de montee doit etre avant le point de descente");
        }

        // une seule reservation CONFIRMEE par passager et par trajet
        if (reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(passager.getId(), trajet.getId(), StatutReservation.CONFIRMEE)){
            throw new BusinessRuleException("Le passager a deja une reservation confirmee sur ce trajet");
        }

        //le nombre de places reservees ne depasse jamais les places proposees
        int placesRestantes = calculerPlacesRestantes(trajet);
        if (request.nombrePlaces() > placesRestantes){
            throw new BusinessRuleException("impossible de reserver ces places, il reste " + placesRestantes + " place(s)");
        }

        Reservation reservation = new Reservation();
        reservation.setPassager(passager);
        reservation.setTrajet(trajet);
        reservation.setPointMontee(montee);
        reservation.setPointDescente(descente);
        reservation.setNombrePlaces(request.nombrePlaces());
        reservation.setMontantTotal(calculerMontant(montee, descente, request.nombrePlaces()));
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setStatut(StatutReservation.CONFIRMEE);

        Reservation sauvegarde = reservationRepository.save(reservation);
        //le trajet va COMPLET quand la derniere place est prise
        if (placesRestantes - request.nombrePlaces() == 0){
            trajet.setStatut(StatutTrajet.COMPLET);
            log.info("Trajet {} devenu COMPLET", trajet.getId());
        }
        log.info("Reservation creee : id={}, trajet={}, montant={}",
                sauvegarde.getId(), trajet.getId(), sauvegarde.getMontantTotal());
        return reservationMapper.toResponse(sauvegarde);
    }

    @Override
    @Transactional
    public void annuler(Long reservationId, Long passagerId) {

        //
        Long trajetId = reservationRepository.findTrajetIdById(reservationId).orElseThrow(()->
                new ResourceNotFoundException("Reservation introuvable avec l'identifiant " + reservationId));

        Trajet trajet = chercherTrajetVerrouille(trajetId); // find trajet et reservation
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(()->
                new ResourceNotFoundException("Reservation introuvable avec l'identifiant "+reservationId));

        // autre passager ne peut pas annuler cette reservation
        if (!reservation.getPassager().getId().equals(passagerId)){
            throw new ForbiddenActionException("Cette reservation n'appartient pas a passager " + passagerId);
        }

        // lareservation est deja annuler
        if (reservation.getStatut() == StatutReservation.ANNULEE){
            throw new BusinessRuleException("La reservation est deja annulee");
        }

        // delai de 24h deppaser
        LocalDateTime limite = trajet.getDateDepart().minusHours(DELAI_ANNULATION_HEURES);
        if (LocalDateTime.now().isAfter(limite)){
            throw new BusinessRuleException("Annulation impossible le delai de 24 h avant le depart est depasse");
        }
        // annuluer
        reservation.setStatut(StatutReservation.ANNULEE);

        // trajet publie
        if (trajet.getStatut() == StatutTrajet.COMPLET){
            trajet.setStatut(StatutTrajet.PUBLIE);
            log.info("Trajet {} Redevenu PUBLIE ",trajet.getId());
        }
        log.info("Reservation Annuler id={} ",reservationId);
    }

    @Override
    public List<ReservationResponse> listerParPassager(Long passagerId) {
        chercherPassager(passagerId); // if exist
        return reservationRepository.findByPassagerIdAvecDetails(passagerId).stream().map(reservationMapper::toResponse).toList();
    }

    @Override
    public List<ReservationResponse> listerParTrajet(Long trajetId, Long conducteurId) {
        Trajet trajet = trajetRepository.findById(trajetId).orElseThrow(()->
                new ResourceNotFoundException("Trajet Introuvable avec l'identifaint "+trajetId));
        // validation de conducteur
        if (!trajet.getConducteur().getId().equals(conducteurId)){
            throw new ForbiddenActionException("Ce trajet n'appartient pas a conducteur "+conducteurId);
        }
        return reservationRepository.findByTrajetIdAvecDetails(trajetId).stream().map(reservationMapper::toResponse).toList();
    }

    // helpers
    // calcule le montant
    private BigDecimal calculerMontant(PointArret montee, PointArret descente, int nombrePlaces) {
        return descente.getPrixDepuisDepart()
                .subtract(montee.getPrixDepuisDepart())
                .multiply(BigDecimal.valueOf(nombrePlaces))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // places restantes
    private int calculerPlacesRestantes(Trajet trajet) {
        long reservees = reservationRepository.sommePlacesParTrajetEtStatut(
                trajet.getId(), StatutReservation.CONFIRMEE);
        return trajet.getPlacesProposees() - (int) reservees;
    }

    // Chercher un point dans les points du trajet
    private PointArret chercherPoint(Trajet trajet, Long pointId) {
        return trajet.getPointsArret().stream()
                .filter(p -> p.getId().equals(pointId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Le point d'arret " + pointId + " n'appartient pas au trajet " + trajet.getId()));
    }

    private Trajet chercherTrajetVerrouille(Long id) {
        return trajetRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'identifiant " + id));
    }

    private Passager chercherPassager(Long id) {
        return passagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passager introuvable avec l'identifiant " + id));
    }

}
