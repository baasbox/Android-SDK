package com.baasbox.android;

import android.webkit.MimeTypeMap;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    public static <T> RequestToken getAll(T tag, Priority priority, BAASBox.BAASHandler<List<BaasFile>, T> handler) {
        return getAll(Filter.ANY, tag, priority, handler);
    }

    public static <T> RequestToken getAll(Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasFile>, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        filter = filter == null ? Filter.ANY : filter;
        priority = priority == null ? Priority.NORMAL : priority;
        ListRequest<T> breq = new ListRequest<T>(box.requestFactory, filter, priority, tag, handler);
        return box.submitRequest(breq);
    }

    public static RequestToken getAll(Filter filter, BAASBox.BAASHandler<List<BaasFile>, ?> handler) {
        return getAll(filter, null, null, handler);
    }

    public static RequestToken getAll(BAASBox.BAASHandler<List<BaasFile>, ?> handler) {
        return getAll(null, null, Priority.NORMAL, handler);
    }

    public static BaasResult<List<BaasFile>> getAllSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        ListRequest<Void> req = new ListRequest<Void>(box.requestFactory, Filter.ANY, null, null, null);
        return box.submitRequestSync(req);
    }

    public static BaasResult<List<BaasFile>> getAllSync(Filter filter) {
        BAASBox box = BAASBox.getDefaultChecked();
        filter = filter == null ? Filter.ANY : filter;
        ListRequest<Void> req = new ListRequest<Void>(box.requestFactory, filter, null, null, null);
        return box.submitRequestSync(req);
    }

    public static RequestToken fetch(String id, BAASBox.BAASHandler<BaasFile, ?> handler) {
        return fetch(id, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken fetch(String id, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        DetailsRequest<T> details = new DetailsRequest<T>(box.requestFactory, id, priority, tag, handler);
        return box.submitRequest(details);
    }

    public static BaasResult<BaasFile> fetchSync(String id) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        DetailsRequest<Void> details = new DetailsRequest<Void>(box.requestFactory, id, null, null, null);
        return box.submitRequestSync(details);
    }

    public BaasResult<BaasFile> refreshSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null)
            throw new NullPointerException("this file is not bound to an entity on the server");
        DetailsRequest<Void> details = new DetailsRequest<Void>(box.requestFactory, this, null, null, null);
        return box.submitRequestSync(details);
    }

    public RequestToken refresh(BAASBox.BAASHandler<BaasFile, ?> handler) {
        return refresh(null, Priority.NORMAL, handler);
    }

    public <T> RequestToken refresh(T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        DetailsRequest<T> details = new DetailsRequest<T>(box.requestFactory, this, priority, tag, handler);
        return box.submitRequest(details);
    }

    public BaasResult<Void> deleteSync() {
        if (id == null)
            throw new IllegalStateException("this file is not bound to an object on the server");
        return BaasFile.deleteSync(id);
    }

    public static BaasResult<Void> deleteSync(String id) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        RequestFactory f = box.requestFactory;
        String endpoint = f.getEndpoint("file/?", id);
        HttpRequest delete = f.delete(endpoint);
        DeleteRequest<Void> req = new DeleteRequest<Void>(delete, null, null, null);
        return box.submitRequestSync(req);
    }

    public static RequestToken delete(String id, BAASBox.BAASHandler<Void, ?> handler) {
        return delete(id, null, Priority.NORMAL, handler);
    }

    public RequestToken delete(BAASBox.BAASHandler<Void, ?> handler) {
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        return BaasFile.delete(id, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken delete(String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        RequestFactory f = box.requestFactory;
        String endpoint = f.getEndpoint("file/?", id);
        HttpRequest delete = f.delete(endpoint);
        BaasRequest<Void, T> breq = new BaasObject.DeleteRequest<T>(delete, priority, tag, handler);
        return box.submitRequest(breq);
    }

    public <T> RequestToken delete(T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        if (id == null)
            throw new IllegalStateException("this file is not bound to an entity on the server");
        return BaasFile.delete(id, tag, priority, handler);
    }

    @Override
    public <T> RequestToken revokeAll(Grant grant, String role, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<T> req = GrantRequest.grantAsync(box, false, grant, true, null, id, role, tag, priority, handler);
        return box.submitRequest(req);
    }

    @Override
    public BaasResult<Void> revokeAllSync(Grant grant, String role) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<Void> req = GrantRequest.grant(box, false, grant, true, null, id, role, null, null, null);
        return box.submitRequestSync(req);
    }

    @Override
    public <T> RequestToken revoke(Grant grant, String username, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<T> req = GrantRequest.grant(box, false, grant, false, null, id, username, tag, priority, handler);
        return box.submitRequest(req);
    }

    @Override
    public BaasResult<Void> revokeSync(Grant grant, String user) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<Void> req = GrantRequest.grant(box, false, grant, false, null, id, user, null, null, null);
        return box.submitRequestSync(req);
    }

    @Override
    public <T> RequestToken grantAll(Grant grant, String role, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<T> req = GrantRequest.grantAsync(box, true, grant, true, null, id, role, tag, priority, handler);
        return box.submitRequest(req);
    }


    @Override
    public BaasResult<Void> grantAllSync(Grant grant, String role) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<Void> req = GrantRequest.grant(box, true, grant, true, null, id, role, null, null, null);
        return box.submitRequestSync(req);
    }

    @Override
    public <T> RequestToken grant(Grant grant, String username, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<T> req = GrantRequest.grant(box, false, grant, true, null, id, username, tag, priority, handler);
        return box.submitRequest(req);
    }

    @Override
    public BaasResult<Void> grantSync(Grant grant, String user) {
        BAASBox box = BAASBox.getDefaultChecked();
        GrantRequest<Void> req = GrantRequest.grant(box, true, grant, false, null, id, user, null, null, null);
        return box.submitRequestSync(req);
    }

    public <T> RequestToken upload(InputStream stream, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        RequestFactory factory = box.requestFactory;
        if (handler == null) throw new NullPointerException("handler cannot be null");
        UploadRequest<T> req = uploadRequest(factory, stream, tag, priority == null ? Priority.NORMAL : priority, handler);
        return box.submitRequest(req);
    }

    public BaasResult<BaasFile> uploadSync(InputStream stream) {
        BAASBox box = BAASBox.getDefaultChecked();
        UploadRequest<Void> req = uploadRequest(box.requestFactory, stream, null, null, null);
        return box.submitRequestSync(req);
    }


    public RequestToken upload(InputStream stream, BAASBox.BAASHandler<BaasFile, ?> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        RequestFactory factory = box.requestFactory;
        if (handler == null) throw new NullPointerException("handler cannot be null");
        UploadRequest<?> req = uploadRequest(factory, stream, null, Priority.NORMAL, handler);
        return box.submitRequest(req);
    }

    public <T> RequestToken upload(File file, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        if (file == null) throw new NullPointerException("file cannot be null");
        try {

            FileInputStream fin = new FileInputStream(file);
            return upload(fin, tag, priority, handler);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }

    public BaasResult<BaasFile> uploadSync(File file) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (file == null) throw new NullPointerException("file cannot be null");
        try {
            FileInputStream in = new FileInputStream(file);
            UploadRequest<Void> req = uploadRequest(box.requestFactory, in, null, null, null);
            return box.submitRequestSync(req);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file does not exists", e);
        }
    }


    public RequestToken upload(File file, BAASBox.BAASHandler<BaasFile, ?> handler) {
        return upload(file, null, null, handler);
    }


    public <T> RequestToken upload(byte[] bytes, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(in, tag, priority, handler);
    }

    public BaasResult<BaasFile> uploadSync(byte[] bytes) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        UploadRequest<Void> req = uploadRequest(box.requestFactory, in, null, null, null);
        return box.submitRequestSync(req);
    }

    public <T> RequestToken upload(byte[] bytes, BAASBox.BAASHandler<BaasFile, T> handler) {
        if (bytes == null) throw new NullPointerException("bytes cannot be null");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return upload(in, null, Priority.NORMAL, handler);
    }

    public static <R, T> RequestToken stream(String id, int size, T tag, Priority priority, DataStreamHandler<R> data, BAASBox.BAASHandler<R, T> handler) {
        return stream(id, null, size, tag, priority, data, handler);
    }

    public static <R> RequestToken stream(String id, int size, DataStreamHandler<R> data, BAASBox.BAASHandler<R, ?> handler) {
        return stream(id, null, size, null, null, data, handler);
    }

    public static <R> RequestToken stream(String id, DataStreamHandler<R> data, BAASBox.BAASHandler<R, Object> handler) {
        return stream(id, null, -1, null, Priority.NORMAL, data, handler);
    }


    public static <R, T> RequestToken stream(String id, T tag, Priority priority, DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, T> handler) {
        return stream(id, null, -1, tag, priority, contentHandler, handler);
    }

    private static <R, T> RequestToken stream(String id, String sizeSpec, int sizeIdx, T tag, Priority priority, DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        RequestFactory factory = box.requestFactory;
        AsyncStreamRequest<T, R> breq = AsyncStreamRequest.buildAsyncFileDataRequest(factory, id, sizeSpec, sizeIdx, tag, priority, contentHandler, handler);
        return box.submitRequest(breq);
    }

    public <R> RequestToken stream(DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, ?> handler) {
        return stream(null, Priority.NORMAL, contentHandler, handler);
    }

    public <R, T> RequestToken stream(int sizeId, T tag, Priority priority, DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, T> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return stream(id, null, sizeId, tag, priority, contentHandler, handler);
    }

    public <R, T> RequestToken stream(T tag, Priority priority, DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, T> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("this file is not bound to any remote entity");
        return stream(id, tag, priority, contentHandler, handler);
    }

    public static BaasResult<BaasStream> streamSync(String id, int sizeId) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        StreamRequest synReq = StreamRequest.buildSyncDataRequest(box, id, sizeId);
        return box.submitRequestSync(synReq);
    }

    public BaasResult<BaasStream> streamSync(int sizeId) {
        if (id == null) throw new NullPointerException("this is not bound to a remote entity");
        return streamSync(id, sizeId);
    }

    public BaasResult<BaasStream> streamSync() {
        if (id == null) throw new NullPointerException("this is not bound to a remote entity");
        return streamSync(id, -1);
    }


    private <T> UploadRequest<T> uploadRequest(RequestFactory factory, InputStream stream, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
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
        HttpRequest req = factory.uploadFile(endpoint, true, stream, name, mimeType, attachedData);
        UploadRequest<T> breq = new UploadRequest<T>(this, req, priority, tag, handler);
        return breq;
    }

    private final static class DetailsRequest<T> extends BaseRequest<BaasFile, T> {
        private BaasFile file;

        DetailsRequest(RequestFactory factory, BaasFile file, Priority priority, T tag, BAASBox.BAASHandler<BaasFile, T> handler) {
            this(factory, file.id, priority, tag, handler);
            this.file = file;
        }

        DetailsRequest(RequestFactory factory, String id, Priority priority, T tag, BAASBox.BAASHandler<BaasFile, T> handler) {
            super(factory.get(factory.getEndpoint("file/details/?", id)), priority, tag, handler);
        }


        @Override
        protected BaasFile handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject details = getJsonEntity(response, config.HTTP_CHARSET).getObject("data");
                if (this.file == null) {
                    return new BaasFile(details, true);
                } else {
                    this.file.update(details);
                    return this.file;
                }

            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


    private final static class ListRequest<T> extends BaseRequest<List<BaasFile>, T> {

        ListRequest(RequestFactory factory, Filter filter, Priority priority, T t, BAASBox.BAASHandler<List<BaasFile>, T> handler) {
            super(factory.get(factory.getEndpoint("file/details"), filter.toParams()), priority, t, handler);
        }

        @Override
        protected List<BaasFile> handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject details = getJsonEntity(response, config.HTTP_CHARSET);
                JsonArray data = details.getArray("data");
                ArrayList<BaasFile> files = new ArrayList<BaasFile>();
                for (Object o : data) {
                    JsonObject obj = (JsonObject) o;
                    BaasFile f = new BaasFile(obj, true);
                    files.add(f);
                }
                return files;
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


    private final static class UploadRequest<T> extends BaseRequest<BaasFile, T> {
        BaasFile file;

        UploadRequest(BaasFile file, HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<BaasFile, T> handler) {
            super(request, priority, t, handler);
            this.file = file;
        }

        @Override
        protected BaasFile handleFail(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return super.handleFail(response, config, credentialStore);
        }

        @Override
        protected BaasFile handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject o = getJsonEntity(response, config.HTTP_CHARSET).getObject("data");
                file.update(o);
                return file;
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


}
