package com.invoiceflow.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByOrganizationId(UUID organizationId);
}
