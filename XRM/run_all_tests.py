import subprocess
import json
import os
import re
import sys

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TEST_SRC_DIR = os.path.join(BASE_DIR, "src", "test", "java", "mljy")
REPORT_BASE_DIR = os.path.join(BASE_DIR, "开发需求文档", "测试报告", "XRM")

def discover_test_classes():
    classes = []
    for root, dirs, files in os.walk(TEST_SRC_DIR):
        for f in files:
            if f.endswith("测试.java") or f == "暮澜纪元插件测试.java":
                rel_path = os.path.relpath(os.path.join(root, f), TEST_SRC_DIR)
                package_path = rel_path.replace(os.sep, ".").replace(".java", "")
                full_class = "mljy." + package_path
                classes.append((full_class, rel_path, f.replace(".java", "")))
    return classes

def run_test_class(class_name):
    cmd = [
        os.path.join(BASE_DIR, "gradlew.bat"),
        "runTests",
        "-PrunTests=true",
        f"-PtestClass={class_name}",
        "--no-daemon"
    ]
    env = os.environ.copy()
    env["JAVA_TOOL_OPTIONS"] = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
    try:
        result = subprocess.run(
            cmd, cwd=BASE_DIR, capture_output=True, text=True,
            encoding="utf-8", timeout=120, env=env
        )
        output = result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        return {"total": 0, "successful": 0, "failed": 0, "aborted": 0, "skipped": 0, "error": "timeout"}
    except Exception as e:
        return {"total": 0, "successful": 0, "failed": 0, "aborted": 0, "skipped": 0, "error": str(e)}

    stats = {"total": 0, "successful": 0, "failed": 0, "aborted": 0, "skipped": 0}
    key_map = {"total": "found", "successful": "successful", "failed": "failed", "aborted": "aborted", "skipped": "skipped"}
    for key, output_key in key_map.items():
        pattern = rf"\[\s*(\d+)\s+tests?\s+{output_key}\s*\]"
        match = re.search(pattern, output)
        if match:
            stats[key] = int(match.group(1))

    test_details = []
    for match in re.finditer(r"\+\-\-\s+(.+?)\s*→\s*.+?\n.*?status:\s*\[(.*?)\]", output, re.DOTALL):
        test_name = match.group(1).strip()
        status = match.group(2).strip()
        test_details.append({"name": test_name, "status": status})
    if not test_details:
        for match in re.finditer(r"\+\-\-\s+(\S+)\(\)\s*\n.*?status:\s*\[(.*?)\]", output, re.DOTALL):
            test_name = match.group(1).strip()
            status = match.group(2).strip()
            test_details.append({"name": test_name, "status": status})

    if not test_details:
        for match in re.finditer(r"(\S+)\(\)\s*»?\s*(.*)", output):
            test_details.append({"name": match.group(1), "status": "FAILED" if "FAILED" or "Exception" in match.group(2) else "SUCCESSFUL"})

    stats["details"] = test_details
    stats["raw_output"] = output[:2000]
    return stats

def find_report_path(rel_src_path, test_file_name):
    dir_part = os.path.dirname(rel_src_path)
    report_dir = os.path.join(REPORT_BASE_DIR, dir_part) if dir_part else REPORT_BASE_DIR
    report_file = os.path.join(report_dir, test_file_name + "报告.md")
    return report_file

def update_report(report_path, class_name, stats):
    if not os.path.exists(report_path):
        return False

    with open(report_path, "r", encoding="utf-8") as f:
        content = f.read()

    total = stats["total"]
    successful = stats["successful"]
    failed = stats["failed"]
    aborted = stats["aborted"]
    skipped = stats["skipped"]
    pass_rate = f"{(successful/total*100):.1f}%" if total > 0 else "0%"

    content = re.sub(r"\| 总测试数\s*\|\s*\d+\s*\|", f"| 总测试数 | {total} |", content)
    content = re.sub(r"\| 通过数\s*\|\s*\d+\s*\|", f"| 通过数 | {successful} |", content)
    content = re.sub(r"\| 失败数\s*\|\s*\d+\s*\|", f"| 失败数 | {failed} |", content)
    content = re.sub(r"\| 跳过数\s*\|\s*\d+\s*\|", f"| 跳过数 | {skipped} |", content)
    content = re.sub(r"\| 通过率\s*\|\s*[\d.]+%\s*\|", f"| 通过率 | {pass_rate} |", content)

    if total > 0:
        overall = "✅ 通过" if failed == 0 and aborted == 0 else "❌ 失败"
    else:
        overall = "⚠️ 无测试用例"

    content = re.sub(r"\| 总体结果\s*\|\s*.*?\s*\|", f"| 总体结果 | {overall} |", content)

    details = stats.get("details", [])
    if details:
        detail_lines = []
        for d in details:
            status_icon = "✅" if "SUCCESS" in d["status"] else "❌" if "FAIL" in d["status"] else "⚠️"
            detail_lines.append(f"| {d['name']} | {status_icon} {d['status']} |")
        detail_block = "\n".join(detail_lines)

        test_detail_pattern = r"### 测试详情\s*\n\s*\n\| 测试名称 \| 结果 \|.*?(?=###|$)"
        replacement = f"### 测试详情\n\n| 测试名称 | 结果 |\n|---|---|\n{detail_block}\n\n"
        content = re.sub(test_detail_pattern, replacement, content, flags=re.DOTALL)

    with open(report_path, "w", encoding="utf-8") as f:
        f.write(content)
    return True

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "run"
    if mode == "discover":
        classes = discover_test_classes()
        print(json.dumps([{"class": c[0], "relPath": c[1], "testFile": c[2]} for c in classes], ensure_ascii=False, indent=2))
        print(f"\n共发现 {len(classes)} 个测试类")
        return

    classes = discover_test_classes()
    print(f"共发现 {len(classes)} 个测试类，开始逐个运行...")

    results = {"total_classes": len(classes), "success": 0, "failed": 0, "no_tests": 0, "errors": 0}
    all_results = []

    for i, (class_name, rel_path, test_file) in enumerate(classes):
        print(f"[{i+1}/{len(classes)}] 运行: {class_name}")
        stats = run_test_class(class_name)
        stats["class_name"] = class_name

        report_path = find_report_path(rel_path, test_file)
        updated = update_report(report_path, class_name, stats)

        if stats.get("error"):
            results["errors"] += 1
            print(f"  ❌ 错误: {stats['error']}")
        elif stats["total"] == 0:
            results["no_tests"] += 1
            print(f"  ⚠️ 无测试用例")
        elif stats["failed"] > 0 or stats["aborted"] > 0:
            results["failed"] += 1
            print(f"  ❌ 失败: {stats['failed']} 失败, {stats['successful']} 通过")
        else:
            results["success"] += 1
            print(f"  ✅ 通过: {stats['successful']}/{stats['total']}")

        all_results.append(stats)

    print(f"\n===== 测试运行完毕 =====")
    print(f"总测试类: {results['total_classes']}")
    print(f"全部通过: {results['success']}")
    print(f"有失败: {results['failed']}")
    print(f"无测试用例: {results['no_tests']}")
    print(f"运行错误: {results['errors']}")

    with open(os.path.join(BASE_DIR, "test_results.json"), "w", encoding="utf-8") as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2)
    print(f"详细结果已保存到 test_results.json")

if __name__ == "__main__":
    main()
