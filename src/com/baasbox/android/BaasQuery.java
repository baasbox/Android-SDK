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
 * A query with specific parameters to a collection endpoint
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
    private final String collOrUsr;
    private final int mode;

    public static Builder builder(){
        return new Builder();
    }

    private BaasQuery(int mode,String collectionOrUser,RequestFactory.Param[] params){
        this.params=params;
        this.mode =mode;
        this.collOrUsr=collectionOrUser;
    }

    public RequestToken query(BaasHandler<List<JsonObject>> handler){
        return query(collOrUsr,Priority.NORMAL,handler);
    }

    public RequestToken query(String what,BaasHandler<List<JsonObject>> handler){
        return query(what,Priority.NORMAL, handler);
    }

    public RequestToken query(Priority priority,BaasHandler<List<JsonObject>> handler){
        return query(collOrUsr,priority,handler);
    }

    public RequestToken query(String what,Priority priority,BaasHandler<List<JsonObject>> handler){
        if (mode == COLLECTIONS && what==null) throw new IllegalArgumentException("collection cannot be null");
        BaasBox box = BaasBox.getDefaultChecked();
        QueryRequest request = new QueryRequest(box,mode,collOrUsr,params,priority,handler);
        return box.submitAsync(request);
    }

    public BaasResult<List<JsonObject>> querySync(){
        return querySync(collOrUsr);
    }

    public BaasResult<List<JsonObject>> querySync(String what){
        if (mode == COLLECTIONS && what==null)throw new IllegalArgumentException("collection cannot be null");
        BaasBox box = BaasBox.getDefaultChecked();
        QueryRequest req = new QueryRequest(box,mode,what,params,null,null);
        return box.submitSync(req);
    }


    private static class QueryRequest extends NetworkTask<List<JsonObject>>{
        private RequestFactory.Param[] params;
        private String endpoint;
        protected QueryRequest(BaasBox box,int mode,String what,RequestFactory.Param[] params, Priority priority, BaasHandler<List<JsonObject>> handler) {
            super(box, priority, handler);
            this.params=params;
            String endpoint;
            switch (mode){
                case COLLECTIONS:
                    endpoint =box.requestFactory.getEndpoint("document/{}",what);
                    break;
                case USERS:
                    endpoint =box.requestFactory.getEndpoint("users");
                    break;
                case FOLLOWERS:
                    if(what == null){
                        endpoint=box.requestFactory.getEndpoint("followers");
                    } else {
                        endpoint=box.requestFactory.getEndpoint("followers/{}");
                    }
                    break;
                case FILES:
                    endpoint = box.requestFactory.getEndpoint("file/details");
                    break;
                case FOLLOWING:
                    if(what == null){
                        endpoint=box.requestFactory.getEndpoint("following");
                    } else {
                        endpoint=box.requestFactory.getEndpoint("following/{}");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unsupported mode");
            }
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
                    //fixme what do i clean?
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

    private static final int COLLECTIONS = 1;
    public static final int USERS = 3;
    public static final int FILES = 2;
    public static final int FOLLOWERS = 4;
    public static final int FOLLOWING = 5;

    public static class Builder{
        private int mMode;
        private StringBuilder whereBuilder;
        private List<CharSequence> params;
        private String sortOrder = null;
        private String fields = null;
        private String groupBy = null;
        private Paging paging = null;
        private String target = null;


        public Filter filter(){
            String where = whereBuilder==null?null:whereBuilder.toString();
            return new Filter(where,params,sortOrder,paging);
        }

        public BaasQuery build(){
            return new BaasQuery(mMode,target,toParams());
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

        public Builder users(){
            mMode = USERS;
            target =null;
            return this;
        }

        public Builder files(){
            mMode = FILES;
            target =null;
            return this;
        }

        public Builder followers(){
            mMode  = FOLLOWERS;
            target = null;
            return this;
        }

        public Builder followers(String userId){
            mMode = FOLLOWERS;
            target =userId;
            return this;
        }

        //fixme add following when baasbox will support it

        public Builder collection(String collection){
            this.mMode = COLLECTIONS;
            target=collection;
            return this;
        }

        public Builder where(String where){
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
//            Logger.error("REQI: %s",this.fields);
            return this;
        }

        public Builder orderBy(String sortOrder){
            this.sortOrder=sortOrder;
            return this;
        }

        public Builder whereParams(String... params){
            if(params!=null&&params.length!=0){
                this.params = new ArrayList<CharSequence>();
                Collections.addAll(this.params, params);
            }
            return this;
        }

        public Builder clearPagination(){
            this.paging=null;
            return this;
        }

        public Builder pagination(int page,int records){
            if(paging==null){
                paging = new Paging(page,records);
            } else {
                paging.page=page;
                paging.records=records;
            }
            return this;
        }

        public Builder groupBy(String groupBy){
            this.groupBy=groupBy;
            return this;
        }
    }
}
