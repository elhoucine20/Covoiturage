package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.conducteur.ConducteurRequest;
import com.covoiturage.covoiturage.dto.conducteur.ConducteurResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.conducteur.ConducteurMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.service.impl.ConducteurServiceImpl;
import com.covoiturage.covoiturage.util.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Active Mockito dans JUnit cree les @Mock et les injecte dans @InjectMocks
@ExtendWith(MockitoExtension.class)
class ConducteurServiceImplTest {

    @Mock
    private ConducteurRepository conducteurRepository;
    @Mock
    private ConducteurMapper conducteurMapper;
    @Mock
    private PasswordUtils passwordUtils;

    @InjectMocks
    private ConducteurServiceImpl conducteurService;

    private ConducteurRequest request;

    @BeforeEach
    void setUp() {
        request = new ConducteurRequest(
                "Lahcen", "Akdime", " Lahcen@Gmail.com ",
                "0601667201", "1234", "12345678");
    }

    @Test
    void creer_nominal_sauvegardeEmailNormaliseEtMotDePasseHache() {
        // given : on programme les doublures
        Conducteur entite = new Conducteur();
        ConducteurResponse reponse = new ConducteurResponse(
                1L, "Lahcen", "Akdime", "lahcen@gmail.com", "0601667201", "1234");

        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermis("1234")).thenReturn(false);
        when(passwordUtils.generateSalt()).thenReturn("sel");
        when(passwordUtils.hashPassword("12345678", "sel")).thenReturn("hash");
        when(conducteurMapper.toEntity(request)).thenReturn(entite);
        // thenAnswer : le repository "renvoie" l'objet qu'on lui a donne
        when(conducteurRepository.save(any(Conducteur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conducteurMapper.toResponse(any(Conducteur.class))).thenReturn(reponse);

        // when
        ConducteurResponse resultat = conducteurService.creer(request);

        // then : ArgumentCaptor recupere l'objet reellement envoye a save()
        ArgumentCaptor<Conducteur> captor = ArgumentCaptor.forClass(Conducteur.class);
        verify(conducteurRepository).save(captor.capture());
        Conducteur sauvegarde = captor.getValue();

        assertThat(sauvegarde.getEmail()).isEqualTo("lahcen@gmail.com");
        assertThat(sauvegarde.getNumeroPermis()).isEqualTo("1234");
        assertThat(sauvegarde.getSalt()).isEqualTo("sel");
        assertThat(sauvegarde.getPassword()).isEqualTo("hash").isNotEqualTo("12345678");
        assertThat(resultat).isEqualTo(reponse);
    }

    @Test
    void creer_emailDejaUtilise_lanceBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.creer(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("lahcen@gmail.com");

        // Regle refusee : rien ne doit etre sauvegarde
        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void creer_permisDejaUtilise_lanceBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermis("1234")).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.creer(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1234");

        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void trouverParId_inexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conducteurService.trouverParId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void modifier_emailPrisParUnAutre_lanceBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(new Conducteur()));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot("lahcen@gmail.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.modifier(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void modifier_nominal_sauvegardeLesNouvellesValeurs() {
        Conducteur existant = new Conducteur();
        ConducteurResponse reponse = new ConducteurResponse(
                1L, "Lahcen", "Akdime", "lahcen@gmail.com", "0601667201", "1234");

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(existant));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot("lahcen@gmail.com", 1L)).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermisAndIdNot("1234", 1L)).thenReturn(false);
        when(passwordUtils.generateSalt()).thenReturn("sel");
        when(passwordUtils.hashPassword("12345678", "sel")).thenReturn("hash");
        when(conducteurRepository.save(existant)).thenReturn(existant);
        when(conducteurMapper.toResponse(existant)).thenReturn(reponse);

        ConducteurResponse resultat = conducteurService.modifier(1L, request);

        assertThat(existant.getEmail()).isEqualTo("lahcen@gmail.com");
        assertThat(existant.getPassword()).isEqualTo("hash");
        assertThat(resultat).isEqualTo(reponse);
    }
}