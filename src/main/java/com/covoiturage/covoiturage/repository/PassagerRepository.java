package com.covoiturage.covoiturage.repository;

import com.covoiturage.covoiturage.model.Passager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassagerRepository extends JpaRepository<Passager, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Passager> findByEmailIgnoreCase(String email);
}