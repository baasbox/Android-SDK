package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.impl.Logging;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.UUID;

/**
 * Created by eto on 09/01/14.
 */
public class BaasFile {

    private JsonObject metaData;
    private File localFile;
    private String mimeType;
    private InputStream stream;
    private String name;
    private String id;

    public BaasFile(File file, JsonObject metaData) {
        if (file == null) throw new NullPointerException("file cannot be null");
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exists");
        }
        localFile = file;
        mimeType = URLConnection.guessContentTypeFromName(localFile.toURI().toString());
        this.metaData = metaData;
        this.name = localFile.getName();
    }

    public BaasFile(String fileName, JsonObject metaData) {
        this(fromName(fileName), metaData);
    }

    public BaasFile(InputStream stream, JsonObject metaData) {
        this.stream = stream;
        this.name = "_file__" + UUID.randomUUID().toString();
        try {
            this.mimeType = URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException e) {
            this.mimeType = "application/octet-stream";
        }
        this.metaData = metaData;
    }

    private static File fromName(String fileName) {
        if (fileName == null) throw new NullPointerException("filename cannot be null");
        return new File(fileName);
    }

    public <T> BaasDisposer save(BAASBox client, T tag, int priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endPoint = factory.getEndpoint("file");
        try {
            if (localFile != null) {
                stream = new FileInputStream(localFile);
            }
            HttpRequest post = factory.uploadFile(endPoint, false, stream, name, mimeType, metaData);
            BaasRequest<BaasFile, T> req = new BaasRequest<BaasFile, T>(post, priority, tag, new UploadParser(this), handler, true);
            return client.submitRequest(req);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> BaasDisposer delete(BAASBox client, String id, T tag, int priority, BAASBox.BAASHandler<Void, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("file/?", id);
        HttpRequest delete = factory.delete(endpoint);
        BaasRequest<Void, T> req = new BaasRequest<Void, T>(delete, priority, tag, deleteParser, handler, true);
        return client.submitRequest(req);
    }

    public <T> BaasDisposer delete(BAASBox client, T tag, int priority, BAASBox.BAASHandler<Void, T> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("File is local only and does not exists on baasbox");
        return delete(client, id, tag, priority, handler);
    }

    public static <T> BaasDisposer download(BAASBox client, String id, T tag, int priority, BAASBox.BAASHandler<BaasFile, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("file/?", id);
        HttpRequest get = factory.get(endpoint);
        BaasRequest<BaasFile, T> req = new BaasRequest<BaasFile, T>(get, priority, tag, new DownloadParser(id), handler, true);
        return client.submitRequest(req);
    }

    protected void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private static final BaasRequest.BaseResponseParser<Void> deleteParser =
            new BaasRequest.BaseResponseParser<Void>() {
                @Override
                protected Void handleOk(BaasRequest<Void, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
                    Logging.debug(getJsonEntity(response, config.HTTP_CHARSET).toString());
                    return null;
                }
            };

    private static class DownloadParser extends BaasRequest.BaseResponseParser<BaasFile> {
        private final String id;

        DownloadParser(String id) {
            this.id = id;
        }

        @Override
        protected BaasFile handleOk(BaasRequest<BaasFile, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            DataInputStream in = null;
            try {
                HttpEntity entity = response.getEntity();
                Header contentType = entity.getContentType();
                long contentLength = entity.getContentLength();
                in = new DataInputStream(entity.getContent());
                byte[] data = new byte[(int) contentLength];
                in.readFully(data);
                ByteArrayInputStream bin = new ByteArrayInputStream(data);
                entity.consumeContent();
                BaasFile file = new BaasFile(bin, null);
                if (contentType != null) {
                    file.mimeType = contentType.getValue();
                }
                file.setId(id);
                return file;
            } catch (IOException e) {
                throw new BAASBoxIOException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {

                    }
                }
            }
        }

    }

    private static class UploadParser extends BaasRequest.BaseResponseParser<BaasFile> {
        BaasFile initial;

        UploadParser(BaasFile file) {
            initial = file;
        }

        @Override
        protected BaasFile handleOk(BaasRequest<BaasFile, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject o = getJsonEntity(response, config.HTTP_CHARSET).getObject("data");
            Logging.debug(o.toString());


            initial.setId(o.getString("id"));
            return initial;
        }
    }

}
