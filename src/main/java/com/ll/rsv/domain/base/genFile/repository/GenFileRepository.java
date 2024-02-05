package com.ll.rsv.domain.base.genFile.repository;

import com.ll.rsv.domain.base.genFile.entity.GenFile.GenFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenFileRepository extends JpaRepository<GenFile, Long> {
}
