package com.covoiturage.covoiturage.repository;

import com.covoiturage.covoiturage.dto.trajet.TrajetRechercheResponse;
import com.covoiturage.covoiturage.model.Trajet;
import com.covoiturage.covoiturage.model.enums.StatutTrajet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    @Query("""
            select t from Trajet t
            join fetch t.conducteur
            join fetch t.vehicule
            left join fetch t.pointsArret
            where t.id = :id
            """)
    Optional<Trajet> findDetailById(@Param("id") Long id);


    // for republication
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trajet t where t.id = :id")
    Optional<Trajet> findByIdForUpdate(@Param("id") Long id);




    // rechercher
    @Query("""
       select t from Trajet t join t.pointsArret depart join t.pointsArret arrivee
            where t.statut = com.covoiturage.covoiturage.model.enums.StatutTrajet.PUBLIE
                 and lower(depart.ville) = :villeDepart and lower(arrivee.ville) = :villeArrivee
                      and depart.ordre < arrivee.ordre
                           and t.dateDepart >= :debutJour and t.dateDepart < :finJour
                                and (arrivee.prixDepuisDepart - depart.prixDepuisDepart) <= :prixMax
                                     order by t.dateDepart, t.id
     """)
    Page<Trajet> rechercher(
                                             @Param("villeDepart") String villeDepart,
                                             @Param("villeArrivee") String villeArrivee,
                                             @Param("debutJour") LocalDateTime debutJour,
                                             @Param("finJour") LocalDateTime finJour,
                                             @Param("prixMax")BigDecimal prixMax,
                                             Pageable pageable);


    boolean existsByVehiculeId(Long vehiculeId);

    List<Trajet> findByConducteurIdAndStatutIn(Long conducteurId, Collection<StatutTrajet> status);
}