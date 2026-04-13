package com.bekri.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionFailureAnalyzerTest {

    private final DatabaseConnectionFailureAnalyzer analyzer = new DatabaseConnectionFailureAnalyzer();

    @Test
    void analyzeReturnsFailureAnalysisForSqlException() {
        SQLException sqlException = new SQLException("Connection refused");

        FailureAnalysis analysis = analyzer.analyze(sqlException);

        assertNotNull(analysis);
        assertSame(sqlException, analysis.getCause());
        assertTrue(analysis.getDescription().contains("Connexion à la base MySQL"));
    }

    @Test
    void analyzeReturnsFailureAnalysisForJdbcKeywordsInNestedCause() {
        RuntimeException root = new RuntimeException("Failed to obtain JDBC connection");
        RuntimeException wrapper = new RuntimeException("Startup failed", root);

        FailureAnalysis analysis = analyzer.analyze(wrapper);

        assertNotNull(analysis);
        assertSame(wrapper, analysis.getCause());
    }

    @Test
    void analyzeReturnsNullForUnrelatedFailure() {
        IllegalStateException failure = new IllegalStateException("Something else happened");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNull(analysis);
    }
}
