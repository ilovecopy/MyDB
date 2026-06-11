package com.utils.mydb.backend.dm.pagecache;

import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.utils.RandomUtil;
import org.junit.jupiter.api.Test;


class PageCacheTest {
    static Random random = new SecureRandom();

    @Test
    void testPageCacheCreateOpen() throws Exception {
        String path = "/tmp/pcache_rebuild_test0";
        new File(path + ".db").delete();

        PageCache pc = PageCache.create(path, (long) PageCache.PAGE_SIZE * 50);
        for (int i = 0; i < 30; i++) {
            byte[] tmp = new byte[PageCache.PAGE_SIZE];
            tmp[0] = (byte) i;
            int pgno = pc.newPage(tmp);
            Page pg = pc.getPage(pgno);
            pg.setDirty(true);
            pg.release();
        }
        pc.close();

        pc = PageCache.open(path, (long) PageCache.PAGE_SIZE * 50);
        for (int i = 1; i <= 30; i++) {
            Page pg = pc.getPage(i);
            assert pg.getData()[0] == (byte) (i - 1);
            pg.release();
        }
        pc.close();

        assert new File(path + ".db").delete();
    }

    private PageCache pc2;
    private PageCache mpc;
    private CountDownLatch cdl2;
    private AtomicInteger noPages2;
    private Lock lockNew;

    @Test
    void testPageCacheMulti() throws Exception {
        String path = "/tmp/pcache_rebuild_test1";
        new File(path + ".db").delete();

        pc2 = PageCache.create(path, (long) PageCache.PAGE_SIZE * 10);
        mpc = new MockPageCache();
        lockNew = new ReentrantLock();

        cdl2 = new CountDownLatch(10);
        noPages2 = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(this::worker2);
            t.start();
        }
        cdl2.await();

        pc2.close();
        assert new File(path + ".db").delete();
    }

    private void worker2() {
        try {
            for (int i = 0; i < 200; i++) {
                int op = Math.abs(random.nextInt() % 20);
                if (op == 0) {
                    byte[] data = RandomUtil.randomBytes(PageCache.PAGE_SIZE);
                    lockNew.lock();
                    try {
                        int pgno = pc2.newPage(data);
                        int mpgno = mpc.newPage(data);
                        assert pgno == mpgno;
                        noPages2.incrementAndGet();
                    } finally {
                        lockNew.unlock();
                    }
                } else if (op < 10) {
                    int mod = noPages2.intValue();
                    if (mod == 0) continue;
                    int pgno = Math.abs(random.nextInt()) % mod + 1;
                    Page pg = pc2.getPage(pgno);
                    Page mpg = mpc.getPage(pgno);
                    pg.lock();
                    try {
                        assert Arrays.equals(mpg.getData(), pg.getData());
                    } finally {
                        pg.unlock();
                        pg.release();
                    }
                } else {
                    int mod = noPages2.intValue();
                    if (mod == 0) continue;
                    int pgno = Math.abs(random.nextInt()) % mod + 1;
                    Page pg = pc2.getPage(pgno);
                    Page mpg = mpc.getPage(pgno);
                    byte[] newData = RandomUtil.randomBytes(PageCache.PAGE_SIZE);
                    pg.lock();
                    try {
                        mpg.setDirty(true);
                        System.arraycopy(newData, 0, mpg.getData(), 0, PageCache.PAGE_SIZE);
                        pg.setDirty(true);
                        System.arraycopy(newData, 0, pg.getData(), 0, PageCache.PAGE_SIZE);
                    } finally {
                        pg.unlock();
                        pg.release();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            cdl2.countDown();
        }
    }
}

