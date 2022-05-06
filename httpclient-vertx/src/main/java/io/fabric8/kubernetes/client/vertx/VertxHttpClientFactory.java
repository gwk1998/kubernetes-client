package io.fabric8.kubernetes.client.vertx;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.Interceptor;
import io.fabric8.kubernetes.client.http.TlsVersion;
import io.fabric8.kubernetes.client.http.WebSocket;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class VertxHttpClientFactory implements io.fabric8.kubernetes.client.http.HttpClient.Factory {

  private Vertx vertx;

  public VertxHttpClientFactory() {
    this.vertx = Vertx.vertx();
  }

  @Override
  public io.fabric8.kubernetes.client.http.HttpClient createHttpClient(Config config) {
    io.fabric8.kubernetes.client.http.HttpClient.Builder builder = newBuilder();
    return builder.build();
  }

  private final class VertxHttpClientBuilder implements io.fabric8.kubernetes.client.http.HttpClient.Builder {
    private final HttpClientOptions options;

    private VertxHttpClientBuilder(HttpClientOptions options) {
      this.options = options;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient build() {
      return new VertxHttpClient(options);
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder readTimeout(long l, TimeUnit timeUnit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder connectTimeout(long l, TimeUnit timeUnit) {
      options.setConnectTimeout((int) TimeUnit.MILLISECONDS.convert(l, timeUnit));
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder forStreaming() {
      // TODO: confirm not needed
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder writeTimeout(long l, TimeUnit timeUnit) {
      // TODO: confirm not needed
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder addOrReplaceInterceptor(String s, Interceptor interceptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder authenticatorNone() {
      // TODO: confirm not needed
      return this;
    }

    @Override
    public Builder sslContext(KeyManager[] keyManagers, TrustManager[] trustManagers) {
      if (trustManagers.length > 0) {
        options.setTrustOptions(TrustOptions.wrap(trustManagers[0]));
      }
      if (keyManagers.length > 0) {
        options.setKeyCertOptions(KeyCertOptions.wrap((X509KeyManager) keyManagers[0]));
      }
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder followAllRedirects() {
      // TODO: confirm not needed
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder proxyAddress(InetSocketAddress inetSocketAddress) {
      throw new UnsupportedOperationException();
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder proxyAuthorization(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder tlsVersions(TlsVersion[] tlsVersions) {
      if (tlsVersions != null && tlsVersions.length > 0) {
        Stream.of(tlsVersions).map(TlsVersion::javaName).forEach(tls -> options.addEnabledCipherSuite(tls));
      }
      return this;
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder preferHttp11() {
      this.options.setProtocolVersion(HttpVersion.HTTP_1_1);
      return this;
    }
  }

  private class VertxHttpClient implements io.fabric8.kubernetes.client.http.HttpClient {

    private HttpClient client;
    private HttpClientOptions options;

    private VertxHttpClient(HttpClientOptions options) {
      client = vertx.createHttpClient(options);
      this.options = options;
    }

    @Override
    public void close() {
      client.close();
    }

    @Override
    public DerivedClientBuilder newBuilder() {
      // TODO: this needs to share, not recreate the client
      // an abstract builder is probably needed
      return new VertxHttpClientBuilder(new HttpClientOptions(options));
    }

    @Override
    public CompletableFuture<HttpResponse<AsyncBody>> consumeBytes(HttpRequest request,
        BodyConsumer<List<ByteBuffer>> consumer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<HttpResponse<AsyncBody>> consumeLines(HttpRequest request, BodyConsumer<String> consumer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Factory getFactory() {
      return VertxHttpClientFactory.this;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest httpRequest, Class<T> clazz) {
      VertxHttpRequest vertxHttpRequest = (VertxHttpRequest) httpRequest;
      return vertxHttpRequest.sendAsync(client, clazz);
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
      return new WebSocket.Builder() {
        WebSocketConnectOptions options = new WebSocketConnectOptions();

        @Override
        public CompletableFuture<io.fabric8.kubernetes.client.http.WebSocket> buildAsync(WebSocket.Listener listener) {
          Future<io.fabric8.kubernetes.client.http.WebSocket> map = client
              .webSocket(options)
              .map(ws -> {
                VertxWebSocket ret = new VertxWebSocket(ws, listener);
                ret.init();
                return ret;
              });
          return map.toCompletionStage().toCompletableFuture();
        }

        @Override
        public WebSocket.Builder subprotocol(String protocol) {
          options.setSubProtocols(Collections.singletonList(protocol));
          return this;
        }

        @Override
        public WebSocket.Builder header(String name, String value) {
          options.addHeader(name, value);
          return this;
        }

        @Override
        public WebSocket.Builder setHeader(String k, String v) {
          options.putHeader(k, v);
          return this;
        }

        @Override
        public WebSocket.Builder uri(URI uri) {
          options.setAbsoluteURI(uri.toString());
          return this;
        }
      };
    }

    @Override
    public HttpRequest.Builder newHttpRequestBuilder() {
      return new HttpRequest.Builder() {

        private URI uri;
        private RequestOptions options = new RequestOptions();
        private Buffer body;

        @Override
        public HttpRequest build() {
          return new VertxHttpRequest(uri, new RequestOptions(options).setAbsoluteURI(uri.toString()), body);
        }

        @Override
        public HttpRequest.Builder uri(String uri) {
          return uri(URI.create(uri));
        }

        @Override
        public HttpRequest.Builder url(URL url) {
          return uri(url.toString());
        }

        @Override
        public HttpRequest.Builder uri(URI uri) {
          this.uri = uri;
          return this;
        }

        @Override
        public HttpRequest.Builder post(String contentType, byte[] bytes) {
          options.setMethod(HttpMethod.POST);
          options.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
          body = Buffer.buffer(bytes);
          return this;
        }

        @Override
        public HttpRequest.Builder post(String contentType, InputStream stream, long length) {
          // The api supports two calls here, the user passing in an arbitrary inputstream
          // or a file - we could split off the file handling
          // The inputstream seems problematic - seems like it needs converted into a ReadStream
          
          options.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
          throw new UnsupportedOperationException();
        }

        @Override
        public HttpRequest.Builder method(String method, String contentType, String s) {
          options.setMethod(HttpMethod.valueOf(method));
          options.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
          body = Buffer.buffer(s);
          return this;
        }

        @Override
        public HttpRequest.Builder header(String k, String v) {
          options.addHeader(k, v);
          return this;
        }

        @Override
        public HttpRequest.Builder setHeader(String k, String v) {
          options.putHeader(k, v);
          return this;
        }

        @Override
        public HttpRequest.Builder expectContinue() {
          // TODO: determine if this is enforced by the client
          options.putHeader("Expect", "100-continue");
          return this;
        }
      };
    }
  }

  @Override
  public io.fabric8.kubernetes.client.http.HttpClient.Builder newBuilder() {
    HttpClientOptions options = new HttpClientOptions();
    return new VertxHttpClientBuilder(options);
  }

}