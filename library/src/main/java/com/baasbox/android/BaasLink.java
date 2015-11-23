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

import android.os.Parcel;
import android.os.Parcelable;

import com.baasbox.android.impl.Util;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.net.HttpRequest;

import com.baasbox.android.net.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a BaasBox Link between objects
 *
 * Created by Andrea Tortorella on 13/08/14.
 */
public final class BaasLink implements Parcelable{


    public enum Direction{
        TO,FROM,BOTH
    }

    private String label;
    private BaasObject source;
    private BaasObject destination;
    private String id;
    private String author;

    private String creationDate;
    private long version;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Util.writeOptString(dest,id);
        Util.writeOptString(dest,label);
        Util.writeOptString(dest,author);
        Util.writeOptString(dest,creationDate);
        dest.writeLong(version);
        writeObject(dest,source);
        writeObject(dest,destination);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private BaasLink(Parcel parcel){
        id = Util.readOptString(parcel);
        label = Util.readOptString(parcel);
        author = Util.readOptString(parcel);
        creationDate = Util.readOptString(parcel);
        version = parcel.readLong();
        source = readObject(parcel);
        destination = readObject(parcel);
    }

    public static final Creator<BaasLink> CREATOR = new Creator<BaasLink>() {
        @Override
        public BaasLink createFromParcel(Parcel source) {
            return new BaasLink(source);
        }

        @Override
        public BaasLink[] newArray(int size) {

            return new BaasLink[size];
        }
    };

    private static BaasObject readObject(Parcel parcel){
        byte b = parcel.readByte();
        if (b == 0) return null;
        if (b==1) {
            BaasDocument d = parcel.readParcelable(BaasDocument.class.getClassLoader());
            return d;
        } else if (b == 2){
            BaasFile f = parcel.readParcelable(BaasFile.class.getClassLoader());
            return f;
        }
        return null;
    }
    private static void writeObject(Parcel p,BaasObject o){
        if (o == null){
            p.writeByte((byte)0);
        } else {
            if (o instanceof BaasDocument){
                p.writeByte((byte)1);
            } else if (o instanceof BaasFile){
                p.writeByte((byte)2);
            }
            p.writeParcelable(o,0);
        }
    }

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

    public String getAuthor(){ return author;}

    public String getCreationDate(){return creationDate;}

    public long getVersion(){return version;}

    static RequestToken makeLink(String label, String sourceId, String destinationId, int flags, BaasHandler<BaasLink> handler){
        if (sourceId==null||sourceId.length()==0) throw new IllegalArgumentException("invalid source");
        if (destinationId==null||destinationId.length()==0) throw new IllegalArgumentException("invalid destination");
        if (label==null||label.length()==0) throw new IllegalArgumentException("invalid label");
        BaasBox cli = BaasBox.getDefaultChecked();
        Create create = new Create(cli,sourceId,destinationId,label,flags,handler);
        return cli.submitAsync(create);
    }

    static BaasResult<BaasLink> makeLinkSync(String label, String sourceId, String destinationId){
        if (sourceId==null||sourceId.length()==0) throw new IllegalArgumentException("invalid source");
        if (destinationId==null||destinationId.length()==0) throw new IllegalArgumentException("invalid destination");
        if (label==null||label.length()==0) throw new IllegalArgumentException("invalid label");
        BaasBox cli = BaasBox.getDefaultChecked();
        Create create = new Create(cli,sourceId,destinationId,label,RequestOptions.DEFAULT,null);
        return cli.submitSync(create);
    }


    public static BaasResult<BaasLink> createSync(String label,String source,String destination){
        return makeLinkSync(label,source,destination);
    }


    public static BaasResult<BaasLink> createSync(String label,BaasObject source,BaasObject destination){
        return makeLinkSync(label,source.getId(),destination.getId());
    }

    public static RequestToken create(String label,String source,String destination,int flags,BaasHandler<BaasLink> handler){
        return makeLink(label, source, destination, flags, handler);
    }

    public static RequestToken create(String label,BaasObject source,BaasObject destination,int flags,BaasHandler<BaasLink> handler){
        return  makeLink(label,source.getId(),destination.getId(),flags,handler);
    }


