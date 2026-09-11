import argparse
import hashlib
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

MANIFEST_FILE = ".sync-manifest.json"
TRANSLATION_NEEDS_FILE = ".translation-needs.json"
SRC_PREFIX = "src/main/java/"
TEST_PREFIX = "src/test/java/"
REPORT_BASE = "开发需求文档/测试报告/"


def get_translation_prefix(base_dir):
    candidates = [
        os.path.join(base_dir, "src/main/resources/文本消息/"),
        os.path.join(base_dir, "src/main/resources/暮澜纪元/文本/"),
    ]
    for candidate in candidates:
        if os.path.isdir(candidate):
            return os.path.relpath(candidate, base_dir).replace(os.sep, "/")
    return "src/main/resources/文本消息/"


def get_report_prefix(base_dir):
    plugin_name = os.path.basename(os.path.abspath(base_dir))
    return REPORT_BASE + plugin_name + "/"


def compute_sha256(file_path):
    h = hashlib.sha256()
    try:
        with open(file_path, "rb") as f:
            while True:
                chunk = f.read(8192)
                if not chunk:
                    break
                h.update(chunk)
        return h.hexdigest()
    except FileNotFoundError:
        return None


def source_to_package_path(source_rel):
    if not source_rel.startswith(SRC_PREFIX):
        return None
    after_prefix = source_rel[len(SRC_PREFIX):]
    parts = after_prefix.split("/")
    if len(parts) < 2:
        return None
    class_file = parts[-1]
    if not class_file.endswith(".java"):
        return None
    class_name = class_file[:-5]
    root_package = parts[0]
    sub_package = parts[1:-1]
    return root_package, sub_package, class_name


def source_to_translation_dir(base_dir, source_rel):
    result = source_to_package_path(source_rel)
    if result is None:
        return None
    root_package, sub_package, class_name = result
    translation_prefix = get_translation_prefix(base_dir)
    path_parts = [base_dir, translation_prefix] + sub_package + [class_name]
    return os.path.join(*path_parts)


def source_to_test_path(source_rel):
    result = source_to_package_path(source_rel)
    if result is None:
        return None
    root_package, sub_package, class_name = result
    test_file = class_name + "测试.java"
    all_dirs = [root_package] + sub_package
    return TEST_PREFIX + "/".join(all_dirs) + "/" + test_file


def source_to_report_path(source_rel, base_dir):
    result = source_to_package_path(source_rel)
    if result is None:
        return None
    root_package, sub_package, class_name = result
    report_file = class_name + "测试报告.md"
    report_prefix = get_report_prefix(base_dir)
    if sub_package:
        return report_prefix + "/".join(sub_package) + "/" + report_file
    return report_prefix + report_file


def scan_translation_hashes(base_dir, source_rel):
    trans_dir = source_to_translation_dir(base_dir, source_rel)
    if trans_dir is None:
        return False, {}
    if not os.path.isdir(trans_dir):
        return False, {}
    hashes = {}
    for entry in os.listdir(trans_dir):
        if entry.endswith(".yml") or entry.endswith(".yaml"):
            lang = os.path.splitext(entry)[0]
            lang_file = os.path.join(trans_dir, entry)
            h = compute_sha256(lang_file)
            if h is not None:
                hashes[lang] = h
    if not hashes:
        return False, {}
    return True, hashes


def compute_sync_status(entry):
    if entry.get("translationNotNeeded") == "unknown":
        return "translation_unknown"
    if not entry["testExists"]:
        return "missing_test"
    if not entry["reportExists"]:
        return "missing_report"
    if not entry["translationExists"] and not entry["translationNotNeeded"]:
        return "missing_translation"
    if entry["translationExists"] and entry["translationHashes"]:
        return "synced"
    return "synced"


def compute_sync_status_with_old(entry, old_entry):
    status = compute_sync_status(entry)
    if status != "synced":
        return status
    if old_entry is None:
        return "synced"
    if entry["sourceHash"] != old_entry.get("sourceHash"):
        return "hash_mismatch"
    if entry["translationHashes"] != old_entry.get("translationHashes", {}):
        return "hash_mismatch"
    return "synced"


