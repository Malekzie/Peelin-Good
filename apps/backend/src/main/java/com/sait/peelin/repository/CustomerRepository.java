package com.sait.peelin.repository;

import com.sait.peelin.model.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    Optional<Customer> findByUser_UserId(UUID userId);

    boolean existsByUser_UserId(UUID userId);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("SELECT c FROM Customer c WHERE c.user IS NOT NULL AND c.user.photoApprovalPending = true")
    List<Customer> findByUserPhotoApprovalPendingTrue();

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("SELECT c FROM Customer c WHERE "
            + "LOWER(COALESCE(c.customerFirstName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(COALESCE(c.customerLastName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(c.customerEmail) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Customer> search(@Param("q") String q);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    Optional<Customer> findById(UUID id);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(TRIM(c.customerEmail)) = LOWER(TRIM(:email))
            ORDER BY CASE WHEN c.user IS NULL THEN 1 ELSE 0 END, c.id
            """)
    List<Customer> findByCustomerEmailNormalized(@Param("email") String email);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("""
            SELECT c FROM Customer c
            JOIN c.address a
            WHERE LOWER(TRIM(c.customerFirstName)) = LOWER(TRIM(:firstName))
              AND LOWER(COALESCE(TRIM(c.customerMiddleInitial), '')) = LOWER(COALESCE(TRIM(:middleInitial), ''))
              AND LOWER(TRIM(c.customerLastName)) = LOWER(TRIM(:lastName))
              AND TRIM(c.customerPhone) = TRIM(:phone)
              AND COALESCE(TRIM(c.customerBusinessPhone), '') = COALESCE(TRIM(:businessPhone), '')
              AND LOWER(TRIM(c.customerEmail)) = LOWER(TRIM(:email))
              AND LOWER(TRIM(a.addressLine1)) = LOWER(TRIM(:addressLine1))
              AND LOWER(COALESCE(TRIM(a.addressLine2), '')) = LOWER(COALESCE(TRIM(:addressLine2), ''))
              AND LOWER(TRIM(a.addressCity)) = LOWER(TRIM(:city))
              AND LOWER(TRIM(a.addressProvince)) = LOWER(TRIM(:province))
              AND UPPER(REPLACE(TRIM(a.addressPostalCode), ' ', '')) = UPPER(REPLACE(TRIM(:postalCode), ' ', ''))
            ORDER BY CASE WHEN c.user IS NULL THEN 1 ELSE 0 END, c.id
            """)
    List<Customer> findExactMatches(
            @Param("firstName") String firstName,
            @Param("middleInitial") String middleInitial,
            @Param("lastName") String lastName,
            @Param("phone") String phone,
            @Param("businessPhone") String businessPhone,
            @Param("email") String email,
            @Param("addressLine1") String addressLine1,
            @Param("addressLine2") String addressLine2,
            @Param("city") String city,
            @Param("province") String province,
            @Param("postalCode") String postalCode
    );

    /**
     * Phone fields match on digits only so stored formatting {@code (###) ###-####} aligns with legacy rows.
     */
    @Query(value = """
            SELECT c.* FROM customer c
            INNER JOIN address a ON c.address_id = a.address_id
            WHERE c.user_id IS NULL
              AND lower(trim(c.customer_first_name)) = lower(trim(:firstName))
              AND lower(trim(coalesce(c.customer_middle_initial, ''))) = lower(trim(coalesce(:middleInitial, '')))
              AND lower(trim(c.customer_last_name)) = lower(trim(:lastName))
              AND regexp_replace(btrim(c.customer_phone), '\\D', '', 'g') = regexp_replace(btrim(:phone), '\\D', '', 'g')
              AND regexp_replace(btrim(coalesce(c.customer_business_phone, '')), '\\D', '', 'g')
                  = regexp_replace(btrim(coalesce(:businessPhone, '')), '\\D', '', 'g')
              AND lower(trim(c.customer_email)) = lower(trim(:email))
              AND lower(trim(a.address_line1)) = lower(trim(:addressLine1))
              AND lower(trim(coalesce(a.address_line2, ''))) = lower(trim(coalesce(:addressLine2, '')))
              AND lower(trim(a.address_city)) = lower(trim(:city))
              AND lower(trim(a.address_province)) = lower(trim(:province))
              AND upper(replace(trim(a.address_postal_code), ' ', '')) = upper(replace(trim(:postalCode), ' ', ''))
            ORDER BY c.customer_id
            """, nativeQuery = true)
    List<Customer> findExactGuestMatches(
            @Param("firstName") String firstName,
            @Param("middleInitial") String middleInitial,
            @Param("lastName") String lastName,
            @Param("phone") String phone,
            @Param("businessPhone") String businessPhone,
            @Param("email") String email,
            @Param("addressLine1") String addressLine1,
            @Param("addressLine2") String addressLine2,
            @Param("city") String city,
            @Param("province") String province,
            @Param("postalCode") String postalCode
    );

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("""
            SELECT c FROM Customer c
            WHERE c.user IS NULL AND LOWER(TRIM(c.customerEmail)) = LOWER(TRIM(:email))
            """)
    List<Customer> findGuestCustomersByEmailNormalized(@Param("email") String email);

    @Query(value = """
            SELECT customer_id FROM customer c
            WHERE c.user_id IS NULL
            AND regexp_replace(btrim(c.customer_phone), '\\D', '', 'g') = :digits
            """, nativeQuery = true)
    List<UUID> findGuestCustomerIdsByPhoneDigits(@Param("digits") String digits);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    List<Customer> findByIdIn(Collection<UUID> ids);

    @Query(value = """
            SELECT COUNT(*) FROM customer c
            WHERE regexp_replace(btrim(c.customer_phone), '\\D', '', 'g') = :digits
            """, nativeQuery = true)
    long countCustomersWithPhoneDigits(@Param("digits") String digits);

    @Query("select distinct c.user.userId from Customer c where c.user is not null")
    List<UUID> findDistinctLinkedUserIds();
}
