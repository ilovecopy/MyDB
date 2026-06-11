package com.utils.mydb.backend.common;

import com.utils.mydb.common.Error;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 共享工具箱
 *
 * @author tangjincheng
 * @date 2026/06/10
 */
public abstract class AbstractCache<T> {
    /**
     * （锤子1号，锤子）
     */
    private final HashMap<Long, T> cache;
    /**
     * 每个锤子几个人在用
     */
    private final HashMap<Long, Integer> references;

    /**
     * 锤子正在搬过来（防止从发加载）
     */
    private final HashMap<Long, Boolean> getting;

    /**
     * 最大缓存数量
     */
    private final int maxResource;
    /**
     * 缓存中对象数量（含正在加载但未放入Cache的）
     */
    private int count;
    private final Lock lock;

    protected AbstractCache(int maxResource) {
        this.maxResource = maxResource;
        this.cache = new HashMap<>();
        this.references = new HashMap<>();
        this.getting = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    /**
     * 从缓存拿东西
     *
     * @param key
     * @return {@link T }
     * @throws Exception
     */
    public T get(long key) throws Exception {
        while (true) {
            lock.lock();
            try {
                if (getting.containsKey(key)) {
                    // 锤子正在搬过来
                } else if (cache.containsKey(key)) {
                    T obj = cache.get(key);
                    references.put(key, references.get(key) + 1);
                    return obj;
                } else {
                    if (maxResource > 0 && count == maxResource) {
                        throw Error.CACHE_FULL_EXCEPTION;
                    }
                    count++;//先留个位置
                    getting.put(key, true);//标记锤子正在搬出来
                    break;
                }
            } finally {
                lock.unlock();
            }
            try {
                Thread.sleep(1);//如果该key再加载中，释放锁，1ms后重试
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        T obj;
        try {
            obj = getForCache(key);//找锤子
        } catch (Exception e) {
            lock.lock();
            try {
                count--;
                getting.remove(key);
            } finally {
                lock.unlock();
            }
            throw e;
        }
        lock.lock();
        try {
            getting.remove(key);
            cache.put(key, obj);
            references.put(key, 1);
        } finally {
            lock.unlock();
        }
        return obj;
    }

    /**
     * 不用或放回仓库
     *
     * @param key
     */
    protected void release(long key) {
        lock.lock();
        try {
            int ref = references.get(key) - 1;
            if (ref == 0) {
                T obj = cache.get(key);
                releaseForCache(obj);//放回仓库
                references.remove(key);//这个锤子没人在用
                cache.remove(key);
                count--;//空出一个格子
            } else {
                references.put(key, ref);
            }
        } finally {
            lock.unlock();
        }
    }

    protected void close() {
        lock.lock();
        try {
            Set<Long> keys = cache.keySet();
            Long[] keysArray = keys.toArray(new Long[0]);
            for (long key : keysArray) {
                T obj = cache.get(key);
                releaseForCache(obj);
                references.remove(key);
                cache.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }

    protected abstract T getForCache(long key) throws Exception;

    protected abstract void releaseForCache(T obj);
}