def build_entry(base_dir, source_rel, old_manifest=None, translation_config=None):
    source_abs = os.path.join(base_dir, source_rel)
    source_hash = compute_sha256(source_abs)
    if source_hash is None:
        return None

    translation_exists, translation_hashes = scan_translation_hashes(base_dir, source_rel)

    if translation_config is None:
        translation_config = load_translation_needs(base_dir)
    if source_rel in translation_config:
        translation_not_needed = not translation_config[source_rel]
    else:
        translation_not_needed = "unknown"

    test_rel = source_to_test_path(source_rel)
    test_abs = os.path.join(base_dir, test_rel) if test_rel else None
    test_exists = os.path.isfile(test_abs) if test_abs else False

    report_rel = source_to_report_path(source_rel, base_dir)
    report_abs = os.path.join(base_dir, report_rel) if report_rel else None
    report_exists = os.path.isfile(report_abs) if report_abs else False

    entry = {
        "sourcePath": source_rel,
        "sourceHash": source_hash,
        "translationExists": translation_exists,
        "translationNotNeeded": translation_not_needed,
        "translationHashes": translation_hashes,
        "testPath": test_rel,
        "testExists": test_exists,
        "reportPath": report_rel,
        "reportExists": report_exists,
    }

    old_entry = old_manifest.get(source_rel) if old_manifest else None
    entry["syncStatus"] = compute_sync_status_with_old(entry, old_entry)

    return entry


def find_java_sources(base_dir):
    src_dir = os.path.join(base_dir, SRC_PREFIX)
    sources = []
    if not os.path.isdir(src_dir):
        return sources
    for root, dirs, files in os.walk(src_dir):
        for f in files:
            if f.endswith(".java"):
                abs_path = os.path.join(root, f)
                rel_path = os.path.relpath(abs_path, base_dir).replace(os.sep, "/")
                sources.append(rel_path)
    sources.sort()
    return sources


