/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android.net;

import android.content.Context;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasIOException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by Andrea Tortorella on 08/07/14.
 */
public class OkClient implements RestClient{

    private static final byte[] ZERO_BYTES=new byte[0];

    private OkHttpClient mOkHttp;
    private String charset;
    public OkClient() { this(new OkHttpClient());}

    @Deprecated
    public OkClient(BaasBox.Config config){
        this(new OkHttpClient());
    }

    public OkClient(OkHttpClient client){
        mOkHttp = client;
    }

    @Override
    public void init(Context context, BaasBox.Config config) {
        this.charset=config.httpCharset;
        mOkHttp.setConnectTimeout(config.httpConnectionTimeout, TimeUnit.MILLISECONDS);
        mOkHttp.setReadTimeout(config.httpSocketTimeout,TimeUnit.MILLISECONDS);
        mOkHttp.setFollowSslRedirects(true);
    }

    private static class InputRequestBody extends RequestBody{
        MediaType media;
        Source in;
        InputRequestBody(String ct,InputStream in){
            this.media=MediaType.parse(ct);
            this.in= Okio.source(in);
        }

        @Override
        public MediaType contentType() {
            return media;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            sink.writeAll(in);
        }
    }

    private RequestBody buildBody(String contentType,InputStream bodyData) {
        if (bodyData==null){
            return RequestBody.create(MediaType.parse("application/json;charset=" + charset), "{}");
        } else {
            return new InputRequestBody(contentType,bodyData);
        }
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws BaasException {
        String contentType = request.headers.get("Content-Type");
        Request.Builder okRequestBuilder = new Request.Builder();
        boolean contentLengthSet = false;
        for (String name: request.headers.keySet()){
            if (!contentLengthSet &&"Content-Length".equals(name)){
                contentLengthSet = true;
            }
            okRequestBuilder.addHeader(name,request.headers.get(name));
        }
        if (!contentLengthSet){
            okRequestBuilder.addHeader("Content-Length","0");
        }
        RequestBody rb;
        switch (request.method){
            case HttpRequest.GET:
                okRequestBuilder.get();
                break;
            case HttpRequest.POST:
                rb = buildBody(contentType,request.body);
                //InputRequestBody rb = new InputRequestBody(contentType,request.body);
                okRequestBuilder.post(rb);
                break;
            case HttpRequest.PUT:
                rb = buildBody(contentType,request.body);
                okRequestBuilder.put(rb);
                break;
            case HttpRequest.DELETE:
                okRequestBuilder.delete();
                break;
            case HttpRequest.PATCH:
                rb = buildBody(contentType,request.body);
                okRequestBuilder.patch(rb);
                break;

        }

        okRequestBuilder.url(request.url);
        Request okRequest=okRequestBuilder.build();
        try {
            Response resp = mOkHttp.newCall(okRequest).execute();
            Protocol protocol = resp.protocol();
            ProtocolVersion pv;
            switch (protocol){
                case HTTP_1_0:
                    pv = new ProtocolVersion("HTTP",1,0);
                    break;
                case HTTP_1_1:
                    pv = new ProtocolVersion("HTTP",1,1);
                    break;
                case HTTP_2:
                    pv = new ProtocolVersion("HTTP",2,0);
                    break;
                case SPDY_3:
                    pv = new ProtocolVersion("spdy",3,1);
                    break;
                default:
                    throw new BaasIOException("Invalid protocol");
            }
            StatusLine line = new BasicStatusLine(pv,resp.code(),resp.message());
            BasicHttpResponse bresp = new BasicHttpResponse(line);
            bresp.setEntity(asEntity(resp));

            for (String name:resp.headers().names()){
                String val = resp.headers().get(name);
                bresp.addHeader(name,val);
            }
            return bresp;
        } catch (IOException e) {
            throw new BaasIOException(e);
        }
    }

    private HttpEntity asEntity(Response resp) throws IOException{
        BasicHttpEntity entity =new BasicHttpEntity();
        InputStream inputStream = resp.body().byteStream();
        entity.setContent(inputStream);
        entity.setContentLength(resp.body().contentLength());
        String ctnt=resp.body().contentType().toString();
        entity.setContentType(ctnt);
        return entity;
    }
}
