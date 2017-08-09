package cz.sparko.boxitory.service;

import cz.sparko.boxitory.conf.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Objects;

public class FilesystemDigestHashService implements HashService {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemDigestHashService.class);
    private MessageDigest messageDigest;
    private int streamBufferLength;

    public FilesystemDigestHashService(MessageDigest messageDigest, AppProperties appProperties) {
        this.messageDigest = messageDigest;
        streamBufferLength = appProperties.getChecksum_buffer_size();
    }

    @Override
    public String getHashType() {
        return messageDigest.getAlgorithm().replaceAll("-", "").toLowerCase();
    }

    @Override
    public String getChecksum(String string) {
        try (InputStream boxDataStream = Files.newInputStream(new File(string).toPath())) {
            LOG.trace("buffering box data (buffer size [{}]b) ...", streamBufferLength);
            final byte[] buffer = new byte[streamBufferLength];
            int read = boxDataStream.read(buffer, 0, streamBufferLength);

            while (read > -1) {
                messageDigest.update(buffer, 0, read);
                read = boxDataStream.read(buffer, 0, streamBufferLength);
            }
        } catch (IOException e) {
            LOG.error("Error during processing file [{}], message: [{}]", string, e.getMessage());
            throw new RuntimeException(
                    "Error while getting checksum for file " + string + " reason: " + e.getMessage(), e
            );
        }

        return getHash(messageDigest.digest());
    }

    private String getHash(byte[] diggestBytes) {
        return DatatypeConverter.printHexBinary(diggestBytes).toLowerCase();
    }

    @Override
    public String toString() {
        return "FilesystemDigestHashService{" +
                "messageDigest=" + messageDigest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        FilesystemDigestHashService that = (FilesystemDigestHashService) o;
        return messageDigest.getAlgorithm().equals(that.messageDigest.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageDigest);
    }
}