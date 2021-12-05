package io.roach.workload.common.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class ResourceSupport {
    private ResourceSupport() {
    }

    public static String resourceAsString(String path) {
        try (Reader reader = new InputStreamReader(new ClassPathResource(path).getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
