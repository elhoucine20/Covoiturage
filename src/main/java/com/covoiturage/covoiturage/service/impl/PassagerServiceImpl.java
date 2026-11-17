package com.covoiturage.covoiturage.service.impl;

import com.covoiturage.covoiturage.dto.passager.PassagerRequest;
import com.covoiturage.covoiturage.dto.passager.PassagerResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import com.covoiturage.covoiturage.mapper.passager.PassagerMapper;
import com.covoiturage.covoiturage.model.Passager;
import com.covoiturage.covoiturage.repository.PassagerRepository;
import com.covoiturage.covoiturage.service.PassagerService;
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
public class PassagerServiceImpl implements PassagerService{

    private final PassagerRepository passagerRepository;
    private final PassagerMapper passagerMapper;
    private final PasswordUtils passwordUtils;

    @Override
    @Transactional
    public PassagerResponse creer(PassagerRequest request) {
        String email = normaliserEmail(request.email());

        if (passagerRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Un passager avec l'email " + email + " existe deja!!");
        }

        String salt = passwordUtils.generateSalt();
        Passager passager = passagerMapper.toEntity(request);
        passager.setEmail(email);
        passager.setSalt(salt);
        passager.setPassword(passwordUtils.hashPassword(request.password(), salt));

        Passager sauvegarde = passagerRepository.save(passager);
        log.info("Passager cree : id={}", sauvegarde.getId());
        return passagerMapper.toResponse(sauvegarde);
    }

    @Override
    public PassagerResponse trouverParId(Long id) {
        return passagerMapper.toResponse(chercher(id));
    }

    @Override
    public List<PassagerResponse> lister() {
        return passagerRepository.findAll().stream().map(passagerMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public PassagerResponse modifier(Long id, PassagerRequest request) {
        Passager passager = chercher(id);
        String email = normaliserEmail(request.email());

        if (passagerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("Un autre passager utilise deja l'email " + email + "!!");
        }

        passagerMapper.updateEntity(request, passager);
        passager.setEmail(email);
        String salt = passwordUtils.generateSalt();
        passager.setSalt(salt);
        passager.setPassword(passwordUtils.hashPassword(request.password(), salt));

        Passager sauvegarde = passagerRepository.save(passager);
        log.info("Passager modifie : id={}", sauvegarde.getId());
        return passagerMapper.toResponse(sauvegarde);
    }

    @Override
    public PassagerResponse trouverParEmail(String email) {
        return passagerRepository.findByEmailIgnoreCase(normaliserEmail(email))
                .map(passagerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Passager introuvable avec l'email " + email));
    }

    // helpers
    private String normaliserEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Passager chercher(Long id) {
        return passagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passager introuvable avec l'identifiant " + id));
    }

}
