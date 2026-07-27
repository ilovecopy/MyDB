package com.utils.mydb.backend.dm.pageindex;

import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.dm.pagecache.PageCache;
import com.utils.mydb.backend.utils.Parser;

import java.util.Arrays;

public class PageX {
    private static final short OF_FREE = 0;
    private static final short OF_DATA = 2;
    public static final int MAX_FREE_SPACE = PageCache.PAGE_SIZE - OF_DATA;

    public static byte[] initRaw() {
        byte[] raw = new byte[PageCache.PAGE_SIZE];
        setFSO(raw, OF_DATA);
        return raw;
    }

    private static void setFSO(byte[] raw, short freeSpaceOffset) {
        System.arraycopy(Parser.short2Byte(freeSpaceOffset), 0, raw, OF_FREE, OF_DATA);
    }

    public static short getFSO(Page pg) {
        return getFSO(pg.getData());
    }

    private static short getFSO(byte[] raw) {
        return Parser.parseShort(Arrays.copyOfRange(raw, OF_FREE, OF_DATA));
    }

    public static short insert(Page pg, byte[] data) {
        pg.setDirty(true);
        short offset = getFSO(pg.getData());
        System.arraycopy(data, 0, pg.getData(), offset, data.length);
        setFSO(pg.getData(), (short) (offset + data.length));
        return offset;
    }

    public static int getFreeSpace(Page pg) {
        return PageCache.PAGE_SIZE - getFSO(pg.getData());
    }

    public static void recoverInsert(Page pg, byte[] raw, short offset) {
        recoverUpdate(pg, raw, offset);
        short rawFSO = getFSO(pg.getData());
        if (rawFSO < offset + raw.length) {
            setFSO(pg.getData(), (short) (offset + raw.length));
        }
    }

    public static void recoverUpdate(Page pg, byte[] raw, short offset) {
        pg.setDirty(true);
        System.arraycopy(raw, 0, pg.getData(), offset, raw.length);
    }
}
