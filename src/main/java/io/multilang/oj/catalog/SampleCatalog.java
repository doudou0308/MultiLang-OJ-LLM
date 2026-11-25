package io.multilang.oj.catalog;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
class SampleCatalog implements ApplicationRunner {
    private final Catalog catalog;
    private final boolean enabled;

    SampleCatalog(Catalog catalog, @Value("${OJ_SEED_SAMPLE:true}") boolean enabled) {
        this.catalog = catalog;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled || !catalog.list(true).isEmpty()) {
            return;
        }
        Exercise starter = new Exercise(0, "lantern-count", "山径灯笼", """
                山径上依次有 n 盏灯笼。第 i 盏灯笼亮起时记为 1，熄灭时记为 0。
                请统计相邻的两盏灯笼中，恰好一盏亮着的相邻对有多少。

                输入
                第一行是 n（1 ≤ n ≤ 1000）。
                第二行是 n 个由空格分隔的整数，每个数只能为 0 或 1。

                输出
                一个整数，表示状态不同的相邻对数。
                """, false, new Exercise.Policy(2000, 4096, Exercise.Comparison.TOKENS), List.of(
                new Exercise.Case(true, "5\n0 1 1 0 1\n", "3\n"),
                new Exercise.Case(true, "1\n1\n", "0\n"),
                new Exercise.Case(false, "6\n1 1 1 1 1 1\n", "0\n"),
                new Exercise.Case(false, "4\n1 0 1 0\n", "3\n")));
        try {
            catalog.create(starter);
        } catch (DuplicateKeyException ignored) {
            // Another startup already installed the example.
        }
    }
}
