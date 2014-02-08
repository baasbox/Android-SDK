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
 * This class represents a query filter that can be applied
 * to batch requests on collections, users and files.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class Filter {

    StringBuilder where = null;
    ArrayList<CharSequence> params = null;
    String orderBy;
    Paging paging;

    /**
     * A filter that does not apply any restriction to the request.
     */
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


    /**
     * Returns a new filter that applies pagination to the request using
     * the given order, page number and records page.
     *
     * @param order   the field to use for sorting
     * @param asc     true if sorting should be ascending false otherwise
     * @param page    the page number to retrieve
     * @param records the number of entity to return per page
     * @return a configured filter
     */
    public static Filter paging(String order, boolean asc, int page, int records) {
        Filter f = new Filter();
        f.setOrderBy(order + (asc ? " ASC" : " DESC"));
        f.setPaging(page, records);
        return f;
    }

    /**
     * Returns a new filter that applies the given <code>where</code> condition to the request.
     * Where condition can be parameterized using '?', params will be filled using
     * the provided <code>params</code>.
     * Where conditions are simply passed to the server database,
     * their syntax is thus the same of OrientDB see:
     * <a href="https://github.com/orientechnologies/orientdb/wiki/SQL-Where">Orient SQL Where reference</a>
     * for a complete reference.
     *
     * @param where  a string
     * @param params params to fill in the condition
     * @return a configured filter
     */
    public static Filter where(String where, String... params) {
        Filter f = new Filter().setWhere(where, params);
        return f;
    }

    /**
     * Returns a new filter that applies the provided sort order to the request.
     *
     * @param order a field to use for sorting
     * @param asc   true if sorting should be ascending false otherwise
     * @return a configured Filter
     */
    public static Filter sort(String order, boolean asc) {
        Filter f = new Filter().setOrderBy(order + (asc ? " ASC" : " DESC"));
        return f;
    }

    /**
     * Sets the where condition for this filter,
     *
     * @param clause a string
     * @param args   arguments to use in the condition
     * @return this filter with this where condition set
     * @see com.baasbox.android.Filter#where(String, String...)
     */
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

    /**
     * Sets the sort order to use with this filter.
     *
     * @param name
     * @return
     */
    public Filter setOrderBy(String name) {
        this.orderBy = name;
        return this;
    }

    /**
     * Configures pagination for this filter.
     *
     * @param orderBy
     * @param page
     * @param numrecords
     * @return
     */
    public Filter setPaging(String orderBy, int page, int numrecords) {
        this.orderBy = orderBy;
        if (this.paging == null) {
            this.paging = new Paging();
        }
        paging.page = page;
        paging.num = numrecords;
        return this;
    }

    /**
     * Configures pagination for this filter
     *
     * @param page
     * @param numrecords
     * @return
     */
    public Filter setPaging(int page, int numrecords) {
        if (this.paging == null) {
            this.paging = new Paging();
        }
        paging.page = page;
        paging.num = numrecords;
        return this;
    }

    /**
     * Removes the pagination from this filter
     *
     * @return
     */
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
