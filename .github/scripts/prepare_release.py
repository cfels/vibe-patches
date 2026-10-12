"""Regenerate release metadata and optionally build the matching Android bundle."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess
import sys
from zipfile import ZipFile

GENERATED_FILES = ("gradle.properties", "patches-list.json", "patches-bundle.json", "README.md")
VERSION_PROPERTY = re.compile(r"(?m)^version\s*=\s*([^\r\n]+)")


def read_version(root: Path) -> str:
    matches = VERSION_PROPERTY.findall((root / "gradle.properties").read_text(encoding="utf-8"))
    if len(matches) != 1 or not re.fullmatch(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)", matches[0].strip()):
        raise ValueError("Expected exactly one major.minor.patch version in gradle.properties")
    return matches[0].strip()


def bump_version(root: Path, part: str) -> str:
    position = ("major", "minor", "patch").index(part)
    numbers = list(map(int, read_version(root).split(".")))
    numbers[position] += 1
    numbers[position + 1:] = [0] * (2 - position)
    version = ".".join(map(str, numbers))
    properties = root / "gradle.properties"
    properties.write_text(VERSION_PROPERTY.sub(f"version = {version}", properties.read_text(encoding="utf-8")),
                          encoding="utf-8")
    return version


def regenerate(root: Path, build_bundle: bool = False) -> str:
    version = read_version(root)
    build = (root / "patches/build.gradle.kts").read_text(encoding="utf-8")
    source = re.search(r'source = "https://github.com/([\w.-]+/[\w.-]+)"', build)
    description = re.search(r'description = "([^"]+)"', build)
    if source is None or description is None:
        raise ValueError("Expected GitHub source and description in patches.about")
    repository = source[1]
    bundle = root / f"patches/build/libs/vibe-patch-{version}.mpp"
    if build_bundle:
        bundle.unlink(missing_ok=True)
        command = ["bash", "compile.sh", "--no-daemon", ":patches:generatePatchesList",
                   ":patches:checkStringResources"]
    else:
        command = ["bash", "gradlew", "--no-daemon", ":patches:generatePatchesList"]
    subprocess.run(command, cwd=root, check=True)
    if build_bundle:
        with ZipFile(bundle) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode()
            fields = dict(line.split(": ", 1) for line in manifest.splitlines() if ": " in line)
            if fields.get("Version") != version or fields.get("Name") != "Vibe Patch":
                raise ValueError("Bundle manifest must match the Vibe Patch release version")
            if "classes.dex" not in archive.namelist():
                raise ValueError("Release bundle must include Android DEX")
    index = json.loads((root / "patches-list.json").read_text(encoding="utf-8"))
    if index["version"] != version:
        raise ValueError("Generated patch index does not match gradle.properties")
    metadata_path = root / "patches-bundle.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    if metadata.get("version") != version:
        metadata["created_at"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")
    metadata.update(
        version=version,
        description=f"## {version}\n\n{description[1]}",
        download_url=f"https://github.com/{repository}/releases/download/v{version}/vibe-patch-{version}.mpp",
        signature_download_url="",
    )
    metadata_path.write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    subprocess.run([sys.executable, ".github/scripts/generate_patches_readme.py", repository, "main"],
                   cwd=root, check=True)
    subprocess.run([sys.executable, "scripts/validate-migration.py"], cwd=root, check=True)
    return version


def check_generated(root: Path) -> None:
    subprocess.run(["git", "diff", "--exit-code", "--", *GENERATED_FILES], cwd=root, check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--bump", choices=("patch", "minor", "major"))
    modes.add_argument("--check", action="store_true", help="Fail if regeneration changes tracked metadata")
    parser.add_argument("--build", action="store_true", help="Build, test and validate the versioned Android bundle")
    arguments = parser.parse_args()
    if arguments.check and arguments.build:
        parser.error("--check cannot be combined with --build")
    root = Path(__file__).resolve().parents[2]
    if arguments.bump:
        bump_version(root, arguments.bump)
    version = regenerate(root, build_bundle=arguments.build)
    if arguments.check:
        check_generated(root)
    print(f"Vibe Patch {version}: metadata regenerated" + (", Android bundle built and validated" if arguments.build else ""))
