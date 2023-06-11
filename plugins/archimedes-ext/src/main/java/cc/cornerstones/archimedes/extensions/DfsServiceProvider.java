package cc.cornerstones.archimedes.extensions;

import cc.cornerstones.archimedes.extensions.types.DecodedFileId;
import org.pf4j.ExtensionPoint;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public abstract class DfsServiceProvider implements ExtensionPoint {
    public String getConfigurationTemplate() throws Exception {
        return null;
    }

    public File downloadFile(String fileId, String configuration) throws Exception {
        return null;
    }

    public String uploadFile(File file, String configuration) throws Exception {
        return null;
    }

    protected String encodeToFileId(String directoryName, String fileName) throws Exception {
        if (fileName == null || fileName.isEmpty() || fileName.trim().isEmpty()) {
            throw new Exception("illegal file name");
        }

        StringBuilder target = new StringBuilder();

        if (directoryName == null || directoryName.isEmpty()) {
            target.append("##");
        } else {
            target.append(directoryName).append("##");
        }

        target.append(fileName).append("::");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS"));

        target.append(timestamp);

        return Base64.getUrlEncoder().encodeToString(target.toString().getBytes(StandardCharsets.UTF_8));
    }

    protected DecodedFileId decodeFromFileId(String fileId) throws Exception {
        byte[] bytes = Base64.getUrlDecoder().decode(fileId);
        String target = new String(bytes, StandardCharsets.UTF_8);

        int directoryNameIndex = target.indexOf("##");
        String directoryName = "";
        if (directoryNameIndex > 0) {
            directoryName = target.substring(0, directoryNameIndex);
            target = target.substring(directoryName.length() + "##".length());
        } else if (directoryNameIndex == 0) {
            target = target.substring("##".length());
        }

        int fileNameIndex = target.indexOf("::");
        String fileName = "";
        if (fileNameIndex > 0) {
            fileName = target.substring(0, fileNameIndex);
            target = target.substring(fileName.length() + "::".length());
        } else if (fileNameIndex == 0) {
            target = target.substring("::".length());
        }

        String timestamp = target;

        DecodedFileId decodedFileId = new DecodedFileId();
        decodedFileId.setDirectoryName(directoryName);
        decodedFileId.setFileName(fileName);
        decodedFileId.setTimestamp(timestamp);
        return decodedFileId;
    }
}
