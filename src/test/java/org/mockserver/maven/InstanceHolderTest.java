package org.mockserver.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockserver.cli.Main;
import org.mockserver.client.MockServerClient;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.echo.http.EchoServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.PortFactory;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author jamesdbloom
 */
public class InstanceHolderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldStartMockServer() {
        // given
        final int freePort = PortFactory.findFreePort();
        MockServerClient mockServerClient = new MockServerClient("127.0.0.1", freePort);
        Level originalLogLevel = ConfigurationProperties.logLevel();

        try {
            // when
            new InstanceHolder().start(
                    new Integer[]{freePort},
                    -1,
                    null,
                    "DEBUG",
                    null
            );

            // then
            assertThat(mockServerClient.isRunning(), is(true));
            assertThat(ConfigurationProperties.logLevel().toString(), is("DEBUG"));
        } finally {
            ConfigurationProperties.logLevel(originalLogLevel.toString());
            mockServerClient.stop();
        }
    }

    @Test
    public void shouldStartMockServerWithRemotePortAndHost() {
        // given
        final int freePort = PortFactory.findFreePort();
        MockServerClient mockServerClient = new MockServerClient("127.0.0.1", freePort);
        try {
            EchoServer echoServer = new EchoServer(false);
            echoServer.withNextResponse(response("port_forwarded_response"));

            // when
            new InstanceHolder().start(
                    new Integer[]{freePort},
                    echoServer.getPort(),
                    "127.0.0.1",
                    "DEBUG",
                    null
            );
            final HttpResponse response = new NettyHttpClient()
                    .sendRequest(
                            request()
                                    .withHeader(HOST.toString(), "127.0.0.1:" + freePort),
                            10,
                            TimeUnit.SECONDS
                    );

            // then
            assertThat(mockServerClient.isRunning(), is(true));
            assertThat(response.getBodyAsString(), is("port_forwarded_response"));
        } finally {
            mockServerClient.stop();
        }
    }

    @Test
    public void shouldStartMockServerWithRemotePort() {
        // given
        final int freePort = PortFactory.findFreePort();
        MockServerClient mockServerClient = new MockServerClient("127.0.0.1", freePort);
        try {
            EchoServer echoServer = new EchoServer(false);
            echoServer.withNextResponse(response("port_forwarded_response"));

            // when
            new InstanceHolder().start(
                    new Integer[]{freePort},
                    echoServer.getPort(),
                    "",
                    "DEBUG",
                    null
            );
            final HttpResponse response = new NettyHttpClient()
                    .sendRequest(
                            request()
                                    .withHeader(HOST.toString(), "127.0.0.1:" + freePort),
                            10,
                            TimeUnit.SECONDS
                    );

            // then
            assertThat(mockServerClient.isRunning(), is(true));
            assertThat(response.getBodyAsString(), is("port_forwarded_response"));
        } finally {
            mockServerClient.stop();
        }
    }

    @Test
    public void shouldPrintOutUsageForInvalidLogLevel() throws UnsupportedEncodingException {
        // given
        final int freePort = PortFactory.findFreePort();

        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("log level \"WRONG\" is not legal it must be one of \"TRACE\", \"DEBUG\", \"INFO\", \"WARN\", \"ERROR\", \"OFF\""));

        // when
        new InstanceHolder().start(
                new Integer[]{freePort},
                -1,
                "",
                "WRONG",
                null
        );
    }
}