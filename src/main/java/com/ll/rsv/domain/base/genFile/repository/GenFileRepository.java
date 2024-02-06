package com.ll.rsv.domain.base.genFile.repository;

import com.ll.rsv.domain.base.genFile.entity.GenFile.GenFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenFileRepository extends JpaRepository<GenFile, Long> {
    List<GenFile> findByRelTypeCodeAndRelId(String relTypeCode, long relId);

    Optional<GenFile> findByFileName(String fileName);
}
