package org.fcrepo.importexport.common;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class TarBagDeserializer implements BagDeserializer {

    private final Logger logger = LoggerFactory.getLogger(TarBagDeserializer.class);

    @Override
    public void deserialize(final Path root) throws IOException {
        final String regex = "\\.tar";
        final Pattern pattern = Pattern.compile(regex);
        final Path parent = root.getParent();
        final Path fileName = root.getFileName();

        final String trimmedName = pattern.matcher(fileName.toString()).replaceFirst("");
        logger.info("Extracting serialized bag {}", trimmedName);

        try (TarArchiveInputStream tais = new TarArchiveInputStream(Files.newInputStream(root))) {
            extract(tais, parent);
        }
    }
}
