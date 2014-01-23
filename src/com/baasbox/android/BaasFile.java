/*
 * Copyright (C) 2014. BaasBox
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
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import android.webkit.MimeTypeMap;
import com.baasbox.android.impl.AsyncStream;
import com.baasbox.android.impl.NetworkTask;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.*;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by eto on 09/01/14.
 */
public class BaasFile extends BaasObject<BaasFile> {

    private JsonObject attachedData;
    private String mimeType;
    private String name;

    private String creationDate;
    private String id;
    private String author;
    private long contentLength;
    private long version;
    private final AtomicBoolean isBound = new AtomicBoolean();

    public BaasFile() {
        this(new JsonObject(), false);
    }

    public BaasFile(JsonObject attachedData) {
        this(attachedData, false);
    }

    BaasFile(JsonObject data, boolean fromServer) {
        super();
        if (fromServer) {
            this.attachedData = new JsonObject();
            update(data);
        } else {
            this.attachedData = data;
        }
    }


    void update(JsonObject fromServer) {
        isBound.set(true);
        this.attachedData.merge(fromServer.getObject("attachedData"));
        this.id = fromServer.getString("id");
        this.creationDate = fromServer.getString("_creation_date");
        this.author = fromServer.getString("_author");
        this.name = fromServer.getString("fileName");
        this.contentLength = fromServer.getLong("contentLength");
        this.version = fromServer.getLong("@version");
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public String getCreationDate() {
        return creationDate;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentType() {
        return mimeType;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public static RequestToken fetchAll(Filter filter, Priority priority, BaasHandler<List<BaasFile>> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Files files = new Files(box, filter, priority, handler);
        return box.submitAsync(files);
    }

    public static RequestToken fetchAll(Filter filter, BaasHandler<List<BaasFile>> handler) {
        return fetchAll(filter, null, handler);
    }

    public static RequestToken fetchAll(BaasHandler<List<BaasFile>> handler) {
        return fetchAll(null, null, handler);
    }

    public static BaasResult<List<BaasFile>> fetchAllSync() {
        return fetchAllSync(null);
    }

    public static BaasResult<List<BaasFile>> fetchAllSync(Filter filter) {
        BaasBox box = BaasBox.getDefaultChecked();
        Files files = new Files(box, filter, null, null);
        return box.submitSync(files);
    }

    public static RequestToken fetch(String id, BaasHandler<BaasFile> handler) {
        return fetch(id, Priority.NORMAL, handler);
    }

    public static RequestToken fetch(String id, Priority priority, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        BaasFile file = new BaasFile();
        file.id = id;
        Details details = new Details(box, file, priority, handler);
        return box.submitAsync(details);
    }

    public static BaasResult<BaasFile> fetchSync(String id) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        BaasFile file = new BaasFile();
        file.id = id;
        Details details = new Details(box, file, null, null);
        return box.submitSync(details);
    }

    public BaasResult<BaasFile> refreshSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new NullPointerException("this file is not bound to an entity on the server");
        Details details = new Details(box, this, null, null);
        return box.submitSync(details);
    }

    public RequestToken refresh(BaasHandler<BaasFile> handler) {
        return refresh(null, handler);
    }

    public RequestToken refresh(Priority priority, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        Details details = new Details(box, this, priority, handler);
        return box.submitAsync(details);
    }

    public BaasResult<Void> deleteSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalStateException("this file is not bounded to an entity on the server");
        Delete delete = new Delete(box, this, null, null);
        return box.submitSync(delete);
    }

