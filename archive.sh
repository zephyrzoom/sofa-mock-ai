#!/usr/bin/env bash
#
# archive.sh — 生成项目代码压缩包 (zip)
#
# 收集 git 已跟踪 + 未跟踪但未被 .gitignore 忽略的文件，
# 排除构建产物(target/)、本地配置(.idea/)、运行时数据(data/)、压缩包等，
# 打包为一个 zip，压缩包内带顶层目录，解压后不会散落文件。
#
# 使用 Python zipfile 生成（而非 macOS 自带 zip 命令），原因：
#   - macOS 自带 Info-ZIP 3.0 不设置文件名 UTF-8 标志位，
#     中文文件名(如 doc/使用手册.md)在 Windows 下会按本地代码页解码成乱码；
#   - 它还会写入 UT(0x5455)/Unix(0x7875) 扩展字段，
#     老版本 7-Zip 解析时可能报"文件末端错误"。
#   Python zipfile 生成的包结构标准、不写这些扩展字段，
#   Windows 7-Zip / 资源管理器 / WinRAR 均可正常解压。
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# 压缩包内的顶层目录名（也是解压后的文件夹名）
NAME="sofa-mock-ai"
OUT="$ROOT/${NAME}.zip"

# 脚本自身和输出包，避免把自己打进去
SCRIPT_NAME="$(basename "$0")"
OUT_NAME="$(basename "$OUT")"

rm -f "$OUT"

COUNT="$(python3 - "$NAME" "$OUT" "$SCRIPT_NAME" "$OUT_NAME" <<'PY'
import os, subprocess, sys, time, zipfile

name, out, script_name, out_name = sys.argv[1:5]

# 收集文件：已跟踪(cached) + 未跟踪(others)，并排除被 .gitignore 忽略的
raw = subprocess.check_output(["git", "ls-files", "-co", "--exclude-standard", "-z"])
files = [f.decode("utf-8") for f in raw.split(b"\0") if f]
files = [
    f for f in files
    if os.path.isfile(f) and f != script_name and f != out_name
]
if not files:
    print("error: 没有可打包的文件", file=sys.stderr)
    sys.exit(1)

# 所有子目录（用于生成目录项，Windows 解压时直接展开成文件夹）
dirs = set()
for f in files:
    d = os.path.dirname(f)
    while d:
        dirs.add(d)
        d = os.path.dirname(d)

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    # 顶层目录
    st = os.stat(".")
    zi = zipfile.ZipInfo(f"{name}/")
    zi.date_time = time.localtime(st.st_mtime)[:6]
    zi.compress_type = zipfile.ZIP_DEFLATED
    zi.external_attr = (st.st_mode & 0xFFFF) << 16  # 保留目录/unix 权限位
    z.writestr(zi, b"")
    # 子目录
    for d in sorted(dirs):
        st = os.stat(d)
        zi = zipfile.ZipInfo(f"{name}/{d}/")
        zi.date_time = time.localtime(st.st_mtime)[:6]
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = (st.st_mode & 0xFFFF) << 16
        z.writestr(zi, b"")
    # 文件
    for f in files:
        st = os.stat(f)
        zi = zipfile.ZipInfo(f"{name}/{f}")
        zi.date_time = time.localtime(st.st_mtime)[:6]
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = (st.st_mode & 0xFFFF) << 16  # 保留可执行位等 unix 权限
        with open(f, "rb") as fh:
            z.writestr(zi, fh.read())

print(len(files))
PY
)"

echo "已生成: $OUT ($(du -h "$OUT" | cut -f1), $COUNT 个文件)"
