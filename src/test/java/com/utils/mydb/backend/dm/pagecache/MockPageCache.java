package com.utils.mydb.backend.dm.pagecache;

import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.dm.page.PageImpl;

import java.util.HashMap;
import java.util.Map;

public class MockPageCache implements PageCache {
    private final Map<Integer, Page> cache = new HashMap<>();
    private int pageNo = 0;

    @Override
    public int newPage(byte[] initData) {
        int pgno = ++pageNo;
        Page pg = new PageImpl(pgno, initData, null);
        cache.put(pgno, pg);
        return pgno;
    }

    @Override
    public Page getPage(int pgno) {
        Page pg = cache.get(pgno);
        if(pg == null) {
            throw new RuntimeException("Page not found: " + pgno);
        }
        return pg;
    }

    @Override
    public void close() {
    }

    @Override
    public void release(Page page) {
    }

    @Override
    public void truncateByPageNo(int maxPgno) {

    }

    @Override
    public int getPageNumber() {
        return pageNo;
    }

    @Override
    public void flushPage(Page pg) {
    }
}

