package com.loveeleve.ecology.web.service;

import com.loveeleve.ecology.web.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.demo.upload-dir:${java.io.tmpdir}/java-ecology-uploads}") String uploadDir)
            throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "file must not be empty");
        }
        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String safeName = StringUtils.getFilename(original);
        if (!StringUtils.hasText(safeName)) {
            safeName = "file";
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "-" + safeName;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FILE_NAME", "invalid file name");
        }
        try {
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORE_FAILED", "failed to store file");
        }
        return new StoredFile(storedName, safeName, file.getSize(), file.getContentType());
    }

    public Resource load(String storedName) {
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "file not found");
        }
        return new FileSystemResource(target);
    }

    public record StoredFile(String storedName, String originalName, long size, String contentType) {
    }
}
