package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.trajet.PointArretRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetRechercheResponse;
import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.trajet.TrajetMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.PointArret;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.model.Vehicule;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.repository.ReservationRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.repository.VehiculeRepository;
import com.covoiturage.covoiturage.service.impl.TrajetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrajetServiceImplTest {

    @Mock
    private TrajetRepository trajetRepository;
    @Mock
    private ConducteurRepository conducteurRepository;
    @Mock
    private VehiculeRepository vehiculeRepository;
    @Mock
    private TrajetMapper trajetMapper;

    @Mock
    private TrajetRechercheResponse trajetRechercheResponse;

    @Mock
    private ReservationRepository reservationRepository;


    @InjectMocks
    private TrajetServiceImpl trajetService;

    // Date de depart du trajet
    private final LocalDateTime depart = LocalDateTime.now().plusDays(5);

    private Conducteur conducteur;
    private Vehicule vehicule; // capacite 4, appartient a "conducteur"

    @BeforeEach
    void setUp() {
        conducteur = conducteur(1L);
        vehicule = vehicule(10L, conducteur, 4);
    }

    // helpers

    private Conducteur conducteur(Long id) {
        Conducteur c = new Conducteur();
        c.setId(id);
        return c;
    }

    private Vehicule vehicule(Long id, Conducteur proprietaire, int capacite) {
        Vehicule v = new Vehicule();
        v.setId(id);
        v.setConducteur(proprietaire);
        v.setCapacite(capacite);
        return v;
    }

    private PointArretRequest point(String ville, LocalDateTime heure, String prix) {
        return new PointArretRequest(ville, heure, new BigDecimal(prix));
    }

    private List<PointArretRequest> pointsValides() {
        List<PointArretRequest> points = new ArrayList<>();
        points.add(point("Rabat", depart, "0"));
        points.add(point("Casablanca", depart.plusMinutes(75), "40"));
        points.add(point("Settat", depart.plusMinutes(135), "70"));
        points.add(point("Marrakech", depart.plusMinutes(270), "120"));
        return points;
    }

    private TrajetRequest requete(int places, List<PointArretRequest> points) {
        return new TrajetRequest(1L, 10L, places, points);
    }

    private void preparerConducteurEtVehicule() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));
    }

    private void assertRefuse(TrajetRequest requete, String fragmentDuMessage) {
        assertThatThrownBy(() -> trajetService.publier(requete))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(fragmentDuMessage);
        verify(trajetRepository, never()).save(any());
    }

    //publier

    @Test
    @DisplayName("publier_departDansLePasse_lanceInvalidRequestException")
    void publier_nominal_deduitVillesDateOrdreEtStatut() {
        preparerConducteurEtVehicule();
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(inv -> inv.getArgument(0));
        TrajetResponse reponse = new TrajetResponse(1L, null, null, "Rabat", "Marrakech",
                depart, 3, 3, StatutTrajet.PUBLIE, List.of());

        when(trajetMapper.toResponse(any(Trajet.class), anyInt())).thenReturn(reponse);

        List<PointArretRequest> points = pointsValides();
        points.set(0, point(" Rabat ", depart, "0"));

        TrajetResponse resultat = trajetService.publier(requete(3, points));

        ArgumentCaptor<Trajet> captor = ArgumentCaptor.forClass(Trajet.class);
        verify(trajetRepository).save(captor.capture());
        Trajet sauvegarde = captor.getValue();

        assertThat(sauvegarde.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(sauvegarde.getVilleDepart()).isEqualTo("Rabat");
        assertThat(sauvegarde.getVilleArrivee()).isEqualTo("Marrakech");
        assertThat(sauvegarde.getDateDepart()).isEqualTo(depart);
        assertThat(sauvegarde.getPlacesProposees()).isEqualTo(3);
        assertThat(sauvegarde.getConducteur()).isSameAs(conducteur);
        assertThat(sauvegarde.getVehicule()).isSameAs(vehicule);


        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getOrdre)
                .containsExactly(0, 1, 2, 3);
        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getVille)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getPrixDepuisDepart)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("40.00"),
                        new BigDecimal("70.00"), new BigDecimal("120.00"));
        assertThat(sauvegarde.getPointsArret())
                .allSatisfy(p -> assertThat(p.getTrajet()).isSameAs(sauvegarde));

        assertThat(resultat).isEqualTo(reponse);
    }

    @Test
    void publier_conducteurInexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(vehiculeRepository);
        verify(trajetRepository, never()).save(any());
    }

    @Test
    void publier_vehiculeInexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(trajetRepository, never()).save(any());
    }

    //
    @Test
    void publier_vehiculeDunAutreConducteur_lanceForbiddenActionException() {
        Vehicule vehiculeDunAutre = vehicule(10L, conducteur(2L), 4);
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehiculeDunAutre));

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(ForbiddenActionException.class);

        verify(trajetRepository, never()).save(any());
    }

    //
    @Test
    void publier_placesSuperieuresALaCapacite_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();

        assertRefuse(requete(5, pointsValides()), "capacite");
    }


    @Test
    @DisplayName("publier_departDansLePasse_lanceInvalidRequestException")
    void publier_departDansLePasse_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        points.set(0, point("Rabat", LocalDateTime.now().minusHours(1), "0"));

        assertRefuse(requete(3, points), "futur");
    }

    @Test
    void publier_premierPrixDifferentDeZero_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        points.set(0, point("Rabat", depart, "5"));

        assertRefuse(requete(3, points), "point de depart");
    }

    @Test
    void publier_villeRepetee_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        points.set(2, point(" RABAT ", depart.plusMinutes(135), "70"));

        assertRefuse(requete(3, points), "plusieurs fois");
    }

    @Test
    void publier_heuresNonStrictementCroissantes_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        points.set(2, point("Settat", depart.plusMinutes(75), "70"));

        assertRefuse(requete(3, points), "heures de passage");
    }

    @Test
    void publier_prixNonStrictementCroissants_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        // meme prix que le point precedent
        points.set(2, point("Settat", depart.plusMinutes(135), "40"));

        assertRefuse(requete(3, points), "prix depuis le depart");
    }

    //trouverParId
    @Test
    void trouverParId_nominal_utiliseLaRequeteAvecJoinFetch() {
        Trajet trajet = new Trajet();
        trajet.setId(5L);
        trajet.setPlacesProposees(3);
        TrajetResponse reponse = new TrajetResponse(5L, null, null, "Rabat", "Marrakech",
                depart, 3, 3, StatutTrajet.PUBLIE, List.of());
        when(trajetRepository.findDetailById(5L)).thenReturn(Optional.of(trajet));
        when(trajetMapper.toResponse(eq(trajet), anyInt())).thenReturn(reponse);

        TrajetResponse resultat = trajetService.trouverParId(5L);

        assertThat(resultat).isEqualTo(reponse);
        verify(trajetRepository).findDetailById(5L);
    }

    @Test
    void trouverParId_inexistant_lanceResourceNotFoundException() {
        when(trajetRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.trouverParId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // rechercher
    // Un point d'arret
    private PointArret pointEntite(String ville, LocalDateTime heure, String prix) {
        PointArret p = new PointArret();
        p.setVille(ville);
        p.setHeurePassage(heure);
        p.setPrixDepuisDepart(new BigDecimal(prix));
        return p;
    }

    private final LocalDate dateRecherche = LocalDate.now().plusDays(3);

    @Test
    void rechercher_nominal_construitLeResultatAvecLePrixDuSegment() {
        Conducteur hamid = conducteur(1L);
        hamid.setPrenom("Hamid");
        hamid.setNom("Alaoui");
        Trajet trajet = new Trajet();
        trajet.setId(5L);
        trajet.setConducteur(hamid);
        trajet.setPlacesProposees(3);
        trajet.ajouterPointArret(pointEntite("Rabat", depart, "0"));
        trajet.ajouterPointArret(pointEntite("Casablanca", depart.plusMinutes(75), "40"));
        trajet.ajouterPointArret(pointEntite("Settat", depart.plusMinutes(135), "70"));
        trajet.ajouterPointArret(pointEntite("Marrakech", depart.plusMinutes(270), "120"));

        when(trajetRepository.rechercher(
                eq("casablanca"), eq("settat"),
                eq(dateRecherche.atStartOfDay()), eq(dateRecherche.plusDays(1).atStartOfDay()),
                eq(new BigDecimal("99999999.99")),
                eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(trajet), PageRequest.of(0, 10), 1));

        Page<TrajetRechercheResponse> resultat = trajetService.rechercher(
                " Casablanca ", "SETTAT", dateRecherche, null, 0, 10);

        assertThat(resultat.getContent()).hasSize(1);
        TrajetRechercheResponse r = resultat.getContent().get(0);
        assertThat(r.trajetId()).isEqualTo(5L);
        assertThat(r.conducteurName()).isEqualTo("Hamid Alaoui");
        assertThat(r.villeMontee()).isEqualTo("Casablanca");
        assertThat(r.villeDescente()).isEqualTo("Settat");
        assertThat(r.heureMontee()).isEqualTo(depart.plusMinutes(75));
        assertThat(r.prixSegment()).isEqualByComparingTo("30"); // 70 - 40
        assertThat(r.placesRestantes()).isEqualTo(3L);
        assertThat(resultat.getTotalElements()).isEqualTo(1);
    }

    // Un trajet deja publie du conducteur 1
    private Trajet trajetExistantSurPeriode(LocalDateTime debut, LocalDateTime fin) {
        Trajet t = new Trajet();
        t.setId(99L);
        t.setConducteur(conducteur);
        t.setStatut(StatutTrajet.PUBLIE);
        t.setDateDepart(debut);
        PointArret premierPoint = new PointArret();
        premierPoint.setHeurePassage(debut);
        PointArret dernierPoint = new PointArret();
        dernierPoint.setHeurePassage(fin);
        t.ajouterPointArret(premierPoint);
        t.ajouterPointArret(dernierPoint);
        return t;
    }

    // conductereur deja a un trajet
    @Test
    void publier_conducteurDejaOccupe_lanceBusinessRuleException() {
        preparerConducteurEtVehicule();
        // trajet existant : de depart+60 min a depart+240 min -> chevauche le nouveau
        when(trajetRepository.findByConducteurIdAndStatutIn(eq(1L), any()))
                .thenReturn(List.of(trajetExistantSurPeriode(depart.plusMinutes(60), depart.plusMinutes(240))));

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deja un trajet");

        verify(trajetRepository, never()).save(any());
    }

    // Publie un trajet
    @Test
    void publier_trajetQuiCommenceQuandLePrecedentFinit_estAccepte() {
        preparerConducteurEtVehicule();
        when(trajetRepository.findByConducteurIdAndStatutIn(eq(1L), any()))
                .thenReturn(List.of(trajetExistantSurPeriode(depart.minusHours(3), depart)));
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trajetMapper.toResponse(any(Trajet.class), anyInt())).thenReturn(
                new TrajetResponse(1L, null, null, "Rabat", "Marrakech",
                        depart, 3, 3, StatutTrajet.PUBLIE, List.of()));

        trajetService.publier(requete(3, pointsValides()));

        verify(trajetRepository).save(any(Trajet.class));
    }
}