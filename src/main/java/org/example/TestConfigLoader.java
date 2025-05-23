package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestConfigLoader {
    private static final String CONFIG_FILE = "/tests_config.json";

    public List<DiagnosticTest> loadTests() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IOException("Конфигурационные файлы не найдены: " + CONFIG_FILE);
            }
            TestConfig config = mapper.readValue(is, TestConfig.class);
            return config.getTests();
        }
    }

    private class TestConfig {
        private List<DiagnosticTest> tests;

        public List<DiagnosticTest> getTests() {
            return tests;
        }
    }
}