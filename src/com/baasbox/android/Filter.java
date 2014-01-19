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
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import java.util.ArrayList;

/**
 * Created by Andrea Tortorella on 15/01/14.
 */
public class Filter {

    StringBuilder where = null;
    ArrayList<CharSequence> params = null;
    String orderBy;
    Paging paging;

    public final static Filter ANY = new Filter() {
        @Override
        RequestFactory.Param[] toParams() {
            return null;
        }
    };

    private static class Paging {
        int page;
        int num;
    }


    public static Filter paging(String order, boolean asc, int page, int records) {
        Filter f = new Filter();
        f.setOrderBy(order + (asc ? " ASC" : " DESC"));
        f.setPaging(page, records);
        return f;
    }

    public static Filter where(String where, String... params) {
        Filter f = new Filter().setWhere(where, params);
        return f;
    }

    public static Filter sort(String order, boolean asc) {
        Filter f = new Filter().setOrderBy(order + (asc ? " ASC" : " DESC"));
        return f;
    }

    public Filter setWhere(CharSequence clause, CharSequence... args) {
        where = null;
        if (clause == null) return this;

        where = new StringBuilder(clause.length() + 16);
        where.append(clause);
        if (args != null) {
            if (params == null) {
                params = new ArrayList<CharSequence>(args.length);
            } else {
                params.clear();
            }
            for (CharSequence a : args) {
                params.add(a);
            }
        } else {
            if (params != null) {
                params.clear();
            }
        }
        return this;
    }

    public Filter setOrderBy(String name) {
        this.orderBy = name;
        return this;
    }

    public Filter setPaging(String orderBy, int page, int numrecords) {
        this.orderBy = orderBy;
        if (this.paging == null) {
            this.paging = new Paging();
        }
        paging.page = page;
        paging.num = numrecords;
        return this;
    }

    public Filter setPaging(int page, int numrecords) {
        if (this.paging == null) {
            this.paging = new Paging();
        }
        paging.page = page;
        paging.num = numrecords;
        return this;
    }

    public Filter clearPaging() {
        this.paging = null;
        return this;
    }

    RequestFactory.Param[] toParams() {
        validate();
        ArrayList<RequestFactory.Param> reqParams = new ArrayList<RequestFactory.Param>();
        if (where != null) {
            reqParams.add(new RequestFactory.Param("where", where.toString()));
            if (params != null) {
                for (CharSequence p : params) {
                    reqParams.add(new RequestFactory.Param("params", p.toString()));
                }
            }
        }
        if (orderBy != null) {
            reqParams.add(new RequestFactory.Param("orderBy", orderBy.toString()));
        }
        if (paging != null) {
            reqParams.add(new RequestFactory.Param("paging", Integer.toString(paging.page)));
            reqParams.add(new RequestFactory.Param("recordPerPage", Integer.toString(paging.num)));
        }
        if (reqParams.size() == 0) return null;
        return reqParams.toArray(new RequestFactory.Param[reqParams.size()]);
    }

    private int countParams() {
        int count = 0;
        if (where != null) {
            count += 1;
            if (params != null) {
                count += params.size();
            }
        }
        if (orderBy != null) {
            count += 1;
        }
        if (paging != null) {
            count += 2;
        }
        return count;
    }

    private void validate() {
        if (paging != null) {
            if (orderBy == null) throw new IllegalArgumentException("paging requires order by");
        }
    }
}
