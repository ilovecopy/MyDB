package com.utils.mydb.backend.common;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.common.Error;
import org.junit.jupiter.api.Test;


class CacheTest {
    static Random random = new SecureRandom();

    private CountDownLatch cdl;
    private MockCache cache;

    @Test
    void testCache() {
        cache = new MockCache();
        cdl = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            Thread t = new Thread(this::work);
            t.start();
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void work() {
        for (int i = 0; i < 1000; i++) {
            long uid = random.nextInt(100);
            long h;
            try {
                h = cache.get(uid);
            } catch (Exception e) {
                if (e == Error.CACHE_FULL_EXCEPTION) continue;
                Panic.panic(e);
                return;
            }
            assert h == uid;
            cache.release(h);
        }
        cdl.countDown();
    }
}

