package com.earth.online.player.ailearn.assistant;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** 只清理助手过期状态；不删除评论、建议、用户和刷题记录。 */
@Configuration
@EnableScheduling
public class AssistantMaintenance {
    private final AssistantRepository repo;
    private final AssistantProperties properties;

    public AssistantMaintenance(AssistantRepository repo, AssistantProperties properties) {
        this.repo = repo; this.properties = properties;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void expire() {
        repo.jdbc().update("UPDATE assistant_run SET status='EXPIRED',execution_epoch=execution_epoch+1 WHERE expires_at <= CURRENT_TIMESTAMP AND status NOT IN ('COMPLETED','FAILED','CANCELLED','EXPIRED')");
        repo.jdbc().update("UPDATE assistant_run SET status='PAUSED',execution_epoch=execution_epoch+1 WHERE status='RUNNING' AND lease_expires_at <= CURRENT_TIMESTAMP AND expires_at > CURRENT_TIMESTAMP");
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    @Transactional
    public void cleanup() {
        repo.jdbc().update("DELETE m FROM assistant_message m INNER JOIN assistant_session s ON s.id=m.session_id WHERE s.expires_at <= CURRENT_TIMESTAMP");
        repo.jdbc().update("DELETE FROM assistant_session WHERE expires_at <= CURRENT_TIMESTAMP");
        var cutoff = java.time.LocalDateTime.now().minusDays(Math.max(1, properties.operationRetentionDays()));
        repo.jdbc().update("DELETE op FROM assistant_operation op INNER JOIN assistant_run r ON r.id=op.run_id WHERE r.created_at < ? AND r.status IN ('COMPLETED','FAILED','CANCELLED','EXPIRED')", cutoff);
        repo.jdbc().update("DELETE FROM assistant_run WHERE created_at < ? AND status IN ('COMPLETED','FAILED','CANCELLED','EXPIRED')", cutoff);
    }
}