def load_manifest(base_dir):
    manifest_path = os.path.join(base_dir, MANIFEST_FILE)
    if not os.path.isfile(manifest_path):
        return {}
    try:
        with open(manifest_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {}


def save_manifest(base_dir, manifest):
    manifest_path = os.path.join(base_dir, MANIFEST_FILE)
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
        f.write("\n")


def load_translation_needs(base_dir):
    config_path = os.path.join(base_dir, TRANSLATION_NEEDS_FILE)
    if not os.path.isfile(config_path):
        return {}
    try:
        with open(config_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {}


def save_translation_needs(base_dir, config):
    config_path = os.path.join(base_dir, TRANSLATION_NEEDS_FILE)
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)
        f.write("\n")


def cmd_scan(base_dir):
    old_manifest = load_manifest(base_dir)
    translation_config = load_translation_needs(base_dir)
    sources = find_java_sources(base_dir)
    manifest = {}
    for source_rel in sources:
        entry = build_entry(base_dir, source_rel, old_manifest, translation_config)
        if entry is not None:
            manifest[source_rel] = entry
    save_manifest(base_dir, manifest)

    total = len(manifest)
    synced = sum(1 for e in manifest.values() if e["syncStatus"] == "synced")
    issues = total - synced
    print(f"扫描完成: 共 {total} 个源文件")
    print(f"  同步: {synced}")
    print(f"  不同步: {issues}")
    if issues > 0:
        status_counts = {}
        for e in manifest.values():
            s = e["syncStatus"]
            if s != "synced":
                status_counts[s] = status_counts.get(s, 0) + 1
        for s, c in sorted(status_counts.items()):
            print(f"    {s}: {c}")

    missing_report = sum(1 for e in manifest.values() if not e.get("reportExists", False))
    print(f"  缺失测试报告: {missing_report}")

    return True


def cmd_check(base_dir):
    old_manifest = load_manifest(base_dir)
    if not old_manifest:
        print("清单文件不存在或为空，请先运行 scan")
        return False

    translation_config = load_translation_needs(base_dir)
    sources = find_java_sources(base_dir)
    inconsistencies = []
    missing_from_manifest = []
    removed_sources = set(old_manifest.keys()) - set(sources)

    for source_rel in sources:
        if source_rel not in old_manifest:
            missing_from_manifest.append(source_rel)
            continue
        entry = build_entry(base_dir, source_rel, old_manifest, translation_config)
        if entry is None:
            continue
        old_entry = old_manifest[source_rel]
        diffs = []
        if entry["sourceHash"] != old_entry.get("sourceHash"):
            diffs.append("sourceHash")
        if entry["translationExists"] != old_entry.get("translationExists"):
            diffs.append("translationExists")
        if entry["translationNotNeeded"] != old_entry.get("translationNotNeeded"):
            diffs.append("translationNotNeeded")
        if entry["translationHashes"] != old_entry.get("translationHashes", {}):
            old_th = old_entry.get("translationHashes", {})
            new_th = entry["translationHashes"]
            changed_langs = []
            for lang in set(list(old_th.keys()) + list(new_th.keys())):
                if old_th.get(lang) != new_th.get(lang):
                    changed_langs.append(lang)
            diffs.append(f"translationHashes({','.join(changed_langs)})")
        if entry["testExists"] != old_entry.get("testExists"):
            diffs.append("testExists")
        if entry["reportExists"] != old_entry.get("reportExists"):
            diffs.append("reportExists")
        if diffs:
            inconsistencies.append((source_rel, diffs))

    total = len(old_manifest)
    print(f"检查完成: 清单中共 {total} 个条目")
    if missing_from_manifest:
        print(f"  新增源文件(未在清单中): {len(missing_from_manifest)}")
        for s in missing_from_manifest:
            print(f"    + {s}")
    if removed_sources:
        print(f"  已删除源文件(仍在清单中): {len(removed_sources)}")
        for s in sorted(removed_sources):
            print(f"    - {s}")
    if inconsistencies:
        print(f"  哈希/状态不一致: {len(inconsistencies)}")
        for source_rel, diffs in inconsistencies:
            print(f"    {source_rel}: {', '.join(diffs)}")
    if not missing_from_manifest and not removed_sources and not inconsistencies:
        print("  所有条目一致")

    has_issues = bool(missing_from_manifest or removed_sources or inconsistencies)
    return not has_issues


def cmd_update(base_dir, source_file):
    source_file = source_file.replace(os.sep, "/")
    old_manifest = load_manifest(base_dir)
    translation_config = load_translation_needs(base_dir)
    source_abs = os.path.join(base_dir, source_file)
    if not os.path.isfile(source_abs):
        print(f"源文件不存在: {source_file}")
        return False

    entry = build_entry(base_dir, source_file, old_manifest, translation_config)
    if entry is None:
        print(f"无法构建条目: {source_file}")
        return False

    old_manifest[source_file] = entry
    save_manifest(base_dir, old_manifest)
    print(f"已更新: {source_file}")
    print(f"  syncStatus: {entry['syncStatus']}")
    print(f"  testExists: {entry['testExists']}")
    print(f"  reportExists: {entry['reportExists']}")
    print(f"  translationExists: {entry['translationExists']}")
    return True


def generate_report_template(entry, base_dir):
    result = source_to_package_path(entry["sourcePath"])
    if result is None:
        return None
    root_package, sub_package, class_name = result
    package_parts = [root_package] + sub_package
    package_dot = ".".join(package_parts)
    full_test_class = package_dot + "." + class_name + "测试"
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    source_hash = entry.get("sourceHash", "")
    test_path = entry.get("testPath", "")
    test_hash = compute_sha256(os.path.join(base_dir, test_path)) if test_path else ""

    template = f"""# {class_name}测试报告

**生成时间**：{now}
**源文件哈希**：{source_hash}
**测试文件哈希**：{test_hash}

## 测试概要

|指标|数值|
|---|---|
|测试总数|(待fill-reports填入)|
|通过数|(待fill-reports填入)|
|失败数|(待fill-reports填入)|
|跳过数|(待fill-reports填入)|

## 测试用例列表

|测试方法|显示名|结果|
|---|---|---|
|(待fill-reports填入)|(待fill-reports填入)|(待fill-reports填入)|
"""
    return template


def cmd_generate_reports(base_dir, force):
    manifest = load_manifest(base_dir)
    if not manifest:
        print("清单文件不存在或为空，请先运行 scan")
        return False

    generated = 0
    skipped = 0
    overwritten = 0

    for source_rel, entry in manifest.items():
        if not entry.get("reportExists", False) or force:
            report_rel = entry.get("reportPath")
            if report_rel is None:
                skipped += 1
                continue

            report_abs = os.path.join(base_dir, report_rel)
            report_dir = os.path.dirname(report_abs)

            if os.path.isfile(report_abs) and not force:
                skipped += 1
                continue

            template = generate_report_template(entry, base_dir)
            if template is None:
                skipped += 1
                continue

            os.makedirs(report_dir, exist_ok=True)

            is_overwrite = os.path.isfile(report_abs)
            with open(report_abs, "w", encoding="utf-8") as f:
                f.write(template)

            if is_overwrite:
                overwritten += 1
            else:
                generated += 1

            entry["reportExists"] = True
            entry["syncStatus"] = compute_sync_status(entry)

    if generated > 0 or overwritten > 0:
        save_manifest(base_dir, manifest)

    print(f"报告生成完成:")
    print(f"  新生成: {generated}")
    print(f"  覆盖: {overwritten}")
    print(f"  跳过: {skipped}")
    return True


JUNIT_XML_DIR = "build/test-results/test/"


def find_junit_xml(base_dir):
    xml_dir = os.path.join(base_dir, JUNIT_XML_DIR)
    xml_files = {}
    if not os.path.isdir(xml_dir):
        return xml_files
    for f in os.listdir(xml_dir):
        if f.startswith("TEST-") and f.endswith(".xml"):
            xml_path = os.path.join(xml_dir, f)
            xml_files[f] = xml_path
    return xml_files


def parse_junit_xml(xml_path):
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except (ET.ParseError, OSError):
        return None

    testsuite = root if root.tag == "testsuite" else root.find("testsuite")
    if testsuite is None:
        return None

    total = int(testsuite.get("tests", "0"))
    failures = int(testsuite.get("failures", "0"))
    skipped = int(testsuite.get("skipped", "0"))
    passed = total - failures - skipped

    testcases = []
    for tc in testsuite.findall("testcase"):
        method_name = tc.get("name", "")
        is_failure = tc.find("failure") is not None
        is_skipped = tc.find("skipped") is not None
        if is_failure:
            result = "❌"
        elif is_skipped:
            result = "⏭️"
        else:
            result = "✅"
        chat_output = ""
        raw_output = ""
        props = tc.find("properties")
        if props is not None:
            for prop in props.findall("property"):
                if prop.get("name") == "玩家聊天栏实际输出":
                    chat_output = prop.get("value", "")
                elif prop.get("name") == "原始MiniMessage格式":
                    raw_output = prop.get("value", "")
        testcases.append({"method": method_name, "result": result, "chat_output": chat_output, "raw_output": raw_output})

    return {
        "total": total,
        "passed": passed,
        "failed": failures,
        "skipped": skipped,
        "testcases": testcases,
    }


def extract_display_names(test_source_path):
    display_names = {}
    if not os.path.isfile(test_source_path):
        return display_names
    try:
        with open(test_source_path, "r", encoding="utf-8") as f:
            content = f.read()
    except OSError:
        return display_names

    pattern = r'@DisplayName\s*\(\s*"([^"]+)"\s*\)'
    matches = re.finditer(pattern, content)

    method_pattern = r'(?:@Test|@ParameterizedTest|@RepeatedTest|@TestFactory|@DisplayName\s*\(\s*"[^"]+"\s*\))\s*(?:public\s+)?(?:void|Stream|Collection)\s+(\w+)\s*\('

    last_display_name = None
    for line in content.split('\n'):
        dm = re.search(r'@DisplayName\s*\(\s*"([^"]+)"\s*\)', line)
        if dm:
            last_display_name = dm.group(1)
            continue
        mm = re.search(r'(?:public\s+)?(?:void|Stream|Collection)\s+(\w+)\s*\(', line)
        if mm and last_display_name:
            method_name = mm.group(1)
            display_names[method_name] = last_display_name
            last_display_name = None

    return display_names


def xml_filename_to_test_class(xml_filename):
    name = xml_filename
    if name.startswith("TEST-"):
        name = name[5:]
    if name.endswith(".xml"):
        name = name[:-4]
    dollar_pos = name.find("$")
    if dollar_pos != -1:
        name = name[:dollar_pos]
    return name


def cmd_fill_reports(base_dir):
    manifest = load_manifest(base_dir)
    if not manifest:
        print("清单文件不存在或为空，请先运行 scan")
        return False

    xml_files = find_junit_xml(base_dir)
    if not xml_files:
        print(f"未找到JUnit XML报告，请先运行测试（目录：{JUNIT_XML_DIR}）")
        return False

    print(f"找到 {len(xml_files)} 个JUnit XML报告文件")

    test_class_to_manifest = {}
    for source_rel, entry in manifest.items():
        result = source_to_package_path(source_rel)
        if result is None:
            continue
        root_package, sub_package, class_name = result
        test_class_name = class_name + "测试"
        package_parts = [root_package] + sub_package
        full_test_class = ".".join(package_parts) + "." + test_class_name
        test_class_to_manifest[full_test_class] = entry

    grouped_xml = {}
    for xml_filename, xml_path in xml_files.items():
        parent_class = xml_filename_to_test_class(xml_filename)
        if parent_class not in grouped_xml:
            grouped_xml[parent_class] = []
        grouped_xml[parent_class].append(xml_path)

    filled = 0
    skipped = 0

    for full_test_class, xml_paths in grouped_xml.items():
        entry = test_class_to_manifest.get(full_test_class)

        if entry is None:
            skipped += len(xml_paths)
            continue

        report_rel = entry.get("reportPath")
        if not report_rel:
            skipped += len(xml_paths)
            continue

        report_abs = os.path.join(base_dir, report_rel)
        if not os.path.isfile(report_abs):
            skipped += len(xml_paths)
            continue

        combined_total = 0
        combined_passed = 0
        combined_failed = 0
        combined_skipped_count = 0
        combined_testcases = []

        for xml_path in xml_paths:
            test_result = parse_junit_xml(xml_path)
            if test_result is None:
                continue
            combined_total += test_result["total"]
            combined_passed += test_result["passed"]
            combined_failed += test_result["failed"]
            combined_skipped_count += test_result["skipped"]
            combined_testcases.extend(test_result["testcases"])

        if combined_total == 0:
            skipped += len(xml_paths)
            continue

        test_path = entry.get("testPath", "")
        test_source_abs = os.path.join(base_dir, test_path) if test_path else ""
        display_names = extract_display_names(test_source_abs)

        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        source_hash = entry.get("sourceHash", "")
        test_hash = compute_sha256(test_source_abs) if test_source_abs and os.path.isfile(test_source_abs) else ""

        result = source_to_package_path(entry["sourcePath"])
        if result is None:
            skipped += len(xml_paths)
            continue
        _, _, class_name = result

        summary_rows = (
            f"|测试总数|{combined_total}|\n"
            f"|通过数|{combined_passed}|\n"
            f"|失败数|{combined_failed}|\n"
            f"|跳过数|{combined_skipped_count}|"
        )

        case_rows = []
        chat_rows = []
        for tc in combined_testcases:
            display_name = display_names.get(tc["method"], tc["method"])
            case_rows.append(f"|{tc['method']}|{display_name}|{tc['result']}|")
            if tc.get("chat_output"):
                raw = tc.get("raw_output", "")
                raw_cell = raw if raw else tc["chat_output"]
                chat_rows.append(f"|{tc['method']}|{display_name}|{tc['chat_output']}|{raw_cell}|")

        cases_table = "\n".join(case_rows) if case_rows else "|(无测试用例)||(无)|"
        chat_table = "\n".join(chat_rows) if chat_rows else "|(无玩家聊天栏输出记录)||(无)||(无)||(无)|"

        report_content = f"""# {class_name}测试报告

**生成时间**：{now}
**源文件哈希**：{source_hash}
**测试文件哈希**：{test_hash}

## 测试概要

|指标|数值|
|---|---|
{summary_rows}

## 测试用例列表

|测试方法|显示名|结果|
|---|---|---|
{cases_table}

## 玩家聊天栏实际输出

|测试方法|显示名|实际输出|原始MiniMessage格式|
|---|---|---|---|
{chat_table}
"""

        report_dir = os.path.dirname(report_abs)
        os.makedirs(report_dir, exist_ok=True)
        with open(report_abs, "w", encoding="utf-8") as f:
            f.write(report_content)

        filled += 1

    print(f"报告填入完成:")
    print(f"  已填入: {filled}")
    print(f"  跳过: {skipped}")
    return True


def cmd_init_translation_needs(base_dir):
    manifest = load_manifest(base_dir)
    if not manifest:
        print("清单文件不存在或为空，请先运行 scan")
        return False

    existing_config = load_translation_needs(base_dir)
    config = dict(existing_config)

    new_count = 0
    for source_rel, entry in manifest.items():
        if source_rel in config:
            continue
        if entry.get("translationExists"):
            config[source_rel] = True
        elif entry.get("translationNotNeeded") and entry["translationNotNeeded"] != "unknown":
            config[source_rel] = False
        new_count += 1

    save_translation_needs(base_dir, config)
    print(f"翻译需求配置已初始化:")
    print(f"  已有配置: {len(existing_config)}")
    print(f"  新增配置: {new_count}")
    print(f"  总计: {len(config)}")
    needs_count = sum(1 for v in config.values() if v)
    not_needs_count = sum(1 for v in config.values() if not v)
    print(f"  需要翻译: {needs_count}")
    print(f"  不需要翻译: {not_needs_count}")
    return True


def cmd_confirm_translation(base_dir, source_file, needs_translation):
    source_file = source_file.replace(os.sep, "/")
    config = load_translation_needs(base_dir)
    config[source_file] = needs_translation
    save_translation_needs(base_dir, config)

    old_manifest = load_manifest(base_dir)
    translation_config = load_translation_needs(base_dir)
    entry = build_entry(base_dir, source_file, old_manifest, translation_config)
    if entry is not None:
        old_manifest[source_file] = entry
        save_manifest(base_dir, old_manifest)

    status = "需要翻译" if needs_translation else "不需要翻译"
    print(f"已确认: {source_file} → {status}")
    print(f"  syncStatus: {entry['syncStatus'] if entry else 'N/A'}")
    return True


def main():
    parser = argparse.ArgumentParser(description="XRM内容哈希清单工具")
    parser.add_argument("--base-dir", default=None, help="XRM插件根目录")
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser("scan", help="扫描所有源文件，生成新清单")

    subparsers.add_parser("check", help="检查当前清单是否与文件一致")

    update_parser = subparsers.add_parser("update", help="更新指定源文件的清单条目")
    update_parser.add_argument("--source-file", required=True, help="源文件相对路径")

    report_parser = subparsers.add_parser("generate-reports", help="为缺失的测试报告生成模板")
    report_parser.add_argument("--force", action="store_true", help="覆盖已存在的报告文件")

    subparsers.add_parser("fill-reports", help="从JUnit XML报告填入测试结果到测试报告")

    subparsers.add_parser("init-translation-needs", help="从当前清单初始化翻译需求配置")

    confirm_parser = subparsers.add_parser("confirm-translation", help="确认源文件是否需要翻译")
    confirm_parser.add_argument("--source-file", required=True, help="源文件相对路径")
    confirm_parser.add_argument("--needs", required=True, choices=["true", "false"], help="是否需要翻译(true/false)")

    args = parser.parse_args()

    if args.command is None:
        parser.print_help()
        sys.exit(1)

    base_dir = args.base_dir or os.getcwd()

    if args.command == "scan":
        ok = cmd_scan(base_dir)
    elif args.command == "check":
        ok = cmd_check(base_dir)
    elif args.command == "update":
        ok = cmd_update(base_dir, args.source_file)
    elif args.command == "generate-reports":
        ok = cmd_generate_reports(base_dir, args.force)
    elif args.command == "fill-reports":
        ok = cmd_fill_reports(base_dir)
    elif args.command == "init-translation-needs":
        ok = cmd_init_translation_needs(base_dir)
    elif args.command == "confirm-translation":
        needs_translation = args.needs == "true"
        ok = cmd_confirm_translation(base_dir, args.source_file, needs_translation)
    else:
        parser.print_help()
        sys.exit(1)

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
