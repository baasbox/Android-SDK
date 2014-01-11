package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by eto on 07/01/14.
 */
public class BaasAsset {

    public final String fileName;
    public final String type;
    public final String encoding;
    public final long length;
    private InputStream data;
    private JsonObject meta;

    private BaasAsset(String name, String type, String encoding, long length) {
        this.fileName = name;
        this.type = type;
        this.length = length;
        this.encoding = encoding;
    }

    public byte[] getData() {
        byte[] d = new byte[(int) length];
        try {
            data.read(d);
            return d;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                data.close();
            } catch (IOException e) {

            }
        }
    }

    protected void setData(InputStream data) {
        this.data = data;
    }

    public interface DataHandler<T> {
        public boolean read(BaasAsset asset, InputStream data, T tag) throws IOException;
    }

    public interface BaasDataHandler<T> extends BAASBox.BAASHandler<BaasAsset, T>, DataHandler<T> {
        @Override
        public boolean read(BaasAsset asset, InputStream data, T tag) throws IOException;

        @Override
        void handle(BaasResult<BaasAsset> result, T tag);
    }


    public static <T> RequestToken get(BAASBox client, String name, T tag, int priority, BaasDataHandler<T> handler) {
        return get(client, name, tag, priority, handler, handler);
    }

    public static <T> RequestToken get(BAASBox client, String name, T tag, int priority, BAASBox.BAASHandler<BaasAsset, T> handler) {
        return get(client, name, tag, priority, new DataHandler<T>() {
            @Override
            public boolean read(BaasAsset asset, InputStream in, T tag) throws IOException {
                byte[] data = new byte[new Long(asset.length).intValue()];
                DataInputStream din = new DataInputStream(in);
                din.readFully(data);
                asset.setData(new ByteArrayInputStream(data));
                return true;
            }
        }, handler);
    }

    public static <T> RequestToken get(BAASBox client, String name, T tag, int priority, DataHandler<T> dataHandler, BAASBox.BAASHandler<BaasAsset, T> endHandler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("asset/?", name);
        HttpRequest get = factory.get(endpoint);
        BaasRequest<BaasAsset, T> req = new BaasRequest<BaasAsset, T>(get, priority, tag, new DataParser<T>(dataHandler, name), endHandler, false);
        return client.submitRequest(req);
    }

    protected static class DataParser<T> extends BaseResponseParser<BaasAsset> {
        private final DataHandler<T> dataHandler;
        private final String dataName;

        DataParser(DataHandler<T> handler, String dataName) {
            dataHandler = handler;
            this.dataName = dataName;
        }

        @Override
        protected BaasAsset handleOk(BaasRequest<BaasAsset, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            HttpEntity entity = response.getEntity();
            long len = entity.getContentLength();
            String contentType = entity.getContentType().getValue();

            Header contentEncodingHeader = entity.getContentEncoding();
            String contentEncoding = contentEncodingHeader == null ? "" : contentEncodingHeader.getValue();
            InputStream in = null;
            boolean consume = false;
            try {
                in = entity.getContent();
                BaasAsset asset = new BaasAsset(dataName, contentType, contentEncoding, len);
                consume = dataHandler.read(asset, in, (T) request.tag);
                if (asset.data == null) throw new IllegalStateException("Content not set");
                if (consume) entity.consumeContent();
                return asset;
            } catch (IOException e) {
                throw new BAASBoxException("Unable to read content", e);
            } finally {
                if (in != null && consume) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
