package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.auth.AccessDtos;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.ExperimentPermissionRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.ExperimentPermissionRecordRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExperimentPermissionsService {

    private final ExperimentPermissionRecordRepository experimentPermissionRecordRepository;
    private final ExperimentRecordRepository experimentRecordRepository;
    private final AppUserRepository userRepository;
    private final AuthorizationService authorizationService;

    public ExperimentPermissionsService(
            ExperimentPermissionRecordRepository experimentPermissionRecordRepository,
            ExperimentRecordRepository experimentRecordRepository,
            AppUserRepository userRepository,
            AuthorizationService authorizationService
    ) {
        this.experimentPermissionRecordRepository = experimentPermissionRecordRepository;
        this.experimentRecordRepository = experimentRecordRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public List<AccessDtos.ExperimentAccessGrantDTO> listAccess(AppUserRecord currentUser, Long experimentId) {
        authorizationService.assertCanManageExperiment(currentUser, experimentId);
        return experimentPermissionRecordRepository.findAllByExperiment_Id(experimentId)
                .stream()
            .map(grant -> {
                AppUserRecord user = grant.getUser();
                if (user == null) {
                return null;
                }
                return new AccessDtos.ExperimentAccessGrantDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    grant.getAccessLevel()
                );
            })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public List<AccessDtos.ExperimentAccessGrantDTO> upsertAccess(
            AppUserRecord currentUser,
            Long experimentId,
            AuthDtos.ExperimentAccessGrantRequest request
    ) {
        authorizationService.assertCanManageExperiment(currentUser, experimentId);
        AppUserRecord targetUser = resolveTargetUser(request.userId(), request.email());
        ResourceAccessLevel accessLevel = request.accessLevel() == null ? ResourceAccessLevel.VIEWER : request.accessLevel();

        grantAccess(experimentId, targetUser.getId(), accessLevel, currentUser.getId());
        return listAccess(currentUser, experimentId);
    }

    @Transactional
    void grantAccess(Long experimentId, String userId, ResourceAccessLevel accessLevel, String grantedByUserId) {
        ExperimentRecord experiment = experimentRecordRepository.getReferenceById(experimentId);
        ExperimentPermissionRecord grant = experimentPermissionRecordRepository
                .findByExperiment_IdAndUser_Id(experimentId, userId)
                .orElseGet(ExperimentPermissionRecord::new);
        grant.setExperiment(experiment);
        grant.setUser(userRepository.getReferenceById(userId));
        grant.setAccessLevel(accessLevel);
        grant.setGrantedByUser(userRepository.getReferenceById(grantedByUserId));
        experimentPermissionRecordRepository.save(grant);
    }

    @Transactional
    public void removeAccess(AppUserRecord currentUser, Long experimentId, String userId) {
        authorizationService.assertCanManageExperiment(currentUser, experimentId);
        experimentPermissionRecordRepository.deleteByExperiment_IdAndUser_Id(experimentId, userId);
    }

    private AppUserRecord resolveTargetUser(String userId, String email) {
        if (StringUtils.hasText(userId)) {
            return userRepository.findById(userId.trim())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        }

        if (StringUtils.hasText(email)) {
            return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        }

        throw new ResponseStatusException(BAD_REQUEST, "User id or email is required");
    }
}
