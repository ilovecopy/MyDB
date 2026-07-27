package com.utils.mydb.backend.dm.pageindex;

import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.dm.pagecache.PageCache;
import com.utils.mydb.backend.utils.RandomUtil;

import java.util.Arrays;

/**
 * @author tangjincheng
 * @date 2026/07/27
 */
public class PageOne {
    private static final int OF_VC = 100;
    private static final int LEN_VC = 8;

    public static byte[] initRaw() {
        byte[] raw = new byte[PageCache.PAGE_SIZE];
        setVcOpen(raw);
        return raw;
    }

    public static void setVcOpen(Page pc) {
        pc.setDirty(true);
        setVcOpen(pc.getData());
    }

    private static void setVcOpen(byte[] raw) {
        System.arraycopy(RandomUtil.randomBytes(LEN_VC), 0, raw, OF_VC, LEN_VC);
    }

    public static void setVcClose(Page pc) {
        pc.setDirty(true);
        setVcClose(pc.getData());
    }

    private static void setVcClose(byte[] raw) {
        System.arraycopy(new byte[LEN_VC], 0, raw, OF_VC, LEN_VC);
    }

    public static boolean checkVc(Page pc) {
        return checkVc(pc.getData());
    }

    private static boolean checkVc(byte[] raw) {
        return Arrays.equals(Arrays.copyOfRange(raw, OF_VC, OF_VC + LEN_VC),
                Arrays.copyOfRange(raw, OF_VC + LEN_VC, OF_VC + 2 * LEN_VC));
    }
}
