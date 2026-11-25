package io.multilang.oj.catalog;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record Exercise(long id, String slug, String title, String statement, boolean archived,
                       Policy policy, List<Case> cases) {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public Exercise {
        require(slug != null && slug.length() <= 72 && SLUG.matcher(slug).matches(),
                "题目标识只支持小写字母、数字和连字符");
        require(title != null && !title.isBlank() && title.length() <= 120, "题目标题须为 1–120 个字符");
        require(statement != null && !statement.isBlank() && utf8Size(statement) <= 30000,
                "题目说明不能为空且不能超过 30 KB");
        require(policy != null, "请填写执行规则");
        require(cases != null && !cases.isEmpty() && cases.size() <= 20, "每题须有 1–20 个用例");
        require(cases.stream().allMatch(Objects::nonNull), "用例不能为空");
        cases = List.copyOf(cases);
    }

    public enum Comparison { EXACT, TOKENS }

    public record Policy(Integer timeMs, Integer outputBytes, Comparison comparison) {
        public Policy {
            require(timeMs != null && timeMs >= 100 && timeMs <= 5000, "运行时限须为 100–5000 毫秒");
            require(outputBytes != null && outputBytes >= 256 && outputBytes <= 16384,
                    "输出限制须为 256–16384 字节");
            require(comparison != null, "请指定输出比较方式");
        }
    }

    public record Case(Boolean example, String input, String expected) {
        public Case {
            require(example != null && input != null && expected != null, "用例须包含输入、期望输出和示例标记");
            require(utf8Size(input) <= 8192 && utf8Size(expected) <= 8192, "每个用例的输入和期望输出须在 8 KB 内");
        }
    }

    public static int utf8Size(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
