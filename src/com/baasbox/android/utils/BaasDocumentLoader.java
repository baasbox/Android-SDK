package com.baasbox.android.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Loader;
import android.os.Build;
import com.baasbox.android.*;

import java.util.List;

/**
 * Created by Andrea Tortorella on 27/01/14.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BaasDocumentLoader extends Loader<BaasResult<List<BaasDocument>>> {
// ------------------------------ FIELDS ------------------------------

    private BaasResult<List<BaasDocument>> mDocuments;
    private final String mCollection;
    private final Filter mFilter;
    private RequestToken mCurrentLoad;

    private final BaasHandler<List<BaasDocument>> handler =
            new BaasHandler<List<BaasDocument>>() {
                @Override
                public void handle(BaasResult<List<BaasDocument>> result) {
                    complete(result);
                }
            };

// --------------------------- CONSTRUCTORS ---------------------------
    /**
     * Stores away the application context associated with context.
     * Since Loaders can be used across multiple activities it's dangerous to
     * store the context directly; always use {@link #getContext()} to retrieve
     * the Loader's Context, don't use the constructor argument directly.
     * The Context returned by {@link #getContext} is safe to use across
     * Activity instances.
     *
     * @param context used to retrieve the application context.
     */
    public BaasDocumentLoader(Context context, String collection, Filter filter) {
        super(context);
        if (collection == null) throw new IllegalArgumentException("collection cannot be null");
        if (BaasBox.getDefault() == null) {
            throw new IllegalStateException("BaasBox has not been initialized");
        }
        this.mCollection = collection;
        this.mFilter = filter == null ? Filter.ANY : filter;
    }

// -------------------------- OTHER METHODS --------------------------

    void complete(final BaasResult<List<BaasDocument>> result) {
        mCurrentLoad = null;
        mDocuments = result;
        if (isStarted()) {
            deliverResult(mDocuments);
        }
    }

    @Override
    protected void onForceLoad() {
        super.onForceLoad();
        if (mCurrentLoad != null) {
            mCurrentLoad.abort();
        }
        mCurrentLoad = BaasDocument.fetchAll(mCollection, mFilter, handler);
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (mCurrentLoad != null) {
            mCurrentLoad.abort();
        }
        mDocuments = null;
        mCurrentLoad = null;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (mDocuments != null) {
            deliverResult(mDocuments);
        } else if (takeContentChanged() || mCurrentLoad == null) {
            forceLoad();
        }
    }
}
