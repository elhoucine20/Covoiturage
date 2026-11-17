package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.trajet.PointArretRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.trajet.TrajetMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.PointArret;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.model.Vehicule;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.repository.TrajetRepository;
import com.covoiturage.covoiturage.repository.VehiculeRepository;
import com.covoiturage.covoiturage.service.impl.TrajetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @InjectMocks
    private TrajetServiceImpl trajetService;

    // Date de depart du trajet de test : toujours dans le futur (relative a "maintenant")
    private final LocalDateTime depart = LocalDateTime.now().plusDays(5);

    private Conducteur conducteur;
    private Vehicule vehicule; // capacite 4, appartient a "conducteur"

    @BeforeEach
    void setUp() {
        conducteur = conducteur(1L);
        vehicule = vehicule(10L, conducteur, 4);
    }

    // ---------- methodes utilitaires du test (helpers) ----------

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

    // Un point d'arret de la requete (le prix est donne en texte pour eviter les erreurs de double)
    private PointArretRequest point(String ville, LocalDateTime heure, String prix) {
        return new PointArretRequest(ville, heure, new BigDecimal(prix));
    }

    // Le trajet de Hamid (brief) : Rabat 0 -> Casablanca 40 -> Settat 70 -> Marrakech 120.
    // Renvoie une liste MODIFIABLE : chaque test d'erreur remplace UN seul point avec list.set(...)
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

    // Programme les deux recherches (conducteur 1 et vehicule 10), utilisees par la plupart des tests
    private void preparerConducteurEtVehicule() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));
    }

    // Verifie qu'une requete invalide est refusee (400) ET que rien n'est sauvegarde
    private void assertRefuse(TrajetRequest requete, String fragmentDuMessage) {
        assertThatThrownBy(() -> trajetService.publier(requete))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(fragmentDuMessage);
        verify(trajetRepository, never()).save(any());
    }

    // ---------- publier : cas nominal ----------

    @Test
    void publier_nominal_deduitVillesDateOrdreEtStatut() {
        preparerConducteurEtVehicule();
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(inv -> inv.getArgument(0));
        TrajetResponse reponse = new TrajetResponse(1L, null, null, "Rabat", "Marrakech",
                depart, 3, 3, StatutTrajet.PUBLIE, List.of());
        // anyInt() : on ne fige pas le nombre de places restantes, car ce calcul changera
        // quand les reservations existeront (le test ne casse pas pour autant)
        when(trajetMapper.toResponse(any(Trajet.class), anyInt())).thenReturn(reponse);

        List<PointArretRequest> points = pointsValides();
        points.set(0, point(" Rabat ", depart, "0")); // espaces autour : teste le trim

        TrajetResponse resultat = trajetService.publier(requete(3, points));

        ArgumentCaptor<Trajet> captor = ArgumentCaptor.forClass(Trajet.class);
        verify(trajetRepository).save(captor.capture());
        Trajet sauvegarde = captor.getValue();

        assertThat(sauvegarde.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(sauvegarde.getVilleDepart()).isEqualTo("Rabat");        // premier point, sans espaces
        assertThat(sauvegarde.getVilleArrivee()).isEqualTo("Marrakech");   // dernier point
        assertThat(sauvegarde.getDateDepart()).isEqualTo(depart);          // heure du premier point
        assertThat(sauvegarde.getPlacesProposees()).isEqualTo(3);
        assertThat(sauvegarde.getConducteur()).isSameAs(conducteur);
        assertThat(sauvegarde.getVehicule()).isSameAs(vehicule);

        // extracting : extrait un champ de chaque element de la liste, pour le comparer d'un coup
        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getOrdre)
                .containsExactly(0, 1, 2, 3);
        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getVille)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
        // setScale(2) : les prix sont stockes avec 2 decimales (BigDecimal.equals tient compte de l'echelle)
        assertThat(sauvegarde.getPointsArret()).extracting(PointArret::getPrixDepuisDepart)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("40.00"),
                        new BigDecimal("70.00"), new BigDecimal("120.00"));
        // allSatisfy : la condition doit etre vraie pour CHAQUE point. Ici : chaque point
        // connait son trajet (c'est le role de la methode ajouterPointArret, sinon la cle etrangere serait vide)
        assertThat(sauvegarde.getPointsArret())
                .allSatisfy(p -> assertThat(p.getTrajet()).isSameAs(sauvegarde));

        assertThat(resultat).isEqualTo(reponse);
    }

    // ---------- publier : ressources et droits ----------

    @Test
    void publier_conducteurInexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(vehiculeRepository); // le service s'arrete avant de chercher le vehicule
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

    // Scenario 8 du brief : Hamid publie avec le vehicule d'un autre conducteur -> 403
    @Test
    void publier_vehiculeDunAutreConducteur_lanceForbiddenActionException() {
        Vehicule vehiculeDunAutre = vehicule(10L, conducteur(2L), 4);
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehiculeDunAutre));

        assertThatThrownBy(() -> trajetService.publier(requete(3, pointsValides())))
                .isInstanceOf(ForbiddenActionException.class);

        verify(trajetRepository, never()).save(any());
    }

    // Scenario 7 du brief : 5 places proposees pour un vehicule de capacite 4 -> 400
    @Test
    void publier_placesSuperieuresALaCapacite_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();

        assertRefuse(requete(5, pointsValides()), "capacite");
    }

    // ---------- publier : regles du parcours (un test par regle) ----------

    @Test
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
        // " RABAT " reapparait en 3e position : teste aussi la normalisation (trim + minuscules)
        points.set(2, point(" RABAT ", depart.plusMinutes(135), "70"));

        assertRefuse(requete(3, points), "plusieurs fois");
    }

    @Test
    void publier_heuresNonStrictementCroissantes_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        // meme heure que le point precedent (Casablanca) : "strictement" croissantes -> refuse
        points.set(2, point("Settat", depart.plusMinutes(75), "70"));

        assertRefuse(requete(3, points), "heures de passage");
    }

    @Test
    void publier_prixNonStrictementCroissants_lanceInvalidRequestException() {
        preparerConducteurEtVehicule();
        List<PointArretRequest> points = pointsValides();
        // meme prix que le point precedent (40) : refuse
        points.set(2, point("Settat", depart.plusMinutes(135), "40"));

        assertRefuse(requete(3, points), "prix depuis le depart");
    }

    // ---------- trouverParId ----------

    @Test
    void trouverParId_nominal_utiliseLaRequeteAvecJoinFetch() {
        Trajet trajet = new Trajet();
        trajet.setId(5L);
        trajet.setPlacesProposees(3);
        TrajetResponse reponse = new TrajetResponse(5L, null, null, "Rabat", "Marrakech",
                depart, 3, 3, StatutTrajet.PUBLIE, List.of());
        when(trajetRepository.findDetailById(5L)).thenReturn(Optional.of(trajet));
        // Quand on utilise un matcher (eq, anyInt), TOUS les arguments doivent etre des matchers
        when(trajetMapper.toResponse(eq(trajet), anyInt())).thenReturn(reponse);

        TrajetResponse resultat = trajetService.trouverParId(5L);

        assertThat(resultat).isEqualTo(reponse);
        // Le detail passe par findDetailById (join fetch, une seule requete SQL : pas de N+1)
        verify(trajetRepository).findDetailById(5L);
    }

    @Test
    void trouverParId_inexistant_lanceResourceNotFoundException() {
        when(trajetRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.trouverParId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}