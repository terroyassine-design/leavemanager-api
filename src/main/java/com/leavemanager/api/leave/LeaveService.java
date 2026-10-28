package com.leavemanager.api.leave;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leavemanager.api.common.ApiException;
import com.leavemanager.api.user.User;
import com.leavemanager.api.user.UserRepository;

@Service
@Transactional
public class LeaveService {

    private static final Set<LeaveStatus> ACTIVE_STATUSES = Set.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;

    public LeaveService(LeaveRequestRepository leaveRepository, UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
    }

    // ---- Employee side ----

    @Transactional(readOnly = true)
    public List<LeaveResponse> findMine(Long employeeId) {
        return leaveRepository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(LeaveResponse::from)
                .toList();
    }

    public LeaveResponse create(Long employeeId, CreateLeaveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("La date de fin doit être après la date de début");
        }
        if (WorkingDays.between(request.startDate(), request.endDate()) == 0) {
            throw ApiException.badRequest("La période ne contient aucun jour ouvré");
        }
        if (leaveRepository.existsOverlapping(employeeId, request.startDate(), request.endDate(), ACTIVE_STATUSES)) {
            throw ApiException.conflict("Vous avez déjà une demande sur cette période");
        }

        User employee = getUser(employeeId);
        LeaveRequest leave = new LeaveRequest(
                employee, request.type(), request.startDate(), request.endDate(), request.reason());
        if (leave.getType() == LeaveType.PAID && leave.getWorkingDays() > employee.getLeaveBalance()) {
            throw ApiException.conflict("Solde de congés insuffisant");
        }
        return LeaveResponse.from(leaveRepository.save(leave));
    }

    public LeaveResponse cancel(Long employeeId, Long leaveId) {
        LeaveRequest leave = getLeave(leaveId);
        if (!leave.getEmployee().getId().equals(employeeId)) {
            throw ApiException.forbidden("Cette demande ne vous appartient pas");
        }
        leave.cancel();
        return LeaveResponse.from(leave);
    }

    // ---- Manager side ----

    @Transactional(readOnly = true)
    public List<LeaveResponse> findAll(LeaveStatus status) {
        List<LeaveRequest> leaves = status == null
                ? leaveRepository.findAllByOrderByStartDateDesc()
                : leaveRepository.findByStatusOrderByStartDateAsc(status);
        return leaves.stream().map(LeaveResponse::from).toList();
    }

    public LeaveResponse approve(Long managerId, Long leaveId, String comment) {
        LeaveRequest leave = getLeave(leaveId);
        leave.approve(getUser(managerId), comment);
        return LeaveResponse.from(leave);
    }

    public LeaveResponse reject(Long managerId, Long leaveId, String comment) {
        LeaveRequest leave = getLeave(leaveId);
        leave.reject(getUser(managerId), comment);
        return LeaveResponse.from(leave);
    }

    private LeaveRequest getLeave(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> ApiException.notFound("Demande introuvable"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }
}
