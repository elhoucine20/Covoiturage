package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.vehicule.VehiculeRequest;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.vehicule.VehiculeMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.model.Vehicule;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.repository.VehiculeRepository;
import com.covoiturage.covoiturage.service.impl.VehiculeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class VehiculeServiceImplTest {

    @Mock
    private VehiculeRepository vehiculeRepository;
    @Mock
    private ConducteurRepository conducteurRepository;
    @Mock
    private VehiculeMapper vehiculeMapper;

    @InjectMocks
    private VehiculeServiceImpl vehiculeService;

    private VehiculeRequest request;
    private VehiculeResponse reponse;

    // Avant chaque test : une requete avec une immatriculation "sale" (espaces + minuscule)
    // pour verifier la normalisation, et la reponse attendue.
    @BeforeEach
    void setUp() {
        request = new VehiculeRequest("Dacia", "Logan", " 12345-a-6 ", "Blanche", 4, 1L);
        reponse = new VehiculeResponse(10L, "Dacia", "Logan", "12345-A-6", "Blanche", 4, 1L);
    }

    // ---------- methodes utilitaires du test (helpers) ----------

    // Construit un conducteur avec un id (setId vient du @Setter de Personne)
    private Conducteur conducteur(Long id) {
        Conducteur conducteur = new Conducteur();
        conducteur.setId(id);
        return conducteur;
    }

    // Construit un vehicule deja "enregistre" (avec id) appartenant a un conducteur
    private Vehicule vehicule(Long id, Conducteur proprietaire) {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(id);
        vehicule.setConducteur(proprietaire);
        return vehicule;
    }

    // ---------- ajouter ----------

    @Test
    void ajouter_nominal_normaliseImmatriculationEtRattacheLeConducteur() {
        Conducteur proprietaire = conducteur(1L);
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
        when(vehiculeRepository.existsByImmatriculationIgnoreCase("12345-A-6")).thenReturn(false);
        when(vehiculeMapper.toEntity(request)).thenReturn(new Vehicule());
        when(vehiculeRepository.save(any(Vehicule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehiculeMapper.toResponse(any(Vehicule.class))).thenReturn(reponse);

        VehiculeResponse resultat = vehiculeService.ajouter(request);

        // ArgumentCaptor : recupere le vehicule reellement envoye a save()
        ArgumentCaptor<Vehicule> captor = ArgumentCaptor.forClass(Vehicule.class);
        verify(vehiculeRepository).save(captor.capture());
        Vehicule sauvegarde = captor.getValue();

        assertThat(sauvegarde.getImmatriculation()).isEqualTo("12345-A-6"); // trim + majuscules
        // isSameAs : exactement le MEME objet (pas seulement un objet egal)
        assertThat(sauvegarde.getConducteur()).isSameAs(proprietaire);
        assertThat(resultat).isEqualTo(reponse);
    }

    @Test
    void ajouter_immatriculationDejaUtilisee_lanceBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur(1L)));
        when(vehiculeRepository.existsByImmatriculationIgnoreCase("12345-A-6")).thenReturn(true);

        assertThatThrownBy(() -> vehiculeService.ajouter(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("12345-A-6");

        verify(vehiculeRepository, never()).save(any());
    }

    @Test
    void ajouter_conducteurInexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.ajouter(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        // verifyNoInteractions : le repository des vehicules n'a MEME PAS ete appele
        // (le service s'arrete des qu'il ne trouve pas le conducteur)
        verifyNoInteractions(vehiculeRepository);
    }

    // ---------- trouverParId / listerParConducteur ----------

    @Test
    void trouverParId_inexistant_lanceResourceNotFoundException() {
        when(vehiculeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.trouverParId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listerParConducteur_nominal_renvoieLesVehiculesDuConducteur() {
        Vehicule vehicule = vehicule(10L, conducteur(1L));
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur(1L)));
        when(vehiculeRepository.findByConducteurId(1L)).thenReturn(List.of(vehicule));
        when(vehiculeMapper.toResponse(vehicule)).thenReturn(reponse);

        List<VehiculeResponse> resultat = vehiculeService.listerParConducteur(1L);

        assertThat(resultat).containsExactly(reponse);
    }

    @Test
    void listerParConducteur_conducteurInexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.listerParConducteur(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(vehiculeRepository);
    }

    // ---------- modifier ----------

    @Test
    void modifier_parUnAutreConducteur_lanceForbiddenActionException() {
        // le vehicule appartient au conducteur 2, mais la requete vient du conducteur 1
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule(10L, conducteur(2L))));

        assertThatThrownBy(() -> vehiculeService.modifier(10L, request))
                .isInstanceOf(ForbiddenActionException.class);

        verify(vehiculeRepository, never()).save(any());
    }

    @Test
    void modifier_immatriculationPriseParUnAutre_lanceBusinessRuleException() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule(10L, conducteur(1L))));
        when(vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot("12345-A-6", 10L)).thenReturn(true);

        assertThatThrownBy(() -> vehiculeService.modifier(10L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(vehiculeRepository, never()).save(any());
    }

    @Test
    void modifier_nominal_appliqueLesChangementsEtNormaliseImmatriculation() {
        Vehicule existant = vehicule(10L, conducteur(1L));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(existant));
        when(vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot("12345-A-6", 10L)).thenReturn(false);
        when(vehiculeRepository.save(existant)).thenReturn(existant);
        when(vehiculeMapper.toResponse(existant)).thenReturn(reponse);

        VehiculeResponse resultat = vehiculeService.modifier(10L, request);

        // updateEntity est une methode void : rien a programmer, on verifie juste qu'elle a ete appelee
        verify(vehiculeMapper).updateEntity(request, existant);
        assertThat(existant.getImmatriculation()).isEqualTo("12345-A-6");
        assertThat(resultat).isEqualTo(reponse);
    }

    // ---------- supprimer ----------

    @Test
    void supprimer_parUnAutreConducteur_lanceForbiddenActionException() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule(10L, conducteur(2L))));

        assertThatThrownBy(() -> vehiculeService.supprimer(10L, 1L))
                .isInstanceOf(ForbiddenActionException.class);

        verify(vehiculeRepository, never()).delete(any());
    }

    @Test
    void supprimer_nominal_supprimeLeVehicule() {
        Vehicule existant = vehicule(10L, conducteur(1L));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(existant));

        vehiculeService.supprimer(10L, 1L);

        verify(vehiculeRepository).delete(existant);
    }
}