package com.whatsforlunch.app.data.ai

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class AiFailureTest {

    @Test
    fun ioExceptionsAreClassifiedAsNoConnection() {
        val failures = listOf(
            IOException("socket closed"),
            UnknownHostException("api.example.com"),
            ConnectException("connection refused"),
            SocketTimeoutException("timeout"),
        )

        failures.forEach { exception ->
            assertEquals(AiFailureCause.NO_CONNECTION, AiFailure.fromException(exception))
        }
    }

    @Test
    fun nonIoExceptionsFallBackToInvalidResponse() {
        val failures = listOf(
            IllegalStateException("unexpected state"),
            org.json.JSONException("bad json"),
            RuntimeException("boom"),
        )

        failures.forEach { exception ->
            assertEquals(AiFailureCause.INVALID_RESPONSE, AiFailure.fromException(exception))
        }
    }
}
