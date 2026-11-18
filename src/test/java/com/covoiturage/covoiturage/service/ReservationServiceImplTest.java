package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.reservation.ReservationRequest;
import com.covoiturage.covoiturage.dto.reservation.ReservationResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.reservation.ReservationMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.Passager;
import com.covoiturage.covoiturage.model.PointArret;
import com.covoiturage.covoiturage.model.Reservation;
import com.covoiturage.covoiturage.model.enums.StatutReservation;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.repository.PassagerRepository;
import com.covoiturage.covoiturage.repository.ReservationRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PassagerRepository passagerRepository;
    @Mock
    private TrajetRepository trajetRepository;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private final LocalDateTime depart = LocalDateTime.now().plusDays(3);

    private Passager salma;
    private Trajet trajet;

    @BeforeEach
    void setUp() {
        salma = passager(1L);
        trajet = trajet(StatutTrajet.PUBLIE, 3, depart);
    }

    // helpers
    private Conducteur conducteur(Long id) {
        Conducteur c = new Conducteur();
        c.setId(id);
        return c;
    }

    private Passager passager(Long id) {
        Passager p = new Passager();
        p.setId(id);
        return p;
    }

    private PointArret point(Long id, int ordre, String ville, String prix) {
        PointArret p = new PointArret();
        p.setId(id);
        p.setOrdre(ordre);
        p.setVille(ville);
        p.setPrixDepuisDepart(new BigDecimal(prix));
        return p;
    }

    private Trajet trajet(StatutTrajet statut, int placesProposees, LocalDateTime dateDepart) {
        Trajet t = new Trajet();
        t.setId(5L);
        t.setConducteur(conducteur(1L));
        t.setStatut(statut);
        t.setPlacesProposees(placesProposees);
        t.setDateDepart(dateDepart);
        //chaque point a son trajet
        t.ajouterPointArret(point(101L, 0, "Rabat", "0"));
        t.ajouterPointArret(point(102L, 1, "Casablanca", "40"));
        t.ajouterPointArret(point(103L, 2, "Settat", "70"));
        t.ajouterPointArret(point(104L, 3, "Marrakech", "120"));
        return t;
    }

    private Reservation reservation(Passager proprietaire, Trajet sonTrajet, StatutReservation statut) {
        Reservation r = new Reservation();
        r.setId(20L);
        r.setPassager(proprietaire);
        r.setTrajet(sonTrajet);
        r.setStatut(statut);
        return r;
    }

    private ReservationRequest requete(Long pointMonteeId, Long pointDescenteId, int places) {
        return new ReservationRequest(1L, 5L, pointMonteeId, pointDescenteId, places);
    }

    // Programme les deux recherches communes a presque tous les tests de "reserver" :
    // le passager 1 et le trajet 5 (charge avec le VERROU : findByIdForUpdate)
    private void preparerPassagerEtTrajet() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(salma));
        when(trajetRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(trajet));
    }

    // Programme la sauvegarde : save() renvoie l'objet recu, le mapper renvoie une reponse bidon
    private ReservationResponse preparerSauvegarde() {
        ReservationResponse reponse = new ReservationResponse(20L, 1L, 5L, "Settat", "Marrakech",
                1, new BigDecimal("50.00"), LocalDateTime.now(), StatutReservation.CONFIRMEE);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationMapper.toResponse(any(Reservation.class))).thenReturn(reponse);
        return reponse;
    }

    // Programme les 3 lectures de "annuler" : id du trajet, trajet verrouille, reservation
    private void preparerAnnulation(Reservation reservation) {
        when(reservationRepository.findTrajetIdById(20L)).thenReturn(Optional.of(5L));
        when(trajetRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.findById(20L)).thenReturn(Optional.of(reservation));
    }

    // ---------- reserver : cas nominaux (scenarios 1 et 3 du brief) ----------

    // Scenario 1 : Salma reserve 1 place Settat -> Marrakech : 120 - 70 = 50 DH, le trajet reste PUBLIE
    @Test
    void reserver_nominal_calculeLeMontantEtLaisseLeTrajetPublie() {
        preparerPassagerEtTrajet();
        ReservationResponse reponse = preparerSauvegarde();
        when(reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(1L, 5L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sommePlacesParTrajetEtStatut(5L, StatutReservation.CONFIRMEE))
                .thenReturn(0L); // aucune place reservee pour l'instant -> 3 restantes

        ReservationResponse resultat = reservationService.reserver(requete(103L, 104L, 1));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation sauvegarde = captor.getValue();

        // BigDecimal.equals tient compte de l'echelle : "50.00" verifie bien les 2 decimales du brief
        assertThat(sauvegarde.getMontantTotal()).isEqualTo(new BigDecimal("50.00"));
        assertThat(sauvegarde.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(sauvegarde.getNombrePlaces()).isEqualTo(1);
        assertThat(sauvegarde.getDateReservation()).isNotNull();
        assertThat(sauvegarde.getPassager()).isSameAs(salma);
        assertThat(sauvegarde.getTrajet()).isSameAs(trajet);
        assertThat(sauvegarde.getPointMontee().getId()).isEqualTo(103L);
        assertThat(sauvegarde.getPointDescente().getId()).isEqualTo(104L);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE); // 2 places restantes

        // Concurrence : on doit utiliser la lecture VERROUILLEE, pas le findById classique
        verify(trajetRepository).findByIdForUpdate(5L);
        verify(trajetRepository, never()).findById(any());

        assertThat(resultat).isEqualTo(reponse);
    }

    // Scenario 3 : Karim reserve 2 places Casablanca -> Marrakech : (120 - 40) x 2 = 160 DH, le trajet devient COMPLET
    @Test
    void reserver_derniereplace_trajetDevientComplet() {
        preparerPassagerEtTrajet();
        preparerSauvegarde();
        when(reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(1L, 5L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sommePlacesParTrajetEtStatut(5L, StatutReservation.CONFIRMEE))
                .thenReturn(1L); // Salma a deja pris 1 place -> il en reste 2

        reservationService.reserver(requete(102L, 104L, 2));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getMontantTotal()).isEqualTo(new BigDecimal("160.00"));
        // 2 restantes - 2 reservees = 0 : transition automatique PUBLIE -> COMPLET
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.COMPLET);
    }

    // ---------- reserver : regles refusees ----------

    @Test
    void reserver_passagerInexistant_lanceResourceNotFoundException() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 104L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Passager");

        verifyNoInteractions(trajetRepository); // le service s'arrete avant de chercher le trajet
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserver_trajetInexistant_lanceResourceNotFoundException() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(salma));
        when(trajetRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 104L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trajet");

        verify(reservationRepository, never()).save(any());
    }

    // @ParameterizedTest + @EnumSource : le MEME test est execute une fois par statut liste.
    // Scenario 4 du brief (trajet COMPLET -> 409) + les statuts TERMINE et ANNULE : aucun ne permet de reserver.
    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"COMPLET", "TERMINE", "ANNULE"})
    void reserver_trajetNonPublie_lanceBusinessRuleException(StatutTrajet statut) {
        trajet.setStatut(statut);
        preparerPassagerEtTrajet();

        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 104L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(statut.name());

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserver_pointHorsDuTrajet_lanceInvalidRequestException() {
        preparerPassagerEtTrajet();

        // 999 n'est pas un point de ce trajet (c'est par exemple un point d'un autre trajet)
        assertThatThrownBy(() -> reservationService.reserver(requete(999L, 104L, 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("n'appartient pas au trajet");

        verify(reservationRepository, never()).save(any());
    }

    // Scenario 6 : Imane monte a Marrakech et descend a Settat -> 400
    @Test
    void reserver_monteeApresDescente_lanceInvalidRequestException() {
        preparerPassagerEtTrajet();

        assertThatThrownBy(() -> reservationService.reserver(requete(104L, 103L, 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("avant le point de descente");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserver_monteeEgaleDescente_lanceInvalidRequestException() {
        preparerPassagerEtTrajet();

        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 103L, 1)))
                .isInstanceOf(InvalidRequestException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserver_passagerADejaUneReservationConfirmee_lanceBusinessRuleException() {
        preparerPassagerEtTrajet();
        when(reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(1L, 5L, StatutReservation.CONFIRMEE))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 104L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deja une reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserver_placesInsuffisantes_lanceBusinessRuleException() {
        preparerPassagerEtTrajet();
        when(reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(1L, 5L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sommePlacesParTrajetEtStatut(5L, StatutReservation.CONFIRMEE))
                .thenReturn(2L); // 3 proposees - 2 reservees = 1 restante

        // on demande 2 places alors qu'il n'en reste qu'une
        assertThatThrownBy(() -> reservationService.reserver(requete(103L, 104L, 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("il reste 1 place");

        verify(reservationRepository, never()).save(any());
    }

    // ---------- annuler ----------

    // Scenario 5 : Karim annule 3 jours avant le depart -> ANNULEE
    @Test
    void annuler_nominal_passeLaReservationAAnnulee() {
        Reservation reservation = reservation(salma, trajet, StatutReservation.CONFIRMEE);
        preparerAnnulation(reservation);

        reservationService.annuler(20L, 1L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE); // il etait deja PUBLIE : inchange
    }

    // Scenario 5 (suite) : le trajet COMPLET redevient PUBLIE car une place se libere
    @Test
    void annuler_trajetComplet_leTrajetRedevientPublie() {
        trajet.setStatut(StatutTrajet.COMPLET);
        Reservation reservation = reservation(salma, trajet, StatutReservation.CONFIRMEE);
        preparerAnnulation(reservation);

        reservationService.annuler(20L, 1L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
    }

    @Test
    void annuler_reservationDunAutrePassager_lanceForbiddenActionException() {
        Reservation reservation = reservation(passager(2L), trajet, StatutReservation.CONFIRMEE);
        preparerAnnulation(reservation);

        // le passager 1 essaie d'annuler la reservation du passager 2
        assertThatThrownBy(() -> reservationService.annuler(20L, 1L))
                .isInstanceOf(ForbiddenActionException.class);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE); // rien n'a change
    }

    @Test
    void annuler_reservationDejaAnnulee_lanceBusinessRuleException() {
        Reservation reservation = reservation(salma, trajet, StatutReservation.ANNULEE);
        preparerAnnulation(reservation);

        assertThatThrownBy(() -> reservationService.annuler(20L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deja annulee");
    }

    // Scenario 10 : Salma annule 2 heures avant le depart -> 409 (delai de 24 h depasse)
    @Test
    void annuler_moinsDe24hAvantLeDepart_lanceBusinessRuleException() {
        trajet.setDateDepart(LocalDateTime.now().plusHours(2));
        Reservation reservation = reservation(salma, trajet, StatutReservation.CONFIRMEE);
        preparerAnnulation(reservation);

        assertThatThrownBy(() -> reservationService.annuler(20L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("24 h");

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE); // rien n'a change
    }

    @Test
    void annuler_reservationInexistante_lanceResourceNotFoundException() {
        when(reservationRepository.findTrajetIdById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.annuler(20L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("20");

        verifyNoInteractions(trajetRepository); // pas de trajet a verrouiller
    }

    // ---------- consultation ----------

    @Test
    void listerParPassager_nominal_renvoieSesReservations() {
        Reservation reservation = reservation(salma, trajet, StatutReservation.CONFIRMEE);
        ReservationResponse reponse = new ReservationResponse(20L, 1L, 5L, "Settat", "Marrakech",
                1, new BigDecimal("50.00"), LocalDateTime.now(), StatutReservation.CONFIRMEE);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(salma));
        when(reservationRepository.findByPassagerIdAvecDetails(1L)).thenReturn(List.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(reponse);

        List<ReservationResponse> resultat = reservationService.listerParPassager(1L);

        assertThat(resultat).containsExactly(reponse);
    }

    @Test
    void listerParPassager_passagerInexistant_lanceResourceNotFoundException() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.listerParPassager(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void listerParTrajet_nominal_leProprietaireVoitLesReservations() {
        Reservation reservation = reservation(salma, trajet, StatutReservation.CONFIRMEE);
        ReservationResponse reponse = new ReservationResponse(20L, 1L, 5L, "Settat", "Marrakech",
                1, new BigDecimal("50.00"), LocalDateTime.now(), StatutReservation.CONFIRMEE);
        when(trajetRepository.findById(5L)).thenReturn(Optional.of(trajet)); // trajet du conducteur 1
        when(reservationRepository.findByTrajetIdAvecDetails(5L)).thenReturn(List.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(reponse);

        List<ReservationResponse> resultat = reservationService.listerParTrajet(5L, 1L);

        assertThat(resultat).containsExactly(reponse);
    }

    // Un conducteur ne consulte que les reservations de SES trajets : le trajet 5 appartient au conducteur 1
    @Test
    void listerParTrajet_autreConducteur_lanceForbiddenActionException() {
        when(trajetRepository.findById(5L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.listerParTrajet(5L, 2L))
                .isInstanceOf(ForbiddenActionException.class);

        verifyNoInteractions(reservationRepository); // aucune reservation n'a ete lue
    }

    @Test
    void listerParTrajet_trajetInexistant_lanceResourceNotFoundException() {
        when(trajetRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.listerParTrajet(5L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}