    public static BaasResult<Void> deleteSync(String id) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        Delete delete = new Delete(box, id, null, null);
        return box.submitSync(delete);
    }

    public static RequestToken delete(String id, BaasHandler<Void> handler) {
        return delete(id, null, handler);
    }

    public RequestToken delete(BaasHandler<Void> handler) {
        return delete(Priority.NORMAL, handler);
    }

    public static RequestToken delete(String id, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        Delete delete = new Delete(box, id, priority, handler);
        return box.submitAsync(delete);
    }

    public RequestToken delete(Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        Delete delete = new Delete(box, this, priority, handler);
        return box.submitAsync(delete);
    }

    @Override
    public RequestToken revokeAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, true, id, role, grant, priority, handler);
        return box.submitAsync(access);
    }

    @Override
    public BaasResult<Void> revokeAllSync(Grant grant, String role) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, true, id, role, grant, null, null);
        return box.submitSync(access);
    }

    @Override
    public RequestToken revoke(Grant grant, String username, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, false, id, username, grant, priority, handler);
        return box.submitAsync(access);
    }

    @Override
    public BaasResult<Void> revokeSync(Grant grant, String user) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, false, id, user, grant, null, null);
        return box.submitSync(access);
    }

    @Override
    public RequestToken grantAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, true, id, role, grant, priority, handler);
        return box.submitAsync(access);
    }


    @Override
    public BaasResult<Void> grantAllSync(Grant grant, String role) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, true, id, role, grant, null, null);
        return box.submitSync(access);
    }


    @Override
    public BaasResult<Void> grantSync(Grant grant, String user) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, false, id, user, grant, null, null);
        return box.submitSync(access);
    }

    @Override
    public RequestToken grant(Grant grant, String username, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, false, id, username, grant, priority, handler);
        return box.submitAsync(access);
    }

    private final static class Access extends BaasObject.Access {

        protected Access(BaasBox box, boolean add, boolean isRole, String id, String to, Grant grant, Priority priority, BaasHandler<Void> handler) {
            super(box, add, isRole, null, id, to, grant, priority, handler);
        }

        @Override
        protected String userGrant(RequestFactory factory, Grant grant, String collection, String id, String to) {
            return factory.getEndpoint("file/?/?/user/?", id, grant.action, to);
        }

        @Override
        protected String roleGrant(RequestFactory factory, Grant grant, String collection, String id, String to) {
            return factory.getEndpoint("file/?/?/role/?", id, grant.action, to);
        }
    }

    public RequestToken upload(InputStream stream, Priority priority, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        RequestFactory factory = box.requestFactory;
        Upload req = uploadRequest(box, stream, priority, handler, new JsonObject());
        return box.submitAsync(req);
    }

    public BaasResult<BaasFile> uploadSync(InputStream stream) {
        BaasBox box = BaasBox.getDefaultChecked();
        Upload req = uploadRequest(box, stream, null, null, new JsonObject());
        return box.submitSync(req);
    }

    public RequestToken upload(InputStream stream, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Upload upload = uploadRequest(box, stream, null, handler, null);
        return box.submitAsync(upload);
    }

    public RequestToken upload(File file, Priority priority, BaasHandler<BaasFile> handler) {
        if (file == null) throw new NullPointerException("file cannot be null");
        try {

            FileInputStream fin = new FileInputStream(file);
            return upload(fin, priority, handler);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }

    public BaasResult<BaasFile> uploadSync(BaasACL acl, File file) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (file == null) throw new NullPointerException("file cannot be null");
        try {
            FileInputStream in = new FileInputStream(file);
            Upload req = uploadRequest(box, in, null, null, acl == null ? null : acl.toJson());
            return box.submitSync(req);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }


    public RequestToken upload(File file, BaasHandler<BaasFile> handler) {
        return upload(file, null, handler);
    }


    public RequestToken upload(byte[] bytes, Priority priority, BaasHandler<BaasFile> handler) {
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(in, priority, handler);
    }

    public BaasResult<BaasFile> uploadSync(byte[] bytes) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        Upload req = uploadRequest(box, in, null, null, new JsonObject());
        return box.submitSync(req);
    }

    public RequestToken upload(byte[] bytes, BaasHandler<BaasFile> handler) {
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(in, null, handler);
    }

    public static <R> RequestToken stream(String id, int size, Priority priority, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, size, priority, data, handler);
    }

    public static <R> RequestToken stream(String id, int size, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, size, null, data, handler);
    }

    public static <R> RequestToken stream(String id, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, -1, null, data, handler);
    }


    public static <R> RequestToken stream(String id, Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return stream(id, null, -1, priority, contentHandler, handler);
    }

    private static <R> RequestToken stream(String id, String sizeSpec, int sizeIdx, Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (contentHandler == null) throw new NullPointerException("data handler cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        AsyncStream<R> stream = new FileStream<R>(box, id, sizeSpec, sizeIdx, priority, contentHandler, handler);
        return box.submitAsync(stream);
    }

    private static class FileStream<R> extends AsyncStream<R> {
        private final String id;
        private HttpRequest request;

        protected FileStream(BaasBox box, String id, String sizeSpec, int sizeId, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
            super(box, priority, dataStream, handler);
            this.id = id;
            RequestFactory.Param param = null;
            if (sizeSpec != null) {
                param = new RequestFactory.Param("resize", sizeSpec);

            } else if (sizeId >= 0) {
                param = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
            }
            String endpoint = box.requestFactory.getEndpoint("file/?", id);
            if (param != null) {
                request = box.requestFactory.get(endpoint, param);
            } else {
                request = box.requestFactory.get(endpoint);
            }
        }

        @Override
        protected String streamId() {
            return id;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }

    public <R> RequestToken stream(DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return stream(Priority.NORMAL, contentHandler, handler);
    }

    public <R> RequestToken stream(int sizeId, Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return stream(id, null, sizeId, priority, contentHandler, handler);
    }

    public <R> RequestToken stream(Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return stream(id, priority, contentHandler, handler);
    }

    private static BaasResult<BaasStream> streamSync(String id, String spec, int sizeId) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        StreamRequest synReq = new StreamRequest(box, id, spec, sizeId);
        return box.submitSync(synReq);
    }


    public static BaasResult<BaasStream> streamSync(String id, int sizeId) {
        return streamSync(id, null, sizeId);
    }

    public static BaasResult<BaasStream> streamSync(String id, String spec) {
        return streamSync(id, spec, -1);
    }


    public BaasResult<BaasStream> streamSync(String spec) {
        if (id == null) throw new NullPointerException("this is not bound to a remote entity");
        return streamSync(id, spec, -1);
    }


    public BaasResult<BaasStream> streamSync(int sizeId) {
        if (id == null) throw new NullPointerException("this is not bound to a remote entity");
        return streamSync(id, null, sizeId);
    }

    public BaasResult<BaasStream> streamSync() {
        if (id == null) throw new NullPointerException("this is not bound to a remote entity");
        return streamSync(id, null, -1);
    }


    private Upload uploadRequest(BaasBox box, InputStream stream, Priority priority, BaasHandler<BaasFile> handler, JsonObject acl) {
        RequestFactory factory = box.requestFactory;
        if (!isBound.compareAndSet(false, true)) {
            throw new IllegalArgumentException("you cannot upload new content for this file");
        }
        if (stream == null) throw new NullPointerException("stream cannot be null");
        boolean tryGuessExtension = false;
        if (this.mimeType == null) {
            try {
                this.mimeType = URLConnection.guessContentTypeFromStream(stream);
                tryGuessExtension = true;
            } catch (IOException e) {
                this.mimeType = "application/octet-stream";
            }
        }
        if (this.name == null) {
            this.name = UUID.randomUUID().toString();
            if (tryGuessExtension) {
                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

                if (ext != null) {
                    this.name = name + "." + ext;
                }
            }
        }
        String endpoint = factory.getEndpoint("file");
        HttpRequest req = factory.uploadFile(endpoint, true, stream, name, mimeType, acl, attachedData);
        return new Upload(box, this, req, priority, handler);
    }

    private final static class Details extends NetworkTask<BaasFile> {
        private final BaasFile file;

        protected Details(BaasBox box, BaasFile file, Priority priority, BaasHandler<BaasFile> handler) {
            super(box, priority, handler);
            this.file = file;
        }

        @Override
        protected BaasFile onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            file.update(data);
            return file;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.get(box.requestFactory.getEndpoint("file/details/?", file.id));
        }
    }

    private final static class Delete extends NetworkTask<Void> {
        private final BaasFile file;
        private final String id;

        protected Delete(BaasBox box, BaasFile file, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.file = file;
            this.id = file.id;
        }

        protected Delete(BaasBox box, String id, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.id = id;
            this.file = null;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            if (file != null) file.id = null;
            return null;
        }

        @Override
        protected Void onSkipRequest() throws BaasException {
            throw new BaasException("file is not bounded to an entity on the server");
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            if (id == null) {
                return null;
            } else {
                String endpoint = box.requestFactory.getEndpoint("file/?", id);
                return box.requestFactory.delete(endpoint);
            }
        }
    }

    private final static class Files extends NetworkTask<List<BaasFile>> {
        private RequestFactory.Param[] params;

        protected Files(BaasBox box, Filter filter, Priority priority, BaasHandler<List<BaasFile>> handler) {
            super(box, priority, handler);
            if (filter == null) {
                params = null;
            } else {
                params = filter.toParams();
            }
        }

        @Override
        protected List<BaasFile> onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            try {
                JsonArray details = parseJson(response, box).getArray("data");

                ArrayList<BaasFile> files = new ArrayList<BaasFile>();
                for (Object o : details) {
                    JsonObject obj = (JsonObject) o;
                    BaasFile f = new BaasFile(obj, true);
                    files.add(f);
                }
                return files;
            } catch (JsonException e) {
                throw new BaasException(e);
            }
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("file/details");
            if (params == null) {
                return box.requestFactory.get(endpoint);
            } else {
                return box.requestFactory.get(endpoint, params);
            }
        }
    }

    private final static class Upload extends NetworkTask<BaasFile> {
        private final BaasFile file;
        private final HttpRequest request;

        protected Upload(BaasBox box, BaasFile file, HttpRequest request, Priority priority, BaasHandler<BaasFile> handler) {
            super(box, priority, handler);
            this.file = file;
            this.request = request;
        }

        @Override
        protected BaasFile onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject o = parseJson(response, box).getObject("data");
            file.update(o);
            return file;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }

    }

}
