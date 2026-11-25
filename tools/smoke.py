"""Exercise the running local service with trusted fixture programs."""
import json
import os
import urllib.error
import urllib.request
import uuid


BASE = os.environ.get("OJ_TEST_URL", "http://127.0.0.1:8080") + "/api/v2"
EXERCISE = "/exercises/lantern-count"
PROGRAMS = {
    "JAVA": """import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt(), prev = in.nextInt(), changes = 0;
        for (int i = 1; i < n; i++) {
            int next = in.nextInt();
            if (next != prev) changes++;
            prev = next;
        }
        System.out.println(changes);
    }
}
""",
    "C": """#include <stdio.h>
int main(void) {
    int n, prev, next, changes = 0;
    if (scanf("%d%d", &n, &prev) != 2) return 1;
    for (int i = 1; i < n; ++i) {
        if (scanf("%d", &next) != 1) return 1;
        changes += next != prev;
        prev = next;
    }
    printf("%d\\n", changes);
    return 0;
}
""",
    "CPP": """#include <iostream>
int main() {
    int n, prev, next, changes = 0;
    if (!(std::cin >> n >> prev)) return 1;
    for (int i = 1; i < n; ++i) {
        if (!(std::cin >> next)) return 1;
        changes += next != prev;
        prev = next;
    }
    std::cout << changes << std::endl;
}
""",
}


def send(path, method="GET", body=None, status=200, origin=None):
    payload = None if body is None else json.dumps(body).encode()
    headers = {"Content-Type": "application/json"} if payload is not None else {}
    if origin:
        headers["Origin"] = origin
    req = urllib.request.Request(BASE + path, data=payload, method=method, headers=headers)
    try:
        response = urllib.request.urlopen(req, timeout=75)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        actual = response.code
        content = response.read()
    assert actual == status, (method, path, status, actual, content[:500])
    return json.loads(content) if content else None


def main():
    view = send(EXERCISE)
    assert len(view["examples"]) == 2 and "cases" not in view
    for language, code in PROGRAMS.items():
        response = send(EXERCISE + "/attempts", "POST", {"language": language, "source": code}, status=201)
        check = response["result"]
        assert check["verdict"] == "ACCEPTED" and len(check["checks"]) == 4, response
        assert all(not row["output"] for row in check["checks"] if not row["example"])
        detail = send("/attempts/" + str(response["summary"]["id"]))
        assert detail == response
        print(language, "accepted, stored and read back")

    before = send("/attempts?exercise=lantern-count")
    result = send(EXERCISE + "/trials", "POST", {
        "language": "JAVA", "source": PROGRAMS["JAVA"], "input": "4\n1 0 1 0\n",
    })
    assert result["verdict"] == "ACCEPTED" and result["checks"][0]["output"].strip() == "3"
    assert send("/attempts?exercise=lantern-count") == before
    for program, expected in [
        ("bad C source", "COMPILE_ERROR"),
        ("#include <stdio.h>\nint main(void) { puts(\"999\"); }", "WRONG_OUTPUT"),
        ("int main(void) { return 7; }", "RUNTIME_ERROR"),
        ("int main(void) { for (;;) {} }", "TIME_LIMIT"),
        ("#include <stdio.h>\nint main(void) { for (int n=0;n<50000;n++) putchar('x'); }", "OUTPUT_LIMIT"),
    ]:
        row = send(EXERCISE + "/attempts", "POST", {"language": "C", "source": program}, status=201)
        assert row["result"]["verdict"] == expected, row
        print(expected)
    send(EXERCISE + "/attempts", "POST", {"language": "C", "source": "int main(void) {}"},
         origin="https://example.test", status=403)
    send(EXERCISE + "/attempts", "POST", {"language": "OTHER", "source": "test"}, status=400)
    send(EXERCISE + "/attempts", "POST", {"language": "C", "source": "灯" * 11000}, status=400)
    print("trial, history and input validation checked")

    slug = "smoke-" + uuid.uuid4().hex[:12]
    draft = {"slug": slug, "title": "Verification fixture", "statement": "Output the number 6.", "archived": False,
             "policy": {"timeMs": 500, "outputBytes": 512, "comparison": "TOKENS"},
             "cases": [{"example": False, "input": "", "expected": "6"}]}
    path = "/authoring/exercises"
    send(path, "POST", draft, status=201)
    try:
        send(path, "POST", draft, status=409)
        assert not send("/exercises/" + slug)["examples"]
        stored = send(path + "/" + slug)
        assert stored["cases"] == draft["cases"]
        draft["title"] = "Updated verification fixture"
        send(path + "/" + slug, "PUT", draft)
        assert send("/exercises/" + slug)["title"] == draft["title"]
    finally:
        draft["archived"] = True
        send(path + "/" + slug, "PUT", draft)
    send("/exercises/" + slug, status=404)
    assert send(path + "/" + slug)["archived"]
    print("authoring, example visibility and archive checked")


if __name__ == "__main__":
    main()
