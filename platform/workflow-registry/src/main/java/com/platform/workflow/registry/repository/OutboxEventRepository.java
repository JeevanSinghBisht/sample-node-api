package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value = """
                    SELECT * FROM outbox_event
                    WHERE published_at IS NULL
                    ORDER BY created_at ASC
                    LIMIT ?1
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> findUnpublishedForUpdate(int limit);
}
