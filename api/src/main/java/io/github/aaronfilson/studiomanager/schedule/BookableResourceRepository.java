package io.github.aaronfilson.studiomanager.schedule;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's bookable resources; Hibernate adds the studio to every query. */
interface BookableResourceRepository extends JpaRepository<BookableResource, UUID> {

  @Query("select r from BookableResource r order by r.name")
  List<BookableResource> findAllByName();

  /**
   * The resource, locked until the transaction ends, so bookings on it are checked and made one at
   * a time and two people can't both take its last seat.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from BookableResource r where r.id = :resourceId")
  Optional<BookableResource> findForBooking(UUID resourceId);

  @Query("select r from BookableResource r where r.assetId = :assetId")
  List<BookableResource> findByAsset(UUID assetId);
}
