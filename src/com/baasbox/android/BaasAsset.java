package com.baasbox.android;

import com.baasbox.android.net.HttpRequest;

/**
 * Created by Andrea Tortorella on 13/03/14.
 */
public final class BaasAsset {

    private BaasAsset(){}

    /**
     * Streams the file using the provided data stream handler.
     *
     * @param id      the name of the asset to download
     * @param data    the data stream handler {@link com.baasbox.android.DataStreamHandler}
     * @param handler the completion handler
     * @param <R>     the type to transform the bytes to.
     * @return a request token to handle the request
     */
    public static <R> RequestToken streamAsset(String id, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return BaasAsset.streamAsset(id, null, -1, null, data, handler);
    }

    /**
     * Streams the file using the provided data stream handler.
     *
     * @param id      the name of the asset to download
     * @param size    a size spec to specify the resize of an image asset
     * @param data    the data stream handler {@link com.baasbox.android.DataStreamHandler}
     * @param handler the completion handler
     * @param <R>     the type to transform the bytes to.
     * @return a request token to handle the request
     */
    public static <R> RequestToken streamAsset(String id, int size, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return BaasAsset.streamAsset(id, null, size, null, data, handler);
    }

    /**
     * Streams the file using the provided data stream handler.
     *
     * @param id       the name of the asset to download
     * @param priority a priority at which the request should be executed defaults to {@link com.baasbox.android.Priority#NORMAL}
     * @param handler  the completion handler
     * @param <R>      the type to transform the bytes to.
     * @return a request token to handle the request
     */
    public static <R> RequestToken streamAsset(String id, Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return BaasAsset.streamAsset(id, null, -1, priority, contentHandler, handler);
    }

    /**
     * Streams the file using the provided data stream handler.
     *
     * @param id       the name of the asset to download
     * @param size     a size spec to specify the resize of an image asset
     * @param priority a priority at which the request should be executed defaults to {@link com.baasbox.android.Priority#NORMAL}
     * @param data     the data stream handler {@link com.baasbox.android.DataStreamHandler}
     * @param handler  the completion handler
     * @param <R>      the type to transform the bytes to.
     * @return a request token to handle the request
     */
    public static <R> RequestToken streamAsset(String id, int size, Priority priority, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return BaasAsset.streamAsset(id, null, size, priority, data, handler);
    }

    /**
     * Synchronously streams the asset.
     *
     * @param id   the name of the asset to download
     * @param spec a size spec to specify the resize of an image asset
     * @return a {@link com.baasbox.android.BaasStream} wrapped in a result
     */
    public static BaasResult<BaasStream> streamAssetSync(String id, String spec) {
        return BaasAsset.streamSync(id, spec, -1);
    }

    /**
     * Synchronously streams the asset.
     *
     * @param id     the name of the asset to download
     * @param sizeId the size index if the asset is an image
     * @return a {@link com.baasbox.android.BaasStream} wrapped in a result
     */
    public static BaasResult<BaasStream> streamAssetSync(String id, int sizeId) {
        return BaasAsset.streamSync(id, null, sizeId);
    }


    static BaasResult<BaasStream> streamSync(String id, String spec, int sizeId) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        StreamRequest synReq = new StreamRequest(box, "asset", id, spec, sizeId);
        return box.submitSync(synReq);
    }

    static <R> RequestToken streamAsset(String name, String sizeSpec, int sizeIdx, Priority priority, DataStreamHandler<R> dataStreamHandler, BaasHandler<R> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (dataStreamHandler == null) throw new IllegalArgumentException("data handler cannot be null");
        if (name == null) throw new IllegalArgumentException("id cannot be null");
        AsyncStream<R> stream = new AssetStream<R>(box, name, sizeSpec, sizeIdx, priority, dataStreamHandler, handler);
        return box.submitAsync(stream);
    }

    private static class AssetStream<R> extends AsyncStream<R> {
        private final String name;
        private HttpRequest request;

        protected AssetStream(BaasBox box, String name, String sizeSpec, int sizeId, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
            super(box, priority, dataStream, handler, false);
            this.name = name;
            RequestFactory.Param param = null;
            if (sizeSpec != null) {
                param = new RequestFactory.Param("resize", sizeSpec);
            } else if (sizeId >= 0) {
                param = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
            }
            String endpoint = box.requestFactory.getEndpoint("asset/{}", name);
            if (param != null) {
                request = box.requestFactory.get(endpoint, param);
            } else {
                request = box.requestFactory.get(endpoint);
            }
        }

        @Override
        protected String streamId() {
            return name;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }
}
