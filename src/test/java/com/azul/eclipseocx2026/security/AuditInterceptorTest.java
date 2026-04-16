package com.azul.eclipseocx2026.security;

import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

    @Mock InvocationContext ctx;

    AuditInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new AuditInterceptor();
        Field field = AuditInterceptor.class.getDeclaredField("maxQuestionLength");
        field.setAccessible(true);
        field.setInt(interceptor, 80);
    }

    @Test
    void success_logsAuditWithMetrics() throws Exception {
        when(ctx.proceed()).thenReturn("response text");
        when(ctx.getMethod()).thenReturn(String.class.getMethod("length"));
        when(ctx.getParameters()).thenReturn(new Object[]{"test question"});

        TestLogHandler handler = captureLogs(AuditInterceptor.class);
        Object result = interceptor.audit(ctx);

        assertEquals("response text", result);
        assertTrue(handler.lastMessage.contains("AUDIT"));
        assertTrue(handler.lastMessage.contains("status=success"));
        assertTrue(handler.lastMessage.contains("result_length=13"));
        assertTrue(handler.lastMessage.contains("duration="));
    }

    @Test
    void error_logsWarningAndRethrows() throws Exception {
        when(ctx.proceed()).thenThrow(new RuntimeException("LLM timeout"));
        when(ctx.getMethod()).thenReturn(String.class.getMethod("length"));
        when(ctx.getParameters()).thenReturn(new Object[]{"test"});

        TestLogHandler handler = captureLogs(AuditInterceptor.class);

        assertThrows(RuntimeException.class, () -> interceptor.audit(ctx));
        assertTrue(handler.lastMessage.contains("status=error"));
        assertTrue(handler.lastMessage.contains("LLM timeout"));
    }

    @Test
    void longParams_truncatedInLog() throws Exception {
        String longParam = "a".repeat(200);
        when(ctx.proceed()).thenReturn("ok");
        when(ctx.getMethod()).thenReturn(String.class.getMethod("length"));
        when(ctx.getParameters()).thenReturn(new Object[]{longParam});

        TestLogHandler handler = captureLogs(AuditInterceptor.class);
        interceptor.audit(ctx);

        assertTrue(handler.lastMessage.contains("a".repeat(80) + "..."));
        assertFalse(handler.lastMessage.contains("a".repeat(100)));
    }

    private TestLogHandler captureLogs(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        TestLogHandler handler = new TestLogHandler();
        logger.addHandler(handler);
        return handler;
    }

    private static class TestLogHandler extends Handler {
        String lastMessage;

        @Override
        public void publish(LogRecord record) {
            this.lastMessage = record.getMessage();
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
