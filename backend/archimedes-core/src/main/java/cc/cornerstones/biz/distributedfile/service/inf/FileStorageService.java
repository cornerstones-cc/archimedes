package cc.cornerstones.biz.distributedfile.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileStorageService {
    Resource loadFileAsResource(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    String storeFile(
            MultipartFile file,
            String homeDirectory,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    String storeFile(
            File file,
            String homeDirectory,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    File getFile(
            String fileId,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
