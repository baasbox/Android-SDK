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
 * @author Sergey Pomytkin
 *
 * @param <T> - model class extending BAAObject e.g.  Person extends BAAObject<Person>
 */
public class BAAObject<T> {
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
	 
	 public BAASBoxResult<Void> delete(BAASBox bbox) {	
			return bbox.deleteDocument(collection, id);
		}
	 
	 public BAASBoxResult<T> save(BAASBox bbox) {
		
			JSONObject document = null;
			try {
				document = new JSONObject( gson.toJson(this));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BAASBoxResult<JSONObject> d = bbox.createDocument(collection, document);
			if(d.hasError()){
				return new BAASBoxResult<T>(d.getError());
			}
			
			String json = d.getValue().toString();
			
			@SuppressWarnings("unchecked")
			T rez = (T) gson.fromJson(json, this.getClass());
			return new BAASBoxResult<T>(rez);
		
		}

	 public BAASBoxResult<List<T>> getAll(BAASBox bbox) {
		 	BAASBoxResult<JSONArray> r = bbox.getAllDocuments(collection);
		 	if(r.hasError()){
		 		return new BAASBoxResult<List<T>>(r.getError()) ;
		 	}else{
	
					List<T> rez = convertJSONArray(r.getValue());
				  return new BAASBoxResult<List<T>>(rez) ;
				
		 	}

		}
	 
	private List<T> convertJSONArray(JSONArray jsonArray) {
	/* Stooped working
	  	Type listType = new TypeToken<ArrayList<T>>(){}.getType();
	 
		@SuppressWarnings("unchecked")
		List<T> rez = (List<T>) gson.fromJson(jsonArray.toString(), listType);
	*/


		List<T> rez=new ArrayList<T>();
		
		for (int i = 0; i < jsonArray.length(); i++){
			
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
