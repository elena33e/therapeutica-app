package com.therapeutica.therapeutica_app.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByPacientIdOrderByDataOraDesc(UUID pacientId);

    List<AuditLog> findByUtilizatorIdOrderByDataOraDesc(UUID utilizatorId);
}