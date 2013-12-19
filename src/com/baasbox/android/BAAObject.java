package com.baasbox.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

/**
 * Base class for BAASBox - managed objects.
 *
 * @param <T> - model class extending BAAObject e.g.  Person extends BAAObject<Person>
 * @author Sergey Pomytkin
 */
public class BAAObject<T extends BAAObject<T>> {
    public String id;
    public String version;
    private static Gson gson = new Gson();
    transient public String collection = this.getClass().getName(); //this.getClass().getSimpleName();

    String toJSON() {
        return gson.toJson(this);
    }

    BAAObject fromJSON(String json) {
        return gson.fromJson(json, this.getClass());
    }


    public void delete(BAASBox bbox, BAASBox.BAASHandler<Void> handler) {
        bbox.deleteDocument(collection, id, handler);
    }

    public void delete(BAASBox bbox, String tag, BAASBox.BAASHandler<Void> handler) {
        bbox.deleteDocument(collection, id, tag, handler);
    }

    public BAASBoxResult<Void> deleteSync(BAASBox bbox) {
        return bbox.deleteDocumentSync(collection, id);
    }

    @Deprecated
    public BAASBoxResult<Void> delete(BAASBox bbox) {
        return bbox.deleteDocumentSync(collection, id);
    }

    @Deprecated
    public BAASBoxResult<T> save(BAASBox bbox) {
        return saveSync(bbox);
    }

    public BAASBoxResult<T> saveSync(BAASBox bbox) {

        JSONObject document = null;
        try {
            document = new JSONObject(gson.toJson(this));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BAASBoxResult<JSONObject> d = bbox.createDocumentSync(collection, document);
        if (d.hasError()) {
            return new BAASBoxResult<T>(d.getError());
        }

        String json = d.getValue().toString();

        @SuppressWarnings("unchecked")
        T rez = (T) gson.fromJson(json, this.getClass());
        return new BAASBoxResult<T>(rez);

    }

    public void save(BAASBox bbox, final BAASBox.BAASHandler<T> handler) {
        save(bbox, null, handler);
    }

    public void save(BAASBox bbox, String tag, final BAASBox.BAASHandler<T> handler) {
        JSONObject document = null;
        try {
            document = new JSONObject(gson.toJson(this));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        bbox.createDocument(collection, document, tag, new BAASBox.BAASHandler<JSONObject>() {
            @Override
            public void handle(BAASBoxResult<JSONObject> result) {
                if (result.hasError()) {
                    handler.handle(BAASBoxResult.<T>failure(result.getError()));
                } else {
                    String json = result.getValue().toString();
                    @SuppressWarnings("unchecked")
                    T rez = (T) gson.fromJson(json, BAAObject.this.getClass());
                    handler.handle(BAASBoxResult.success(rez));
                }
            }
        });
    }

    @Deprecated
    public BAASBoxResult<List<T>> getAll(BAASBox bbox) {
        return getAllSync(bbox);
    }

    public BAASBoxResult<List<T>> getAllSync(BAASBox bbox) {
        BAASBoxResult<JSONArray> r = bbox.getAllDocumentsSync(collection);
        if (r.hasError()) {
            return new BAASBoxResult<List<T>>(r.getError());
        } else {

            List<T> rez = convertJSONArray(r.getValue());
            return new BAASBoxResult<List<T>>(rez);

        }

    }

    public void getAll(BAASBox bbox, String tag, final BAASBox.BAASHandler<List<T>> handler) {
        bbox.getAllDocuments(collection, new BAASBox.BAASHandler<JSONArray>() {
            @Override
            public void handle(BAASBoxResult<JSONArray> r) {
                if (r.hasError()) {
                    handler.handle(BAASBoxResult.<List<T>>failure(r.getError()));
                } else {
                    List<T> rez = convertJSONArray(r.getValue());
                    handler.handle(BAASBoxResult.success(rez));
                }
            }
        });
    }

    private List<T> convertJSONArray(JSONArray jsonArray) {
    /* Stooped working
	  	Type listType = new TypeToken<ArrayList<T>>(){}.getType();
	 
		@SuppressWarnings("unchecked")
		List<T> rez = (List<T>) gson.fromJson(jsonArray.toString(), listType);
	*/


        List<T> rez = new ArrayList<T>();

        for (int i = 0; i < jsonArray.length(); i++) {

            String json = null;
            try {
                json = jsonArray.getJSONObject(i).toString();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            T r = (T) gson.fromJson(json, this.getClass());
            rez.add(r);
        }
        return rez;
    }

}
