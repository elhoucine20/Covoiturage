package com.covoiturage.covoiturage.service.impl;

import com.covoiturage.covoiturage.dto.conducteur.ConducteurRequest;
import com.covoiturage.covoiturage.dto.conducteur.ConducteurResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.conducteur.ConducteurMapper;
import com.covoiturage.covoiturage.model.Conducteur;
import com.covoiturage.covoiturage.repository.ConducteurRepository;
import com.covoiturage.covoiturage.service.ConducteurService;
import com.covoiturage.covoiturage.util.PasswordUtils;
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
public class ConducteurServiceImpl implements ConducteurService {

    private final ConducteurRepository conducteurRepository;
    private final ConducteurMapper conducteurMapper;
    private final PasswordUtils passwordUtils;


    @Override
    @Transactional
    public ConducteurResponse creer(ConducteurRequest request) {
        String email = normaliserEmail(request.email());
        String numeroPermis = request.numeroPermis().trim();

        if (conducteurRepository.existsByEmailIgnoreCase(email)){
            throw new BusinessRuleException("Un conducteur avec l'email " + email + " existe deja!!");
        }
        if (conducteurRepository.existsByNumeroPermis(numeroPermis)){
            throw new BusinessRuleException("Un conducteur avec le numero de permis " + numeroPermis + " existe deja!!");
        }
        String salt = passwordUtils.generateSalt();
        String hashPassword = passwordUtils.hashPassword(request.password(),salt);

        Conducteur conducteur = conducteurMapper.toEntity(request);
        conducteur.setEmail(email);
        conducteur.setNumeroPermis(numeroPermis);
        conducteur.setSalt(salt);
        conducteur.setPassword(hashPassword);

        Conducteur sauvegarde = conducteurRepository.save(conducteur);
        log.info("conducteur cree : id{}",sauvegarde.getId());
        return conducteurMapper.toResponse(sauvegarde);
    }

    @Override
    public ConducteurResponse trouverParId(Long id) {
        return conducteurMapper.toResponse(chercher(id));
    }

    @Override
    public List<ConducteurResponse> lister() {
        return conducteurRepository.findAll().stream().map(conducteurMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ConducteurResponse modifier(Long id, ConducteurRequest request) {
        Conducteur conducteur = chercher(id);
        String email = normaliserEmail(request.email());
        String numeroPermis = request.numeroPermis().trim();

        if (conducteurRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("Un autre conducteur utilise deja l'email " + email + "!!");
        }
        if (conducteurRepository.existsByNumeroPermisAndIdNot(numeroPermis, id)) {
            throw new BusinessRuleException("Un autre conducteur utilise deja le numero de permis " + numeroPermis + "!!");
        }

        conducteurMapper.updateEntity(request, conducteur);
        conducteur.setEmail(email);
        conducteur.setNumeroPermis(numeroPermis);

        String salt = passwordUtils.generateSalt();
        conducteur.setSalt(salt);
        conducteur.setPassword(passwordUtils.hashPassword(request.password(), salt));

        Conducteur sauvegarder = conducteurRepository.save(conducteur);
        log.info("Conducteur modifie : id{}",sauvegarder.getId());
        return conducteurMapper.toResponse(sauvegarder);
    }

    @Override
    public List<ConducteurResponse> rechercher(String critere) {
        return conducteurRepository.rechercherParNomOuEmail(critere.trim()).stream().map(conducteurMapper::toResponse).toList();
    }



    // hepers
    private String normaliserEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT); //
    }

    // find by id
    private Conducteur chercher(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur introuvable avec l'identifiant " + id));
    }


}