    public RequestToken refresh(int flags,BaasHandler<BaasLink> handler){
        if (id==null) throw new IllegalStateException("this link is not stored on the server");
        BaasBox cli = BaasBox.getDefaultChecked();
        Refresh refresh = new Refresh(cli,this,flags,handler);
        return cli.submitAsync(refresh);
    }

    public BaasResult<BaasLink> refreshSync(){
        if (id==null) throw new IllegalStateException("this link is not stored on the server");
        BaasBox cli = BaasBox.getDefaultChecked();
        Refresh refresh = new Refresh(cli,this,RequestOptions.DEFAULT,null);
        return cli.submitSync(refresh);
    }

    public static RequestToken fetch(String id,int flags,BaasHandler<BaasLink> handler){
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        BaasLink link = new BaasLink(id,null);
        return link.refresh(flags,handler);
    }

    public static BaasResult<BaasLink> fetchSync(String id){
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        BaasLink link = new BaasLink(id,null);
        return link.refreshSync();
    }


    public RequestToken delete(int flags,BaasHandler<Void> handler){
        if (id == null) throw new IllegalStateException("this link is not bound on the server");
        BaasBox cli  = BaasBox.getDefaultChecked();
        Delete delete = new Delete(cli,this,flags,handler);
        return cli.submitAsync(delete);
    }

    public BaasResult<Void> deleteSync(){
        if (id==null) throw new IllegalArgumentException("this link is not bound on the server");
        BaasBox cli = BaasBox.getDefaultChecked();
        Delete delete = new Delete(cli,this,RequestOptions.DEFAULT,null);
        return cli.submitSync(delete);
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

    public static BaasResult<List<BaasLink>> fetchAllSync(String label,BaasQuery.Criteria criteria){
        if (label!=null){
            if (criteria == null){
                criteria = BaasQuery.builder().where("label = ?").whereParams(label).criteria();
            } else {
                criteria = criteria.buildUpon().and("label = ?").whereParams(label).criteria();
            }
        }
        return fetchAllSync(criteria);
    }


    public static RequestToken fetchAll(BaasQuery.Criteria criteria,int flags,BaasHandler<List<BaasLink>> handler){
        if (criteria == null){
            criteria = BaasQuery.Criteria.ANY;
        }
        BaasBox cli  = BaasBox.getDefaultChecked();
        FetchAll all = new FetchAll(cli,criteria,flags,handler);
        return cli.submitAsync(all);
    }

    public static BaasResult<List<BaasLink>> fetchAllSync(BaasQuery.Criteria criteria){
        if (criteria == null) {
            criteria = BaasQuery.Criteria.ANY;
        }
        BaasBox cli = BaasBox.getDefaultChecked();
        FetchAll all = new FetchAll(cli,criteria,RequestOptions.DEFAULT,null);
        return cli.submitSync(all);
    }

    public static BaasLink withId(String id) {
        return new BaasLink(id,null);
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
        private final BaasLink link;
        private final String id;
        protected Delete(BaasBox box,BaasLink link, int flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
            this.link = link;
            this.id=link.id;
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
            String endpoint = box.requestFactory.getEndpoint("link/{}",id);
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

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.put("label",this.label);
        o.put("id",this.id);
        o.put("author",this.author);
        o.put("_creation_date",this.creationDate);
        o.put("@version",this.version);
        o.put("out",this.source.toJson());
        o.put("in",this.destination.toJson());
        return o;
    }

    private void update(JsonObject data) {
        this.label=data.getString("label");
        this.id=data.getString("id");
        this.author=data.getString("_author");
        this.creationDate=data.getString("_creation_date");
        this.version = data.getLong("@version");
        Object in = data.get("out");
        Object out = data.get("in");
//        JsonObject in = data.getObject("out");
//        JsonObject out = data.getObject("in");
        this.source = parseObject(in,out);
        this.destination = parseObject(out,in);
    }

    private BaasObject parseObject(Object def, Object alternate){

        if (def == null) return null;
        if (def instanceof String){
            return parseObject(alternate,null);
        } else if (def instanceof JsonObject) {
            JsonObject object = (JsonObject)def;
            BaasObject ret;
            if (object.contains("@class")) {
                ret = new BaasDocument(object);
            } else {
                BaasFile f = new BaasFile();
                f.update(object);
                ret = f;
            }
            return ret;
        } else {
            return null;
        }
    }


}
