package cc.cornerstones.almond.utils;

import cc.cornerstones.archimedes.extensions.types.DecodedFileId;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class AbcFileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbcFileUtils.class);

    public static final String ILLEGAL_FILE_NAME_REGEX = "[\\s\\\\/:\\*\\?\\\"<>\\|]";

    public static String readContent(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(inputStreamReader);
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
    }

    /**
     * 读取文本文件内容
     *
     * @param file
     * @return
     */
    public static String readContent(File file, Charset charset) {

        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder sb = null;
        try {
            fileInputStream = new FileInputStream(file);

            inputStreamReader = new InputStreamReader(fileInputStream, charset);

            bufferedReader = new BufferedReader(inputStreamReader);

            sb = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return null;
    }

    public static void zipFile(String srcFilePath, String destFilePath) {
        FileInputStream fis = null;
        ZipOutputStream zos = null;

        File file = new File(srcFilePath);
        try {
            fis = new FileInputStream(srcFilePath);
            FileOutputStream fos = new FileOutputStream(destFilePath);
            zos = new ZipOutputStream(fos);

            zos.putNextEntry(new ZipEntry(file.getName()));

            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        } catch (IOException e) {
            LOGGER.error("fail to compress src:" + srcFilePath + " to dest:" + destFilePath, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将存放在sourceFilePath目录下的源文件,打包成fileName名称的ZIP文件,并存放到zipFilePath。
     *
     * @param sourceDirectoryPath 待压缩的文件路径
     * @param zipDirectoryPath    压缩后存放路径
     * @param zipFileName         压缩后文件的名称
     * @return flag
     */
    public static void zipDirectory(
            String sourceDirectoryPath,
            String zipDirectoryPath,
            String zipFileName) {
        File sourceDirectoryFile = new File(sourceDirectoryPath);
        Path archivePath = Paths.get(zipDirectoryPath, zipFileName);
        FileOutputStream archiveFileOutputStream = null;
        ZipArchiveOutputStream archiveOutputStream = null;
        try {
            archiveFileOutputStream = new FileOutputStream(archivePath.toString());
            archiveOutputStream = (ZipArchiveOutputStream) new ArchiveStreamFactory()
                    .createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveFileOutputStream);

            archiveOutputStream.setEncoding("utf-8");
            archiveOutputStream.setUseZip64(Zip64Mode.AsNeeded);
            Collection<File> files = FileUtils.listFilesAndDirs(sourceDirectoryFile,
                    TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

            for (File file : files) {
                InputStream in = null;
                try {
                    if (file.getPath().equals(sourceDirectoryFile.getPath())) {
                        continue;
                    }
                    String relativePath = StringUtils.replace(file.getPath(), sourceDirectoryPath + File.separator, "");
                    ZipArchiveEntry entry = new ZipArchiveEntry(file, relativePath);
                    archiveOutputStream.putArchiveEntry(entry);
                    if (file.isDirectory()) {
                        continue;
                    }

                    in = new FileInputStream(file);
                    IOUtils.copy(in, archiveOutputStream);
                    archiveOutputStream.closeArchiveEntry();
                } finally {
                    if (in != null) {
                        IOUtils.closeQuietly(in);
                    }
                }
            }
            archiveOutputStream.finish();
        } catch (ArchiveException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recursivelyZipDirectory(String inputDirectoryPath, String outputFilePath) {
        File inputDirectoryFile = new File((inputDirectoryPath));
        File outputFile = new File(outputFilePath);
        outputFile.getParentFile().mkdirs();

        List<File> files = new LinkedList<>();
        recursivelyCollectFiles(inputDirectoryFile, files);

        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
            createZipFile(files, inputDirectoryFile, zipOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void recursivelyCollectFiles(File inputDirectoryFile, List<File> files) {
        File[] allFiles = inputDirectoryFile.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            return;
        }
        for (File file : allFiles) {
            if (file.isDirectory()) {
                recursivelyCollectFiles(file, files);
            } else {
                files.add(file);
            }
        }
    }

    private static void createZipFile(
            List<File> files,
            File inputDirectoryFile,
            ZipOutputStream zipOutputStream) throws IOException {
        for (File file : files) {
            String filePath = file.getCanonicalPath();
            int lengthDirectoryPath = inputDirectoryFile.getCanonicalPath().length();
            int lengthFilePath = file.getCanonicalPath().length();

            // Get path of files relative to input directory.
            String zipFilePath = filePath.substring(lengthDirectoryPath + 1, lengthFilePath);

            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zipOutputStream.putNextEntry(zipEntry);

            FileInputStream inputStream = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = inputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            zipOutputStream.closeEntry();
            System.out.println("Zipped file:" + filePath);
        }
        zipOutputStream.close();
    }

    /**
     * 格式化文件长度，自适应用GB/MB/KB/B表示长度
     *
     * @param length
     * @return
     */
    public static String formatFileLength(Long length) {
        if (length == null) {
            return null;
        }

        String result;
        if (length > (1000 * 1000 * 1000)) {
            long integerPart = length / 1000 / 1000 / 1000;
            long remainder = length % (1000 * 1000 * 1000);
            if (remainder > 0) {
                result = integerPart + "." + remainder / (1000 * 1000 * 100) + "GB";
            } else {
                result = integerPart + "GB";
            }
        } else if (length > (1000 * 1000)) {
            long integerPart = length / 1000 / 1000;
            long remainder = length % (1000 * 1000);
            if (remainder > 0) {
                result = integerPart + "." + remainder / (1000 * 100) + "MB";
            } else {
                result = integerPart + "MB";
            }
        } else if (length > 1000) {
            long integerPart = length / 1000;
            long remainder = length % (1000);
            if (remainder > 0) {
                result = integerPart + "." + remainder / 100 + "KB";
            } else {
                result = integerPart + "KB";
            }
        } else {
            result = length + "B";
        }

        return result;
    }

    public static void recursivelyDeleteFile(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        if (path.toFile().isDirectory()) {
            File[] childFiles = path.toFile().listFiles();
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    recursivelyDeleteFile(childFile.toPath());
                }
            }
        }

        LOGGER.info("try deleting file {}", path.toFile().getAbsolutePath());

        path.toFile().delete();
    }

    public static String replaceIllegalCharactersWithEmpty(String name) {
        Pattern pattern = Pattern.compile(ILLEGAL_FILE_NAME_REGEX);
        Matcher matcher = pattern.matcher(name);

        // 将匹配到的非法字符以空替换
        return matcher.replaceAll("");
    }

    public static void unzipFile(String zipFilePath, String destinationDirectoryPath) throws IOException {
        File destinationDirectoryFile = new File(destinationDirectoryPath);

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destinationDirectoryFile, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destinationFile = new File(destinationDir, zipEntry.getName());

        String destinationDirPath = destinationDir.getCanonicalPath();
        String destinationFilePath = destinationFile.getCanonicalPath();

        if (!destinationFilePath.startsWith(destinationDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destinationFile;
    }

    public static void main(String[] args) {
        tc4();
    }

    private static void tc1() {
        String fileId = "YXJjaGltZWRlcyMjc2FtcGxlLWltYWdlXzEuanBlZzo6MjAyMjEyMDIxMTU0MDA1Nw==";

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

        System.out.println(decodedFileId);
    }

    private static void tc2() {
        String inputDirectoryPath = "/Users/bbottong/Downloads/sfa-gsp-main";
        String outputFilePath = "/Users/bbottong/Downloads/sfa-gsp-main-lzp.zip";

        recursivelyZipDirectory(inputDirectoryPath, outputFilePath);
    }

    public static void tc3() {
        String fileName = "b" +
                "/G" +
                ":9" +
                "*0dXMtZn" +
                "?MjI3d4d29ya19pbWdfcmFt\"eV>8zZTgwM<TRi|ODNlZjQ3NDQyNWFkZTA1Nzk0ODVmNDEwYzo6MjAyMjAyMjAxODQzNTkwNA" +
                "==\n" +
                ".jpg";

        String newFileName = replaceIllegalCharactersWithEmpty(fileName);

        System.out.print(newFileName);
    }

    public static void tc4() {
        String sourceDirectoryPath = "/Users/bbottong/tmp/archimedes-server/download/dsfasdfasdfasdfasd";
        String zipDirectoryPath = "/Users/bbottong/tmp/archimedes-server/";
        String zipFileName = "/Users/bbottong/tmp/archimedes-server/download/migration.zip";

        recursivelyZipDirectory(sourceDirectoryPath, zipFileName);
    }
}
