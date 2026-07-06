USE exam_oj;

-- 管理员账号 admin@exam.com / 123456
INSERT INTO user (id, username, email, password, role) VALUES
(1, 'admin', 'admin@exam.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6n5te', 'admin');

-- 测试用户 test@exam.com / 123456
INSERT INTO user (id, username, email, password, role) VALUES
(2, 'student', 'test@exam.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6n5te', 'user');

-- 示例算法题目
INSERT INTO problem (id, title, content, difficulty, tags, answer, judge_case, judge_config, user_id) VALUES
(1, '两数之和',
'给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出和为目标值 `target` 的那两个整数，并返回它们的数组下标。\n\n你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。\n\n**示例 1：**\n```\n输入：nums = [2,7,11,15], target = 9\n输出：[0,1]\n解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1]\n```',
'简单', '["数组","哈希表"]',
'```java\nimport java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int target = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        Map<Integer, Integer> map = new HashMap<>();\n        for (int i = 0; i < n; i++) {\n            int t = target - nums[i];\n            if (map.containsKey(t)) {\n                System.out.println(map.get(t) + " " + i);\n                return;\n            }\n            map.put(nums[i], i);\n        }\n    }\n}\n```',
'[{"input":"4 9\\n2 7 11 15","output":"0 1"},{"input":"3 6\\n3 2 4","output":"1 2"}]',
'{"timeLimit":2000,"memoryLimit":256}', 1),

(2, '最大子数组和',
'给你一个整数数组 `nums`，请你找出一个具有最大和的连续子数组（子数组最少包含一个元素），返回其最大和。\n\n**示例 1：**\n```\n输入：nums = [-2,1,-3,4,-1,2,1,-5,4]\n输出：6\n解释：连续子数组 [4,-1,2,1] 的和最大，为 6\n```',
'简单', '["动态规划","数组"]',
'```java\nimport java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        int max = nums[0], cur = nums[0];\n        for (int i = 1; i < n; i++) {\n            cur = Math.max(nums[i], cur + nums[i]);\n            max = Math.max(max, cur);\n        }\n        System.out.println(max);\n    }\n}\n```',
'[{"input":"9\\n-2 1 -3 4 -1 2 1 -5 4","output":"6"},{"input":"1\\n1","output":"1"}]',
'{"timeLimit":2000,"memoryLimit":256}', 1),

(3, '反转链表',
'给定单链表的头节点 `head`，请反转链表，并返回反转后的链表。\n\n输入格式：第一行为链表节点数n，第二行为n个整数表示链表节点值，输出反转后的链表节点值。',
'简单', '["链表","递归"]',
'```java\nimport java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        for (int i = n - 1; i >= 0; i--) System.out.print(arr[i] + (i == 0 ? "" : " "));\n    }\n}\n```',
'[{"input":"5\\n1 2 3 4 5","output":"5 4 3 2 1"},{"input":"2\\n1 2","output":"2 1"}]',
'{"timeLimit":2000,"memoryLimit":256}', 1);
