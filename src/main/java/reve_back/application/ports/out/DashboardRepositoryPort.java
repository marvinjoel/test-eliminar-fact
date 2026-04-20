package reve_back.application.ports.out;

import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.SessionReportResponse;
import reve_back.infrastructure.web.dto.WorkerRankingResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardRepositoryPort {

    List<SessionReportResponse> getSessionHistory(LocalDateTime start, LocalDateTime end);
    List<WorkerRankingResponse> getWorkerRanking(LocalDateTime start, LocalDateTime end);
    List<InventoryAlertResponse> getInventoryAlerts(Double thresholdPercentage);


}
