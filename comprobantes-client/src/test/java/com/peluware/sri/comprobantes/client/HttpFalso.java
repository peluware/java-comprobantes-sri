package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/**
 * Transporte falso que captura la petición y devuelve una respuesta fija.
 */
final class HttpFalso extends HttpClient {

    private final int estado;
    private final byte[] respuesta;
    private final @Nullable IOException fallo;

    @Nullable HttpRequest peticion;

    HttpFalso(int estado, byte[] respuesta) {
        this.estado = estado;
        this.respuesta = respuesta;
        this.fallo = null;
    }

    HttpFalso(@Nullable IOException fallo) {
        this.estado = 0;
        this.respuesta = new byte[0];
        this.fallo = fallo;
    }

    /**
     * El cuerpo de la petición que se capturó, o cadena vacía si no hubo ninguna.
     */
    String cuerpo() {
        var enviada = peticion;
        return enviada == null ? "" : enviada.bodyPublisher().map(HttpFalso::leer).orElse("");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        this.peticion = request;
        if (fallo != null) {
            throw fallo;
        }
        return (HttpResponse<T>) new RespuestaFalsa(request, estado, respuesta);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> promise) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }

    @Override
    public Redirect followRedirects() {
        return Redirect.NEVER;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.empty();
    }

    @Override
    public SSLContext sslContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLParameters sslParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }

    @Override
    public Version version() {
        return Version.HTTP_1_1;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }

    private static String leer(HttpRequest.BodyPublisher publisher) {
        var recolector = new Recolector();
        publisher.subscribe(recolector);
        return recolector.texto();
    }

    private static final class Recolector implements Flow.Subscriber<ByteBuffer> {

        private final ByteArrayOutputStream acumulado = new ByteArrayOutputStream();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
            byte[] trozo = new byte[item.remaining()];
            item.get(trozo);
            acumulado.writeBytes(trozo);
        }

        @Override
        public void onError(Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        @Override
        public void onComplete() {
        }

        private String texto() {
            return acumulado.toString(StandardCharsets.UTF_8);
        }
    }

    private record RespuestaFalsa(HttpRequest request, int statusCode, byte[] body) implements HttpResponse<byte[]> {

        @Override
        public Optional<HttpResponse<byte[]>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of("Content-Type", List.of("text/xml")), (a, b) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return Objects.requireNonNull(request, "request").uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
