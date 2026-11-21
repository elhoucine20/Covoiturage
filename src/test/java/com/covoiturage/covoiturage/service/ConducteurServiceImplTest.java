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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Active Mockito dans JUnit : cree les @Mock et les injecte dans @InjectMocks
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
        // given
        Conducteur entite = new Conducteur();
        ConducteurResponse reponse = new ConducteurResponse(
                1L, "Lahcen", "Akdime", "lahcen@gmail.com", "0601667201", "1234");

        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermis("1234")).thenReturn(false);
        when(passwordUtils.generateSalt()).thenReturn("sel");
        when(passwordUtils.hashPassword("12345678", "sel")).thenReturn("hash");
        when(conducteurMapper.toEntity(request)).thenReturn(entite);
        when(conducteurRepository.save(any(Conducteur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conducteurMapper.toResponse(any(Conducteur.class))).thenReturn(reponse);

        // when
        ConducteurResponse resultat = conducteurService.creer(request);

        // then
        ArgumentCaptor<Conducteur> captor = ArgumentCaptor.forClass(Conducteur.class);
        verify(conducteurRepository).save(captor.capture());
        Conducteur sauvegarde = captor.getValue();

        //
        assertEquals("lahcen@gmail.com", sauvegarde.getEmail());
        assertEquals("1234", sauvegarde.getNumeroPermis());
        assertEquals("sel", sauvegarde.getSalt());
        assertEquals("hash", sauvegarde.getPassword());
        assertNotEquals("12345678", sauvegarde.getPassword());
        assertEquals(reponse, resultat);
    }

    @Test
    void creer_emailDejaUtilise_lanceBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> conducteurService.creer(request));

        assertTrue(ex.getMessage().contains("lahcen@gmail.com"));

        // pas de save
        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void creer_permisDejaUtilise_lanceBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase("lahcen@gmail.com")).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermis("1234")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> conducteurService.creer(request));

        assertTrue(ex.getMessage().contains("1234"));

        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void trouverParId_inexistant_lanceResourceNotFoundException() {
        when(conducteurRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> conducteurService.trouverParId(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void modifier_emailPrisParUnAutre_lanceBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(new Conducteur()));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot("lahcen@gmail.com", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> conducteurService.modifier(1L, request));

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

        assertEquals("lahcen@gmail.com", existant.getEmail());
        assertEquals("hash", existant.getPassword());
        assertEquals(reponse, resultat);
    }
}