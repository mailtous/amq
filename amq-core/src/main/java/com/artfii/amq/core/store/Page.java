package com.artfii.amq.core.store;

import java.io.Serializable;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/3/26.
 */
public class Page<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int default_page_size = 10;
    private int pageNumber = 1;
    private int pageSize = default_page_size;
    private List<T> items;
    private int total;

    public Page() {
    }

    public Page(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    //============================== No POJO =======================================

    public int first() {
        return first(this.pageNumber, this.pageSize);
    }

    public long totalPages() {
        return totalPages(this.total, this.pageSize);
    }

    public int limit() {
        return limit(this.total,this.pageNumber, this.pageSize);
    }

    public static int first(int pageNumber, int pageSize) {
        pageNumber = pageNumber < 0 ? 1 : pageNumber;
        return (pageNumber - 1) * pageSize;
    }

    public static long totalPages(int total, int pageSize) {
        if (total < 0) return 0;
        long count = total / pageSize;
        return (total % pageSize > 0) ? count++ : count;
    }

    public static int limit(int total, int pageNumber, int pageSize) {
        return (pageNumber * pageSize > total) ? (total % pageSize) : pageSize;
    }

    //============================= GETTER && SETTER ===================================

    public int getPageNumber() {
        return pageNumber;
    }

    public Page setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Page setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public int getTotal() {
        return total;
    }

    public Page setTotal(int total) {
        this.total = total;
        return this;
    }

    public List<T> getItems() {
        return items;
    }

    public Page<T> setItems(List<T> items) {
        this.items = items;
        return this;
    }
}
