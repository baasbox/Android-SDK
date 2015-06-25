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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.baasbox.android.impl.Util;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a BaasBox file.
 * <p>
 * A file is an entity that can be stored on the server.
 * Unlike documents files do not dictate a format for their content, but they can
 * have optionally json attached data.
 * </p>
 * <p/>
 * <p>
 * Files can be created, stored and retrieved from the server either synchronously or asynchronously,
 * through the provided methods.
 * </p>
 * <p>
 * A BaasFile does not represent the content of the file, but an handle to the actual content.
 * The actual content can be obtained through a streaming api.
 * </p>
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class BaasFile extends BaasObject implements Parcelable{
// ------------------------------ FIELDS ------------------------------

    private JsonWrapper attachedData;
    private JsonObject metaData;
    private String mimeType;
    private String name;

    private String creationDate;
    private String id;
    private String author;
    private long contentLength;
    private long version;
    private final AtomicBoolean isBound = new AtomicBoolean();
    private AtomicReference<byte[]> data = new AtomicReference<byte[]>();



    // --------------------------- CONSTRUCTORS ---------------------------

    public static BaasFile create(){
        return new BaasFile();
    }

    public static BaasFile create(JsonObject attachedData) {
        return new BaasFile(attachedData);
    }

    public static BaasFile from(JsonObject file){
        if (!file.contains("@version") || !file.contains("_creationDate")||!file.contains("id")){
            throw new IllegalArgumentException("This seems not to be a valid file representation");
        }
        return new BaasFile(file,true);
    }

    public BaasFile() {
        this(new JsonObject(), false);
    }

    public BaasFile(JsonObject attachedData) {
        this(attachedData, false);
    }

    BaasFile(JsonObject data, boolean fromServer) {
        super();
        if (fromServer) {
            this.attachedData = new JsonWrapper(data);
            this.metaData = new JsonObject();
            update(data);
        } else {
            this.attachedData = new JsonWrapper(data);
        }
    }


    void update(JsonObject fromServer) {
        isBound.set(true);
        this.attachedData.merge(fromServer.getObject("attachedData"));
        if (this.metaData == null) this.metaData = new JsonObject();
        this.metaData.merge(fromServer.getObject("metadata"));
        this.id = fromServer.getString("id");
        this.creationDate = fromServer.getString("_creation_date");
        this.author = fromServer.getString("_author");
        this.name = fromServer.getString("fileName");
        this.contentLength = fromServer.getLong("contentLength");
        this.version = fromServer.getLong("@version");
        attachedData.setDirty(false);
    }

    @Override
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.put("attachedData",this.attachedData.asObject());
        if (this.metaData!=null){
            o.put("metadata",this.metaData);
        }
        o.put("id",this.id);
        o.put("_creation_date",this.creationDate);
        o.put("_author",this.author);
        o.put("fileName",this.name);
        o.put("contentLength",this.contentLength);
        o.put("@version",this.version);
        return o;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Util.writeOptString(dest,name);
        Util.writeOptString(dest,id);
        Util.writeOptString(dest,creationDate);
        Util.writeOptString(dest,author);
        Util.writeOptString(dest, mimeType);
        dest.writeParcelable(attachedData,flags);
        dest.writeParcelable(metaData, flags);
        dest.writeLong(contentLength);
        dest.writeLong(version);
        Util.writeBoolean(dest, isBound.get());
        Util.writeOptBytes(dest,data.get());
    }

    private BaasFile(Parcel source){
        name = Util.readOptString(source);
        id = Util.readOptString(source);
        creationDate = Util.readOptString(source);
        author = Util.readOptString(source);
        mimeType = Util.readOptString(source);
        attachedData =  source.readParcelable(BaasFile.class.getClassLoader());
        metaData = source.readParcelable(BaasFile.class.getClassLoader());
        contentLength = source.readLong();
        version = source.readLong();
        isBound.set(Util.readBoolean(source));
        data.set(Util.readOptBytes(source));
    }
    
    public static final Creator<BaasFile> CREATOR = new Creator<BaasFile>() {
        @Override
        public BaasFile createFromParcel(Parcel source) {
            return new BaasFile(source);
        }

        @Override
        public BaasFile[] newArray(int size) {
            return new BaasFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public boolean isDirty() {
        return attachedData.isDirty();
    }

    // -------------------------- STATIC METHODS --------------------------

    public static RequestToken fetchAll(BaasHandler<List<BaasFile>> handler) {
        return fetchAll(null, RequestOptions.DEFAULT, handler);
    }

    public static RequestToken fetchAll(BaasQuery.Criteria filter, BaasHandler<List<BaasFile>> handler) {
        return fetchAll(filter, RequestOptions.DEFAULT, handler);
    }

    public static RequestToken fetchAll(BaasQuery.Criteria filter, int flags, BaasHandler<List<BaasFile>> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Files files = new Files(box, filter, flags, handler);
        return box.submitAsync(files);
    }

    public static BaasResult<List<BaasFile>> fetchAllSync() {
        return fetchAllSync(null);
    }

    public static BaasResult<List<BaasFile>> fetchAllSync(BaasQuery.Criteria filter) {
        BaasBox box = BaasBox.getDefaultChecked();
        Files files = new Files(box, filter, RequestOptions.DEFAULT, null);
        return box.submitSync(files);
    }

    public static RequestToken fetch(String id, BaasHandler<BaasFile> handler) {
        return fetch(id, RequestOptions.DEFAULT, handler);
    }

    public static RequestToken fetch(String id, int flags, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        BaasFile file = new BaasFile();
        file.id = id;
        Details details = new Details(box, file, flags, handler);
        return box.submitAsync(details);
    }

    public static BaasResult<BaasFile> fetchSync(String id) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        BaasFile file = new BaasFile();
        file.id = id;
        Details details = new Details(box, file, RequestOptions.DEFAULT, null);
        return box.submitSync(details);
    }

    public static BaasResult<Void> deleteSync(String id) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        Delete delete = new Delete(box, id, RequestOptions.DEFAULT, null);
        return box.submitSync(delete);
    }

    public static RequestToken delete(String id, BaasHandler<Void> handler) {
        return delete(id, RequestOptions.DEFAULT, handler);
    }

    public static RequestToken delete(String id, int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        Delete delete = new Delete(box, id, flags, handler);
        return box.submitAsync(delete);
    }

    public static RequestToken fetchStream(String id, BaasHandler<BaasFile> handler) {
        BaasFile f = new BaasFile();
        f.id = id;
        return f.doStream(-1, null, RequestOptions.DEFAULT, handler);
    }

    private RequestToken doStream(int size, String spec, int flags, BaasHandler<BaasFile> handler) {
        return doStream(this.id, spec, size, flags, new FileStreamer(this), handler);
    }

    private static <R> RequestToken doStream(String id, String sizeSpec, int sizeIdx, int flags, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (contentHandler == null) throw new IllegalArgumentException("data handler cannot be null");
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        AsyncStream<R> stream = new FileStream<R>(box, id, sizeSpec, sizeIdx, flags, contentHandler, handler);
        return box.submitAsync(stream);
    }

    public static BaasResult<BaasStream> streamImageSync(String id, int sizeId) {
        return doStreamSync(id, null, sizeId);
    }


    public Uri getStreamUri(){
        if (id==null) throw new IllegalArgumentException("file is not bound");
        BaasBox cli = BaasBox.getDefaultChecked();
        String endpoint = cli.requestFactory.getEndpoint("file/{}", id);
        return cli.requestFactory.getAuthenticatedUri(endpoint);
    }

    public static BaasResult<BaasStream> streamImageSync(String id, String spec) {
        return doStreamSync(id, spec, -1);
    }

    private static BaasResult<BaasStream> doStreamSync(String id, String spec, int sizeId) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        StreamRequest synReq = new StreamRequest(box, "file", id, spec, sizeId);
        return box.submitSync(synReq);
    }

    public static <R> RequestToken stream(String id, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return doStream(id, null, -1, RequestOptions.DEFAULT, data, handler);
    }

    public static <R> RequestToken streamImage(String id, int size, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return doStream(id, null, size, RequestOptions.DEFAULT, data, handler);
    }

    public static <R> RequestToken stream(String id, int flags, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return doStream(id, null, -1, flags, contentHandler, handler);
    }

    public static <R> RequestToken streamImage(String id, int size, int flags, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return doStream(id, null, size, flags, data, handler);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public JsonObject getAttachedData() {
        return attachedData;
    }


    @Override
    public final boolean isFile() {
        return true;
    }

    @Override
    public final boolean isDocument() {
        return false;
    }


    /**
     * Retrieves the metadata associated with this file
     * coomputed by the server.
     *
     * @return an object containing metadata about this file or null if no metadata is found
     */
    public JsonObject getMetadata() {
        return metaData;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getCreationDate() {
        return creationDate;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getVersion() {
        return version;
    }

    public String getContentType() {
        return mimeType;
    }

    public byte[] getData() {
        return data.get();
    }

// -------------------------- OTHER METHODS --------------------------

    public RequestToken delete(BaasHandler<Void> handler) {
        return delete(RequestOptions.DEFAULT, handler);
    }

    public RequestToken delete(int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        Delete delete = new Delete(box, this, flags, handler);
        return box.submitAsync(delete);
    }

    public BaasResult<Void> deleteSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalStateException("this file is not bounded to an entity on the server");
        Delete delete = new Delete(box, this, RequestOptions.DEFAULT, null);
        return box.submitSync(delete);
    }

    @Override
    public RequestToken grant(Grant grant, String username, int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, false, id, username, grant, flags, handler);
        return box.submitAsync(access);
    }

    @Override
    public RequestToken grantAll(Grant grant, String role, int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, true, id, role, grant, flags, handler);
        return box.submitAsync(access);
    }

    @Override
    public BaasResult<Void> grantAllSync(Grant grant, String role) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, true, id, role, grant, RequestOptions.DEFAULT, null);
        return box.submitSync(access);
    }

    @Override
    public BaasResult<Void> grantSync(Grant grant, String user) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, true, false, id, user, grant, RequestOptions.DEFAULT, null);
        return box.submitSync(access);
    }

    public RequestToken refresh(BaasHandler<BaasFile> handler) {
        return refresh(RequestOptions.DEFAULT, handler);
    }

    public RequestToken refresh(int flags, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        Details details = new Details(box, this, flags, handler);
        return box.submitAsync(details);
    }

    public BaasResult<BaasFile> refreshSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        Details details = new Details(box, this, RequestOptions.DEFAULT, null);
        return box.submitSync(details);
    }

    @Override
    public RequestToken revoke(Grant grant, String username, int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, false, id, username, grant, RequestOptions.DEFAULT, handler);
        return box.submitAsync(access);
    }

    @Override
    public RequestToken revokeAll(Grant grant, String role, int flags, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, true, id, role, grant, RequestOptions.DEFAULT, handler);
        return box.submitAsync(access);
    }

    @Override
    public BaasResult<Void> revokeAllSync(Grant grant, String role) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, true, id, role, grant, RequestOptions.DEFAULT, null);
        return box.submitSync(access);
    }

    @Override
    public BaasResult<Void> revokeSync(Grant grant, String user) {
        BaasBox box = BaasBox.getDefaultChecked();
        Access access = new Access(box, false, false, id, user, grant, RequestOptions.DEFAULT, null);
        return box.submitSync(access);
    }

    public RequestToken extractedContent(int flags, BaasHandler<String> handler) {
        BaasBox box = BaasBox.getDefault();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        ContentRequest req = new ContentRequest(box, id, flags, handler);
        return box.submitAsync(req);
    }

    private static class ContentRequest extends NetworkTask<String> {
        private final String file;

        protected ContentRequest(BaasBox box, String file, int flags, BaasHandler<String> handler) {
            super(box, flags, handler);
            this.file = file;
        }

        @Override
        protected String onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            try {
                return EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                throw new BaasIOException("unable to parse content");
            }
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String ep = box.requestFactory.getEndpoint("file/content/{}", file);
            return box.requestFactory.get(ep);
        }
    }

    public RequestToken extractedContent(BaasHandler<String> handler) {
        return extractedContent(RequestOptions.DEFAULT, handler);
    }

    public BaasResult<String> extractedContentSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        ContentRequest req = new ContentRequest(box, id, RequestOptions.DEFAULT, null);
        return box.submitSync(req);
    }

    public RequestToken stream(BaasHandler<BaasFile> handler) {
        return doStream(-1, null, RequestOptions.DEFAULT, handler);
    }

    public RequestToken download(String path, BaasHandler<Pair<BaasFile, String>> handler) {
        return doStream(id, null, -1, RequestOptions.DEFAULT, new SaveToDisk(this, path), handler);
    }


    private static class SaveToDisk implements DataStreamHandler<Pair<BaasFile, String>> {
        final String fileName;
        final BaasFile file;
        FileOutputStream out;

        private SaveToDisk(BaasFile file, String fileName) {
            this.fileName = fileName;
            this.file = file;
        }

        @Override
        public void startData(String id, long contentLength, String contentType) throws Exception {
            out = new FileOutputStream(fileName);
        }

        @Override
        public void onData(byte[] data, int read) throws Exception {
            out.write(data, 0, read);
        }

        @Override
        public Pair<BaasFile, String> endData(String id, long contentLength, String contentType) throws Exception {
            return new Pair<BaasFile, String>(file, fileName);
        }

        @Override
        public void finishStream(String id) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // swallow
                }
            }
        }
    }

    public RequestToken streamImage(int sizeIdx, BaasHandler<BaasFile> handler) {
        return doStream(sizeIdx, null, RequestOptions.DEFAULT, handler);
    }

    public RequestToken streamImage(String sizeSpec, BaasHandler<BaasFile> handler) {
        return doStream(-1, sizeSpec, RequestOptions.DEFAULT, handler);
    }

    public <R> RequestToken stream(DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return stream(RequestOptions.DEFAULT, contentHandler, handler);
    }

    public <R> RequestToken stream(int flags, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return stream(id, flags, contentHandler, handler);
    }

    public <R> RequestToken stream(int sizeId, int flags, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return doStream(id, null, sizeId, flags, contentHandler, handler);
    }

    public BaasResult<BaasStream> streamSync() {
        if (id == null) throw new IllegalStateException("this is not bound to a remote entity");
        return doStreamSync(id, null, -1);
    }

    public BaasResult<BaasStream> streamImageSync(String spec) {
        if (id == null) throw new IllegalStateException("this is not bound to a remote entity");
        return doStreamSync(id, spec, -1);
    }

    public BaasResult<BaasStream> streamImaeSync(int sizeId) {
        if (id == null) throw new IllegalStateException("this is not bound to a remote entity");
        return doStreamSync(id, null, sizeId);
    }

    public RequestToken upload(InputStream stream, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Upload upload = uploadRequest(box, stream, RequestOptions.DEFAULT, handler, new JsonObject());
        return box.submitAsync(upload);
    }

    private Upload uploadRequest(BaasBox box, InputStream stream, int flags, BaasHandler<BaasFile> handler, JsonObject acl) {
        RequestFactory factory = box.requestFactory;
        if (!isBound.compareAndSet(false, true)) {
            throw new IllegalArgumentException("you cannot upload new content for this file");
        }
        if (stream == null) throw new IllegalArgumentException("doStream cannot be null");
        boolean tryGuessExtension = false;
        if (this.mimeType == null) {
            try {
                this.mimeType = URLConnection.guessContentTypeFromStream(stream);
                tryGuessExtension = true;
            } catch (IOException e) {
                this.mimeType = "application/octet-doStream";
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
        return new Upload(box, this, req, flags, handler);
    }

    public RequestToken upload(File file, BaasHandler<BaasFile> handler) {
        return upload(file, RequestOptions.DEFAULT, handler);
    }

    public RequestToken upload(byte[] bytes, BaasHandler<BaasFile> handler) {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(in, RequestOptions.DEFAULT, handler);
    }

    public RequestToken upload(InputStream stream, int flags, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        RequestFactory factory = box.requestFactory;
        Upload req = uploadRequest(box, stream, flags, handler, new JsonObject());
        return box.submitAsync(req);
    }

    public RequestToken upload(BaasACL acl, InputStream stream, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Upload upload = uploadRequest(box, stream, RequestOptions.DEFAULT, handler, acl.toJson());
        return box.submitAsync(upload);
    }

    public RequestToken upload(File file, int flags, BaasHandler<BaasFile> handler) {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        try {
            FileInputStream fin = new FileInputStream(file);
            return upload(fin, flags, handler);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }

    public RequestToken upload(BaasACL acl, File file, BaasHandler<BaasFile> handler) {
        return upload(acl, file, RequestOptions.DEFAULT, handler);
    }

    public RequestToken upload(byte[] bytes, int flags, BaasHandler<BaasFile> handler) {
        return upload(BaasACL.builder().build(), bytes, flags, handler);
    }

    public RequestToken upload(BaasACL acl, byte[] bytes, BaasHandler<BaasFile> handler) {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(acl, in, RequestOptions.DEFAULT, handler);
    }

    public RequestToken upload(BaasACL acl, InputStream stream, int flags, BaasHandler<BaasFile> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        RequestFactory factory = box.requestFactory;
        Upload req = uploadRequest(box, stream, flags, handler, acl.toJson());
        return box.submitAsync(req);
    }

    public RequestToken upload(BaasACL acl, File file, int flags, BaasHandler<BaasFile> handler) {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        try {
            FileInputStream fin = new FileInputStream(file);
            return upload(acl, fin, flags, handler);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }

    public RequestToken upload(BaasACL acl, byte[] bytes, int flags, BaasHandler<BaasFile> handler) {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(acl, in, flags, handler);
    }

    public BaasResult<BaasFile> uploadSync(InputStream stream){
        return uploadSync(null,stream);
    }

    public BaasResult<BaasFile> uploadSync(BaasACL acl,InputStream stream) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (stream == null) throw new IllegalArgumentException("stream cannot be null");
        Upload req = uploadRequest(box, stream, RequestOptions.DEFAULT, null, acl==null?new JsonObject():acl.toJson());
        return box.submitSync(req);
    }

    public BaasResult<BaasFile> uploadSync(byte[] bytes){
        return uploadSync(null,bytes);
    }

    public BaasResult<BaasFile> uploadSync(BaasACL acl,byte[] bytes) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        Upload req = uploadRequest(box, in, RequestOptions.DEFAULT,null,acl == null?new JsonObject():acl.toJson());
        return box.submitSync(req);
    }

    public BaasResult<BaasFile> uploadSync(File file){
        return uploadSync(null,file);
    }

    public BaasResult<BaasFile> uploadSync(BaasACL acl, File file) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        try {
            FileInputStream in = new FileInputStream(file);
            Upload req = uploadRequest(box, in, RequestOptions.DEFAULT, null, acl == null ? new JsonObject() : acl.toJson());
            return box.submitSync(req);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }

// -------------------------- INNER CLASSES --------------------------

    private static final class Access extends BaasObject.Access {
        protected Access(BaasBox box, boolean add, boolean isRole, String id, String to, Grant grant, int flags, BaasHandler<Void> handler) {
            super(box, add, isRole, null, id, to, grant, flags, handler);
        }

        @Override
        protected String userGrant(RequestFactory factory, Grant grant, String collection, String id, String to) {
            return factory.getEndpoint("file/{}/{}/user/{}", id, grant.action, to);
        }

        @Override
        protected String roleGrant(RequestFactory factory, Grant grant, String collection, String id, String to) {
            return factory.getEndpoint("file/{}/{}/role/{}", id, grant.action, to);
        }
    }

    private static class FileStreamer extends StreamBody<BaasFile> {
        BaasFile file;

        FileStreamer(BaasFile file) {
            this.file = file;
        }

        @Override
        protected BaasFile convert(byte[] body, String id, String contentType, long contentLength) {
            file.data.set(body);
            file.mimeType = contentType;
            file.contentLength = contentLength==-1?body.length:contentLength;
            return file;
        }
    }

    private static class FileStream<R> extends AsyncStream<R> {
        private final String id;
        private final String cacheKey;
        private HttpRequest request;

        protected FileStream(BaasBox box, String id, String sizeSpec, int sizeId, int flags, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
            super(box, flags, dataStream, handler);
            this.id = id;
            RequestFactory.Param param = null;
            if (sizeSpec != null) {
                param = new RequestFactory.Param("resize", sizeSpec);
                this.cacheKey = id + "_" + sizeSpec.replace("<=", "lte");
            } else if (sizeId >= 0) {
                param = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
                this.cacheKey = id + "_" + sizeId;
            } else {
                cacheKey = id;
            }
            String endpoint = box.requestFactory.getEndpoint("file/{}", id);
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
        protected String cacheKey() {
            return cacheKey;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }

    private static final class Details extends NetworkTask<BaasFile> {
        private final BaasFile file;

        protected Details(BaasBox box, BaasFile file, int flags, BaasHandler<BaasFile> handler) {
            super(box, flags, handler);
            this.file = file;
        }

        @Override
        protected BaasFile onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject jsonData = parseJson(response, box).getObject("data");
            file.update(jsonData);
            return file;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.get(box.requestFactory.getEndpoint("file/details/{}", file.id));
        }
    }

    private static final class Delete extends NetworkTask<Void> {
        private final BaasFile file;
        private final String id;

        protected Delete(BaasBox box, BaasFile file, int flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
            this.file = file;
            this.id = file.id;
        }

        protected Delete(BaasBox box, String id, int flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
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
                String endpoint = box.requestFactory.getEndpoint("file/{}", id);
                return box.requestFactory.delete(endpoint);
            }
        }
    }

    private static final class Files extends NetworkTask<List<BaasFile>> {
        private RequestFactory.Param[] params;

        protected Files(BaasBox box, BaasQuery.Criteria filter, int flags, BaasHandler<List<BaasFile>> handler) {
            super(box, flags, handler);
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

                List<BaasFile> files = new ArrayList<BaasFile>();
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

    private static final class Upload extends NetworkTask<BaasFile> {
        private final BaasFile file;
        private final HttpRequest request;

        protected Upload(BaasBox box, BaasFile file, HttpRequest request, int flags, BaasHandler<BaasFile> handler) {
            super(box, flags, handler);
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
