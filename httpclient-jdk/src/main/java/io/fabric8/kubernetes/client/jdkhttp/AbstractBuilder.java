/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubernetes.client.jdkhttp;

import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.http.Interceptor;
import io.fabric8.kubernetes.client.http.TlsVersion;
import io.fabric8.kubernetes.client.internal.SSLUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBuilder implements Builder {

  protected LinkedHashMap<String, Interceptor> interceptors = new LinkedHashMap<>();
  protected Duration connectTimeout;
  protected Duration readTimeout;
  protected SSLContext sslContext;
  protected String proxyAuthorization;
  protected InetSocketAddress proxyAddress;
  protected boolean followRedirects;
  protected boolean preferHttp11;
  protected TlsVersion[] tlsVersions;
  protected boolean forStreaming;
  protected Duration writeTimeout;
  protected boolean authenticatorNone;

  @Override
  public Builder readTimeout(long readTimeout, TimeUnit unit) {
    if (readTimeout == 0) {
      this.readTimeout = null;
    } else {
      this.readTimeout = Duration.ofNanos(unit.toNanos(readTimeout));
    }
    return this;
  }

  @Override
  public Builder connectTimeout(long connectTimeout, TimeUnit unit) {
    this.connectTimeout = Duration.ofNanos(unit.toNanos(connectTimeout));
    return this;
  }

  @Override
  public Builder forStreaming() {
    this.forStreaming = true;
    return this;
  }

  @Override
  public Builder writeTimeout(long timeout, TimeUnit timeoutUnit) {
    if (timeout == 0) {
      this.writeTimeout = null;
    } else {
      this.writeTimeout = Duration.ofNanos(timeoutUnit.toNanos(timeout));
    }
    return this;
  }

  @Override
  public Builder addOrReplaceInterceptor(String name, Interceptor interceptor) {
    if (interceptor == null) {
      interceptors.remove(name);
    } else {
      interceptors.put(name, interceptor);
    }
    return this;
  }

  @Override
  public Builder authenticatorNone() {
    this.authenticatorNone = true;
    return this;
  }

  @Override
  public Builder sslContext(KeyManager[] keyManagers, TrustManager[] trustManagers) {
    this.sslContext = SSLUtils.sslContext(keyManagers, trustManagers);
    return this;
  }

  @Override
  public Builder followAllRedirects() {
    this.followRedirects = true;
    return this;
  }

  @Override
  public Builder proxyAddress(InetSocketAddress proxyAddress) {
    this.proxyAddress = proxyAddress;
    return this;
  }

  @Override
  public Builder proxyAuthorization(String credentials) {
    this.proxyAuthorization = credentials;
    return this;
  }

  @Override
  public Builder preferHttp11() {
    this.preferHttp11 = true;
    return this;
  }

  @Override
  public Builder tlsVersions(TlsVersion[] tlsVersions) {
    this.tlsVersions = tlsVersions;
    return this;
  }

  protected void copyTo(AbstractBuilder copy) {
    copy.connectTimeout = this.connectTimeout;
    copy.readTimeout = this.readTimeout;
    copy.sslContext = this.sslContext;
    copy.interceptors = new LinkedHashMap<>(this.interceptors);
    copy.followRedirects = this.followRedirects;
    copy.proxyAddress = this.proxyAddress;
    copy.proxyAuthorization = this.proxyAuthorization;
    copy.tlsVersions = this.tlsVersions;
    copy.preferHttp11 = this.preferHttp11;
    copy.followRedirects = this.followRedirects;
    copy.forStreaming = this.forStreaming;
    copy.writeTimeout = this.writeTimeout;
    copy.authenticatorNone = this.authenticatorNone;
  }

}