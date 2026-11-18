package com.covoiturage.covoiturage.repository;

import com.covoiturage.covoiturage.model.Reservation;
import com.covoiturage.covoiturage.model.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByPassagerIdAndTrajetIdAndStatut(Long passagerId, Long trajetId, StatutReservation statut);

    // coalesce  renvoie 0 s'il n'y a aucune reservation
    @Query("""
            select coalesce(sum(r.nombrePlaces), 0L)
            from Reservation r
            where r.trajet.id = :trajetId and r.statut = :statut
            """)
    long sommePlacesParTrajetEtStatut(@Param("trajetId") Long trajetId,
                                      @Param("statut") StatutReservation statut);

    // Pour l'annulation on lit uniquement l'id du trajet (sans charger le trajet)
    @Query("select r.trajet.id from Reservation r where r.id = :id")
    Optional<Long> findTrajetIdById(@Param("id") Long id);

    //
    @Query("""
            select r from Reservation r
            join fetch r.passager
            join fetch r.trajet
            join fetch r.pointMontee
            join fetch r.pointDescente
            where r.passager.id = :passagerId
            order by r.dateReservation desc
            """)
    List<Reservation> findByPassagerIdAvecDetails(@Param("passagerId") Long passagerId);

    @Query("""
            select r from Reservation r
            join fetch r.passager
            join fetch r.trajet
            join fetch r.pointMontee
            join fetch r.pointDescente
            where r.trajet.id = :trajetId
            order by r.dateReservation asc
            """)
    List<Reservation> findByTrajetIdAvecDetails(@Param("trajetId") Long trajetId);
}