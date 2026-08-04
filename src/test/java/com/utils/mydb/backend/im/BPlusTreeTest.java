package com.utils.mydb.backend.im;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import com.utils.mydb.backend.dm.DataManager;
import com.utils.mydb.backend.dm.pagecache.PageCache;
import com.utils.mydb.backend.dm.tm.TransactionManager;
import com.utils.mydb.backend.tm.MockTransactionManager;
import com.utils.mydb.backend.utils.Panic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class BPlusTreeTest {

    private String newTestPath(String prefix) throws IOException {
        Path dir = Files.createTempDirectory(prefix);
        return dir.resolve("db").toString();
    }

    private void cleanupTestPath(String path) {
        Path dir = Paths.get(path).getParent();
        if(dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    @Test
    void testTreeSingle() throws Exception {
        String path = newTestPath("TestTreeSingle");
        TransactionManager tm = new MockTransactionManager();
        DataManager dm = DataManager.create(path, PageCache.PAGE_SIZE * 10, tm);

        try {
            long root = BPlusTree.create(dm);
            BPlusTree tree = BPlusTree.load(root, dm);

            int lim = 10000;
            for(int i = lim - 1; i >= 0; i--) {
                tree.insert(i, i);
            }

            for(int i = 0; i < lim; i++) {
                List<Long> uids = tree.search(i);
                assertEquals(1, uids.size());
                assertEquals((long)i, (long)uids.get(0));
            }

            List<Long> range = tree.searchRange(100, 199);
            assertEquals(100, range.size());
            for(int i = 0; i < range.size(); i++) {
                assertEquals(100L + i, (long)range.get(i));
            }

            tree.close();
        } finally {
            dm.close();
            tm.close();
            cleanupTestPath(path);
        }
    }
}
