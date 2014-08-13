/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
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

import android.os.Parcelable;

import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrea Tortorella on 13/08/14.
 */
public class BaasLink {

    private String label;
    private BaasObject source;
    private BaasObject destination;
    private String id;
    private String author;
    private String creationDate;
    private long version;

    private BaasLink(String id,String label){
        this.id = id;
        this.label = label;
    }

    public BaasObject in(){
        return source;
    }

    public BaasObject out(){
        return destination;
    }

    public String getLabel(){
        return label;
    }

    public String getId(){
        return id;
    }


    protected static RequestToken link(String label,String sourceId,String destinationId,int flags,BaasHandler<BaasLink> handler){
        if (sourceId==null||sourceId.length()==0) throw new IllegalArgumentException("invalid source");
        if (destinationId==null||destinationId.length()==0) throw new IllegalArgumentException("invalid destination");
        if (label==null||label.length()==0) throw new IllegalArgumentException("invalid label");
        BaasBox cli = BaasBox.getDefaultChecked();
        Create create = new Create(cli,sourceId,destinationId,label,flags,handler);
        return cli.submitAsync(create);
    }

    public RequestToken refresh(int flags,BaasHandler<BaasLink> handler){
        if (id==null) throw new IllegalStateException("this link is not stored on the server");
        BaasBox cli = BaasBox.getDefaultChecked();
        Refresh refresh = new Refresh(cli,this,flags,handler);
        return cli.submitAsync(refresh);
    }

    public static RequestToken fetch(String id,int flags,BaasHandler<BaasLink> handler){
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        BaasLink link = new BaasLink(id,null);
        return link.refresh(flags,handler);
    }


    public RequestToken delete(int flags,BaasHandler<Void> handler){
        if (id == null) throw new IllegalStateException("this link is not bound on the server");
        BaasBox cli  = BaasBox.getDefaultChecked();
        Delete delete = new Delete(cli,this,flags,handler);
        return cli.submitAsync(delete);
    }

    public static RequestToken fetchAllLabeled(String label,int flags,BaasHandler<List<BaasLink>> handler){
        if (label == null||label.length()==0) throw new IllegalArgumentException("invalid label");
        BaasQuery.Criteria criteria = BaasQuery.builder().where("label = ?").whereParams(label).criteria();
        return fetchAll(criteria,flags,handler);
    }


    public static RequestToken fetchAll(String label,BaasQuery.Criteria criteria,int flags,BaasHandler<List<BaasLink>> handler){
        if (label!=null){
            if (criteria == null){
                criteria = BaasQuery.builder().where("label = ?").whereParams(label).criteria();
            } else {
                criteria = criteria.buildUpon().and("label = ?").whereParams(label).criteria();
            }
        }
        return fetchAll(criteria,flags,handler);
    }

    public static RequestToken fetchAll(BaasQuery.Criteria criteria,int flags,BaasHandler<List<BaasLink>> handler){
        if (criteria == null){
            criteria = BaasQuery.Criteria.ANY;
        }
        BaasBox cli  = BaasBox.getDefaultChecked();
        FetchAll all = new FetchAll(cli,criteria,flags,handler);
        return cli.submitAsync(all);
    }


    private static class FetchAll extends NetworkTask<List<BaasLink>>{
        BaasQuery.Criteria criteria;
        protected FetchAll(BaasBox box,BaasQuery.Criteria criteria, int flags, BaasHandler<List<BaasLink>> handler) {
            super(box, flags, handler, true);
            this.criteria =criteria;
        }

        @Override
        protected List<BaasLink> onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonArray data = parseJson(response,box).getArray("data");
            ArrayList<BaasLink> ret = new ArrayList<BaasLink>();
            for (Object o:data){
                JsonObject object =(JsonObject)o;
                BaasLink link = new BaasLink(null,null);
                link.update(object);
                ret.add(link);
            }
            return ret;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("link");
            return box.requestFactory.get(endpoint,criteria.toParams());
        }
    }

    private static class Delete extends NetworkTask<Void>{
        private BaasLink link;
        protected Delete(BaasBox box,BaasLink link, int flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
            this.link = link;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            link.id =null;
            link.label=null;
            link.version=0;
            link.destination=null;
            link.source=null;
            link.author=null;
            link.creationDate=null;
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("link/{}",link.id);
            return box.requestFactory.delete(endpoint);
        }
    }

    private static class Create extends NetworkTask<BaasLink>{
        private final String label;
        private final String source;
        private final String destination;

        protected Create(BaasBox box,String source,String destination,String label, int flags, BaasHandler<BaasLink> handler) {
            super(box, flags, handler,true);
            this.label=label;
            this.source=source;
            this.destination=destination;
        }

        @Override
        protected BaasLink onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response,box).getObject("data");
            BaasLink l = new BaasLink(null,null);
            l.update(data);
            return l;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("link/{}/{}/{}",source,label,destination);
            return box.requestFactory.post(endpoint);
        }
    }

    private static class Refresh extends NetworkTask<BaasLink>{
        private final BaasLink link;

        protected Refresh(BaasBox box,BaasLink link, int flags, BaasHandler<BaasLink> handler) {
            super(box, flags, handler, true);
            this.link = link;
        }

        @Override
        protected BaasLink onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            link.update(data);
            return link;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("link/{}", link.id);
            return box.requestFactory.get(endpoint);
        }
    }

    private void update(JsonObject data) {
        this.label=data.getString("label");
        this.id=data.getString("id");
        this.author=data.getString("_author");
        this.creationDate=data.getString("_creation_date");
        JsonObject in = data.getObject("in");
        JsonObject out = data.getObject("out");
        this.source = parseObject(in);
        this.destination = parseObject(out);
    }

    private BaasObject parseObject(JsonObject object){
        BaasObject ret;
        if (object.contains("@class")){
            ret = new BaasDocument(object);
        } else {
            BaasFile f = new BaasFile();
            f.update(object);
            ret = f;
        }
        return ret;
    }


}
