package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by eto on 09/01/14.
 */
public class BaasFile {

    private JsonObject attachedData;
    private String mimeType;
    private String name;

    private String id;
    private String author;
    private long contentLength;

    public BaasFile() {
        this(null, null, null);
    }

    public BaasFile(JsonObject attachedData) {
        this(null, null, attachedData);
    }

    public BaasFile(String id) {
        this(id, null, null);
    }

    public BaasFile(String id, JsonObject attachedData) {
        this(id, null, attachedData);
    }

    public BaasFile(String id, String name, JsonObject attachedData) {
        this.id = id;
        this.attachedData = attachedData;
        this.name = name == null ? "file_" + UUID.randomUUID().toString() : name;
    }

    public static <T> RequestToken get(BAASBox box, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file/details/?", id);
        HttpRequest req = factory.get(endpoint);
        BaasRequest<BaasFile, T> breq = new DetailsRequest<T>(req, priority, tag, handler);
        return box.submitRequest(breq);
    }

    public static <T> RequestToken getAll(BAASBox box, T tag, Priority priority, BAASBox.BAASHandler<List<BaasFile>, T> handler) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file/details");
        HttpRequest req = factory.get(endpoint);
        BaasRequest<List<BaasFile>, T> breq = new ListRequest<T>(req, priority, tag, handler);
        return box.submitRequest(breq);
    }

    private final static class ListRequest<T> extends BaseRequest<List<BaasFile>, T> {

        ListRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<List<BaasFile>, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected List<BaasFile> handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject details = getJsonEntity(response, config.HTTP_CHARSET);
            JsonArray data = details.getArray("data");
            ArrayList<BaasFile> files = new ArrayList<BaasFile>();
            for (Object o : data) {
                JsonObject obj = (JsonObject) o;
                String id = obj.getString("id");
                String fileName = obj.getString("fileName");
                String contentType = obj.getString("contentType");
                long contentLength = obj.getLong("contentLength", -1);
                String author = obj.getString("_author");
                JsonObject attachedData = obj.getObject("attachedData");
                BaasFile f = new BaasFile(id, fileName, attachedData);
                f.mimeType = contentType;
                f.contentLength = contentLength;
                f.author = author;
                files.add(f);
            }
            return files;
        }
    }

    private final static class DetailsRequest<T> extends BaseRequest<BaasFile, T> {

        DetailsRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<BaasFile, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected BaasFile handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject details = getJsonEntity(response, config.HTTP_CHARSET);
            BAASLogging.debug(details.toString());
            return null;
        }
    }

    public static <T> RequestToken save(BAASBox box, JsonObject attachedData, InputStream in, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        BaasFile file = new BaasFile(attachedData);
        return file.save(box, in, tag, priority, handler);
    }

    public static BaasResult<BaasFile> saveSync(BAASBox box, JsonObject attachedData, InputStream in) {
        BaasFile f = new BaasFile(attachedData);
        return f.saveSync(box, in);
    }

    public static BaasResult<BaasFile> saveSync(BAASBox box, JsonObject attachedData, File file) {
        BaasFile f = new BaasFile(attachedData);
        return f.saveSync(box, file);
    }

    public static <T> RequestToken save(BAASBox box, JsonObject attachedData, File in, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        BaasFile file = new BaasFile(attachedData);
        return file.save(box, in, tag, priority, handler);
    }

    public <T> RequestToken save(BAASBox box, File file, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        try {
            FileInputStream in = new FileInputStream(file);
            this.name = file.getName();
            return save(box, attachedData, in, tag, priority, handler);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File does not exists");
        }
    }

    public BaasResult<BaasFile> saveSync(BAASBox box, File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            this.name = file.getName();
            return saveSync(box, in);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File does not exists");
        }
    }

    public BaasResult<BaasFile> saveSync(BAASBox box, InputStream in) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file");
        try {
            this.mimeType = URLConnection.guessContentTypeFromStream(in);
        } catch (IOException e) {
        }
        HttpRequest upload = factory.uploadFile(endpoint, true, in, name, mimeType, attachedData);
        BaasRequest<BaasFile, Void> breq = new UploadRequest<Void>(this, upload, null, null, null);
        return box.submitRequestSync(breq);
    }

    public <T> RequestToken save(BAASBox box, InputStream stream, T tag, Priority priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file");
        try {
            this.mimeType = URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException e) {

        }
        HttpRequest upload = factory.uploadFile(endpoint, true, stream, name, mimeType, attachedData);
        BaasRequest<BaasFile, T> breq = new UploadRequest<T>(this, upload, priority, tag, handler);
        return box.submitRequest(breq);
    }

    void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setContentType(String contentType) {
        this.mimeType = contentType;
    }

    public String getContentType() {
        return mimeType;
    }

    void setContentLength(long length) {
        this.contentLength = length;
    }

    public long getContentLength() {
        return this.contentLength;
    }


    private final static class UploadRequest<T> extends BaseRequest<BaasFile, T> {
        private final BaasFile initial;

        UploadRequest(BaasFile file, HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<BaasFile, T> handler) {
            super(request, priority, t, handler);
            this.initial = file;
        }

        @Override
        protected BaasFile handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject o = getJsonEntity(response, config.HTTP_CHARSET).getObject("data");
            BAASLogging.debug(o.toString());
            initial.setId(o.getString("id"));
            return initial;
        }
    }

    public static <T> RequestToken delete(BAASBox box, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        RequestFactory f = box.requestFactory;
        String endpoint = f.getEndpoint("file/?", id);
        HttpRequest delete = f.delete(endpoint);
        BaasRequest<Void, T> breq = new BaasObject.DeleteRequest<T>(delete, priority, tag, handler);
        return box.submitRequest(breq);
    }

    public <T> RequestToken delete(BAASBox box, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        return BaasFile.delete(box, id, tag, priority, handler);
    }

    public static <T> RequestToken download(BAASBox box, String id, T tag, Priority priority, DataHandler<T> contentHandler, BAASBox.BAASHandler<Void, T> handler) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file/?", id);
        HttpRequest request = factory.get(endpoint);
        BaasRequest<Void, T> breq = new DataRequest<T>(id, request, priority, tag, contentHandler, handler);
        ;
        return box.submitRequest(breq);
    }

    public <T> RequestToken download(BAASBox box, T tag, Priority priority, DataHandler<T> contentHandler, BAASBox.BAASHandler<Void, T> handler) {
        String id = getId();
        if (id == null) throw new IllegalStateException("Unknown file id");
        return download(box, id, tag, priority, contentHandler, handler);
    }

    //todo determine if data handler should return an object to the end handler.
    public static interface DataHandler<T> {
        public boolean onData(byte[] data, String id, String contentType, T tag, boolean more) throws Exception;
    }

    private static class DataRequest<T> extends BaseRequest<Void, T> {
        private final String id;
        private final DataHandler<T> dataHandler;

        DataRequest(String id, HttpRequest request, Priority priority, T t, DataHandler<T> dataHandler, BAASBox.BAASHandler<Void, T> handler) {
            super(request, priority, t, handler);
            this.id = id;
            this.dataHandler = dataHandler;
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            HttpEntity entity = null;
            BufferedInputStream in = null;
            try {
                entity = response.getEntity();
                Header contentTypeHeader = entity.getContentType();
                String contentType = "application/octet-stream";
                if (contentTypeHeader != null) contentType = contentTypeHeader.getValue();
                long contentLength = entity.getContentLength();
                byte[] data = new byte[Math.min((int) contentLength, 4096)];
                in = getInput(entity);
                int read = 0;
                long available = contentLength;
                while ((read = in.read(data, 0, Math.min((int) available, data.length))) != -1) {
                    available -= read;
                    if (!dataHandler.onData(data, id, contentType, tag, available > 0)) break;
                }
            } catch (IOException e) {
                throw new BAASBoxException(e);
            } catch (Exception e) {
                throw new BAASBoxException(e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (entity != null) {
                        entity.consumeContent();
                    }
                } catch (IOException e) {
                    BAASLogging.debug("Error while parsing data " + e.getMessage());
                }
            }
            return null;
        }

        private BufferedInputStream getInput(HttpEntity entity) throws IOException {
            InputStream in = entity.getContent();
            if (in instanceof BufferedInputStream) {
                return (BufferedInputStream) in;
            }
            return new BufferedInputStream(in);
        }
    }
}
