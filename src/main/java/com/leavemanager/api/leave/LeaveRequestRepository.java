package com.leavemanager.api.leave;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // EntityGraph loads the related users in the same query (avoids the N+1 problem)
    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    List<LeaveRequest> findByStatusOrderByStartDateAsc(LeaveStatus status);

    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    List<LeaveRequest> findAllByOrderByStartDateDesc();

    @Query("""
            select count(l) > 0 from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("statuses") Collection<LeaveStatus> statuses);
}
