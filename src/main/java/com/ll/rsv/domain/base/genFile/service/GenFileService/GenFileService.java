package com.ll.rsv.domain.base.genFile.service.GenFileService;

import com.ll.rsv.domain.base.genFile.entity.GenFile.GenFile;
import com.ll.rsv.domain.base.genFile.repository.GenFileRepository;
import com.ll.rsv.global.app.AppConfig;
import com.ll.rsv.global.jpa.entity.BaseEntity;
import com.ll.rsv.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GenFileService {
    private final GenFileRepository genFileRepository;

    @Transactional
    public void save(BaseEntity entity, String typeCode, String type2Code, int fileNo, MultipartFile file) {
        String filePath = Ut.file.toFile(file, AppConfig.getTempDirPath());

        GenFile genFile = save(
                entity.getModelName(),
                entity.getId(),
                typeCode,
                type2Code,
                fileNo,
                filePath
        );

        System.out.println("filePath = " + filePath); // TODO : remove
    }

    private String getCurrentDirName(String relTypeCode) {
        return relTypeCode + "/" + Ut.date.getCurrentDateFormatted("yyyy_MM_dd");
    }

    @Transactional
    public GenFile save(String relTypeCode, long relId, String typeCode, String type2Code, int fileNo, String filePath) {
        String originFileName = Ut.file.getOriginFileName(filePath);
        String fileExt = Ut.file.getExt(originFileName);
        String fileExtTypeCode = Ut.file.getFileExtTypeCodeFromFileExt(fileExt);
        String fileExtType2Code = Ut.file.getFileExtType2CodeFromFileExt(fileExt);
        long fileSize = new File(filePath).length();
        String fileDir = getCurrentDirName(relTypeCode);
        String fileName = UUID.randomUUID() + "." + fileExt;

        GenFile genFile = GenFile.builder()
                .fileName(fileName)
                .relTypeCode(relTypeCode)
                .relId(relId)
                .typeCode(typeCode)
                .type2Code(type2Code)
                .fileNo(fileNo)
                .fileExtTypeCode(fileExtTypeCode)
                .fileExtType2Code(fileExtType2Code)
                .fileSize(fileSize)
                .fileExt(fileExt)
                .fileDir(fileDir)
                .originFileName(originFileName)
                .build();

        genFileRepository.save(genFile);

        File file = new File(genFile.getFilePath());

        file.getParentFile().mkdirs();

        Ut.file.moveFile(filePath, file);
        Ut.file.remove(filePath);

        return genFile;
    }

    public List<GenFile> findByRel(BaseEntity entity) {
        return genFileRepository.findByRelTypeCodeAndRelId(entity.getModelName(), entity.getId());
    }

    public Optional<GenFile> findByFileName(String fileName) {
        return genFileRepository.findByFileName(fileName);
    }
}
