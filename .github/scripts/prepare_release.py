"""Build the exact semantic-release version and stage its single Android bundle."""
from pathlib import Path
import json
import os
import re
import shutil
import subprocess
import sys
from zipfile import ZipFile


def prepare(version: str, root: Path) -> None:
    if not re.fullmatch(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z.-]+)?", version):
        raise ValueError("Invalid semantic release version")
    repository = os.environ["GITHUB_REPOSITORY"]
    if not re.fullmatch(r"[\w.-]+/[\w.-]+", repository):
        raise ValueError("Invalid GitHub repository")
    branch = os.environ["GITHUB_REF_NAME"]
    destination = root / "patches/build/release/vibe-patch.mpp"
    # A failed retry must never leave a previously staged release asset.
    destination.unlink(missing_ok=True)
    properties = root / "gradle.properties"
    content, count = re.subn(r"(?m)^version\s*=\s*[^\r\n]+", f"version = {version}", properties.read_text())
    if count != 1:
        raise ValueError("Expected exactly one Gradle version property")
    properties.write_text(content, encoding="utf-8")

    subprocess.run(["bash", "compile.sh", "--no-daemon", ":patches:generatePatchesList",
                    ":patches:checkStringResources"], cwd=root, check=True)
    subprocess.run([sys.executable, ".github/scripts/generate_patches_readme.py", repository, branch],
                   cwd=root, check=True)
    subprocess.run([sys.executable, "scripts/validate-migration.py"], cwd=root, check=True)
    bundle = root / f"patches/build/libs/vibe-patch-{version}.mpp"
    with ZipFile(bundle) as archive:
        manifest = archive.read("META-INF/MANIFEST.MF").decode()
        fields = dict(line.split(": ", 1) for line in manifest.splitlines() if ": " in line)
        if fields.get("Version") != version or fields.get("Name") != "Vibe Patch":
            raise ValueError("Bundle manifest must match the Vibe Patch release version")
        if "classes.dex" not in archive.namelist():
            raise ValueError("Release bundle must include Android DEX")
    metadata = json.loads((root / "patches-bundle.json").read_text())
    expected_url = f"https://github.com/{repository}/releases/download/v{version}/vibe-patch-{version}.mpp"
    if metadata["version"] != version or metadata["download_url"] != expected_url:
        raise ValueError("Release metadata must match the version and point to the new asset")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bundle, destination)


if __name__ == "__main__":
    prepare(sys.argv[1], Path(__file__).resolve().parents[2])
