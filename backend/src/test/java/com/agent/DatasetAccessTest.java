package com.agent;

import com.agent.entity.DatasetEntity;
import com.agent.entity.UserEntity;
import com.agent.repository.DatasetRepository;
import com.agent.repository.UserRepository;
import com.agent.service.DatasetAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DatasetAccess")
class DatasetAccessTest {

    @Autowired private DatasetAccessService accessService;
    @Autowired private UserRepository userRepo;
    @Autowired private DatasetRepository datasetRepo;
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    private Long analystId;
    private Long dsId;

    @BeforeEach
    void seed() {
        UserEntity analyst = new UserEntity();
        analyst.setUsername("tester_" + System.nanoTime());
        analyst.setPasswordHash(enc.encode("x"));
        analyst.setRole(UserEntity.ROLE_ANALYST);
        analyst.setOrgId(0L);
        analyst.setIsEnabled(true);
        analystId = userRepo.save(analyst).getId();

        DatasetEntity ds = new DatasetEntity();
        ds.setName("授权测试集");
        ds.setTableName("access_test");
        ds.setOrgId(0L);
        ds.setIsEnabled(true);
        dsId = datasetRepo.save(ds).getId();
    }

    @Test
    @DisplayName("analyst cannot access dataset before grant")
    void noAccessBeforeGrant() {
        assertFalse(accessService.canAccess(analystId, dsId));
    }

    @Test
    @DisplayName("grant grants access, revoke removes it")
    void grantAndRevoke() {
        accessService.grant(analystId, dsId);
        assertTrue(accessService.canAccess(analystId, dsId));

        accessService.revoke(analystId, dsId);
        assertFalse(accessService.canAccess(analystId, dsId));
    }

    @Test
    @DisplayName("cross-org grant is rejected")
    void crossOrgRejected() {
        DatasetEntity otherOrg = new DatasetEntity();
        otherOrg.setName("他组数据");
        otherOrg.setTableName("other_org");
        otherOrg.setOrgId(99L); // different org
        otherOrg.setIsEnabled(true);
        Long otherDs = datasetRepo.save(otherOrg).getId();

        assertThrows(IllegalArgumentException.class, () -> accessService.grant(analystId, otherDs));
    }

    @Test
    @DisplayName("empty access list for new analyst")
    void emptyAccessList() {
        assertTrue(accessService.authorizedDatasetIds(analystId).isEmpty());
    }
}
