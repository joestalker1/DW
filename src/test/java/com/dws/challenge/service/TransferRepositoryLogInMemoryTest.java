package com.dws.challenge.service;

import com.dws.challenge.domain.TransferLog;
import com.dws.challenge.repository.TransferRepositoryLogInMemory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferRepositoryLogInMemoryTest {
    @Test
    public void testAppendNewLog(){
        TransferRepositoryLogInMemory transferRepository = new TransferRepositoryLogInMemory();
        TransferLog log1 = new TransferLog("accId1","accId2", BigDecimal.ZERO);
        transferRepository.append(log1);
        TransferLog log2 = new TransferLog("accId1","accId2", BigDecimal.valueOf(100));
        transferRepository.append(log2);
        assertThat(transferRepository.getAccountToTransfers().get("accId1:accId2").size()).isEqualTo(2);
    }

    @Test
    public void testSearchByTime() {
        TransferRepositoryLogInMemory transferRepository = new TransferRepositoryLogInMemory();
        TransferLog log1 = new TransferLog("accId1","accId2", BigDecimal.ZERO);
        log1.setUpdatedAt(LocalDateTime.MIN);
        transferRepository.append(log1);
        TransferLog log2 = new TransferLog("accId1","accId2", BigDecimal.valueOf(100));
        transferRepository.append(log2);
        log1.setUpdatedAt(LocalDateTime.MIN);
        assertThat(transferRepository.findFor("accId1", "accId2").size()).isEqualTo(2);
        assertThat(transferRepository.findFor("accId1", "accId2", LocalDateTime.MAX,LocalDateTime.MAX).size()).isEqualTo(0);
        assertThat(transferRepository.findFor("accId1", "accId2", LocalDateTime.MIN,LocalDateTime.MAX).size()).isEqualTo(2);
    }
}
