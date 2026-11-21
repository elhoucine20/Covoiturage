package com.covoiturage.covoiturage.repository;

import com.covoiturage.covoiturage.model.Conducteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConducteurRepository extends JpaRepository<Conducteur,Long> {

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id); //
    boolean existsByNumeroPermis(String numeroPermis);
    boolean existsByNumeroPermisAndIdNot(String numeroPermis, Long id);

    @Query("""
            select c from Conducteur c
            where lower(c.nom) like lower(concat('%', :critere, '%')) or lower(c.prenom) like lower(concat('%', :critere, '%'))
               or lower(c.email) like lower(concat('%', :critere, '%'))
            order by c.nom, c.prenom
            """)
    List<Conducteur> rechercherParNomOuEmail(@Param("critere") String critere);

}
