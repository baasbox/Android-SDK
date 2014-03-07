package com.baasbox.android;


import android.text.TextUtils;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Andrea Tortorella on 07/03/14.
 */
public class BaasQuery {

    static class Paging{
        Paging(int page,int records){
            this.page=page;
            this.records=records;
        }
        int page;
        int records;
    }

    private final RequestFactory.Param[] params;
    private final String collection;

    public static Builder builder(){
        return new Builder();
    }

    private BaasQuery(String collection,RequestFactory.Param[] params){
        this.params=params;
        this.collection=collection;
    }

    public RequestToken query(BaasHandler<List<JsonObject>> handler){
        return query(collection,Priority.NORMAL,handler);
    }

    public RequestToken query(String collection,BaasHandler<List<JsonObject>> handler){
        return query(collection,Priority.NORMAL, handler);
    }

    public RequestToken query(Priority priority,BaasHandler<List<JsonObject>> handler){
        return query(collection,priority,handler);
    }

    public RequestToken query(String collection,Priority priority,BaasHandler<List<JsonObject>> handler){
        if (collection==null) throw new IllegalArgumentException("collection cannot be null");
        BaasBox box = BaasBox.getDefaultChecked();
        String endpoint =box.requestFactory.getEndpoint("document/{}",collection);
        QueryRequest request = new QueryRequest(box,endpoint,params,priority,handler);
        return box.submitAsync(request);
    }

    public BaasResult<List<JsonObject>> querySync(){
        return querySync(collection);
    }

    public BaasResult<List<JsonObject>> querySync(String collection){
        if (collection==null)throw new IllegalArgumentException("collection cannot be null");
        BaasBox box = BaasBox.getDefaultChecked();
        String endpoint = box.requestFactory.getEndpoint("document/{}",collection);
        QueryRequest req = new QueryRequest(box,endpoint,params,null,null);
        return box.submitSync(req);
    }

    private static class QueryRequest extends NetworkTask<List<JsonObject>>{
        private RequestFactory.Param[] params;
        private String endpoint;
        protected QueryRequest(BaasBox box,String endpoint,RequestFactory.Param[] params, Priority priority, BaasHandler<List<JsonObject>> handler) {
            super(box, priority, handler);
            this.params=params;
            this.endpoint=endpoint;
        }

        @Override
        protected List<JsonObject> onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonArray a=parseJson(response,box).getArray("data");
            List<JsonObject> r = new ArrayList<JsonObject>();
            for(Object o:a){
                if(o instanceof JsonObject){
                    JsonObject jo = (JsonObject)o;
                    jo.remove("@rid");
                    r.add(jo);
                } else {
                    throw new BaasIOException("unable to parse json");
                }
            }
            return r;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.get(endpoint,params);
        }
    }

    public static class Builder{
        private StringBuilder whereBuilder;
        private List<CharSequence> params;
        private String sortOrder = null;
        private String fields = null;
        private String groupBy = null;
        private Paging paging = null;
        private String boundCollection = null;

        public Filter filter(){
            String where = whereBuilder==null?null:whereBuilder.toString();
            return new Filter(where,params,sortOrder,paging);
        }

        public BaasQuery build(){
            return new BaasQuery(boundCollection,toParams());
        }

        private void validate(){
            if (paging!=null){
                if (TextUtils.isEmpty(sortOrder)) throw new IllegalStateException("paging requires a sort order");
            }
        }

        private RequestFactory.Param[] toParams(){
            validate();
            List<RequestFactory.Param> reqParams = new ArrayList<RequestFactory.Param>();
            if (whereBuilder!=null){
                reqParams.add(new RequestFactory.Param("where",whereBuilder.toString()));
                if (params!=null){
                    for(CharSequence p: params){
                        reqParams.add(new RequestFactory.Param("params",p.toString()));
                    }
                }
            }
            if (sortOrder!=null){
                reqParams.add(new RequestFactory.Param("orderBy",sortOrder));
            }
            if (paging!=null){
                reqParams.add(new RequestFactory.Param("page",Integer.toString(paging.page)));
                reqParams.add(new RequestFactory.Param("recordsPerPage",Integer.toString(paging.records)));
            }
            if (groupBy!=null){
                reqParams.add(new RequestFactory.Param("groupBy",groupBy));
            }
            if (fields!=null){
                Logger.error("REQ: %s",fields);
                reqParams.add(new RequestFactory.Param("fields",fields));
            }
            if (reqParams.size()==0) return null;
            return reqParams.toArray(new RequestFactory.Param[reqParams.size()]);
        }

        public Builder collection(String collection){
            this.boundCollection=collection;
            return this;
        }

        public Builder appendWhere(String where){
            if (where!=null){
                if(whereBuilder ==null){
                    whereBuilder = new StringBuilder(where.length()+16);
                }
                whereBuilder.append(where);
            }
            return this;
        }

        public Builder projection(String ...fields){
            if(fields==null){
                this.fields=null;
            } else {
                StringBuilder sb = new StringBuilder();
                for(String f:fields){
                    sb.append(f).append(',');
                }
                sb.setLength(sb.length()-1);
                this.fields= sb.toString();//Arrays.toString(fields);//.substring(1,fields.length-1);
            }
            Logger.error("REQI: %s",this.fields);
            return this;
        }

        public Builder orderBy(String sortOrder){
            this.sortOrder=sortOrder;
            return this;
        }

        public Builder setWhereParams(String ...params){
            if(params!=null&&params.length==0){
                this.params = new ArrayList<CharSequence>();
                Collections.addAll(this.params, params);
            }
            return this;
        }

        public Builder clearPagination(){
            this.paging=null;
            return this;
        }

        public Builder setPagination(int page,int records){
            if(paging==null){
                paging = new Paging(page,records);
            } else {
                paging.page=page;
                paging.records=records;
            }
            return this;
        }

        public Builder setGroupBy(String groupBy){
            this.groupBy=groupBy;
            return this;
        }
    }
}
