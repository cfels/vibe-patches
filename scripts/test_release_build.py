"""Release metadata and bundle checks without APK builds, network access or publication."""
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import parse_qs, urlparse
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("prepare_release", ROOT / ".github/scripts/prepare_release.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for name in release.GENERATED_FILES:
            shutil.copyfile(ROOT / name, self.root / name)
        (self.root / "patches").mkdir()
        shutil.copyfile(ROOT / "patches/build.gradle.kts", self.root / "patches/build.gradle.kts")
        (self.root / ".github/scripts").mkdir(parents=True)
        shutil.copyfile(ROOT / ".github/scripts/generate_patches_readme.py",
                        self.root / ".github/scripts/generate_patches_readme.py")
        self.run = subprocess.run

    def generate(self, command, **kwargs):
        self.assertEqual(kwargs["cwd"], self.root)
        self.assertTrue(kwargs["check"])
        if command[:2] in (["bash", "gradlew"], ["bash", "compile.sh"]):
            if command[1] == "compile.sh":
                self.assertEqual(command, ["bash", "compile.sh", "--no-daemon", ":patches:generatePatchesList",
                                           ":patches:checkStringResources"])
                self.archive()
            else:
                self.assertEqual(command, ["bash", "gradlew", "--no-daemon", ":patches:generatePatchesList"])
            index_path = self.root / "patches-list.json"
            index = json.loads(index_path.read_text())
            index["version"] = release.read_version(self.root)
            index_path.write_text(json.dumps(index, indent=2) + "\n")
        elif command[-1] != "scripts/validate-migration.py":
            self.run(command, **kwargs, stdout=subprocess.DEVNULL)

    def archive(self, version=None, name="Vibe Patch", dex=True):
        current = release.read_version(self.root)
        bundle = self.root / f"patches/build/libs/vibe-patch-{current}.mpp"
        bundle.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(bundle, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", f"Name: {name}\r\nVersion: {version or current}\r\n")
            if dex:
                archive.writestr("classes.dex", b"test DEX fixture")
        return bundle

    def test_repository_metadata_resolves_the_versioned_bundle(self):
        version = release.read_version(ROOT)
        metadata = json.loads((ROOT / "patches-bundle.json").read_text())
        index = json.loads((ROOT / "patches-list.json").read_text())
        source = re.search(r'source = "(https://github.com/[^"]+)"',
                           (ROOT / "patches/build.gradle.kts").read_text())[1]
        repository = urlparse(source).path.strip("/")
        self.assertEqual(metadata["version"], version)
        self.assertEqual(index["version"], version)
        self.assertEqual(metadata["download_url"],
                         f"{source}/releases/download/v{version}/vibe-patch-{version}.mpp")
        readme = (ROOT / "README.md").read_text()
        self.assertIn(f"/releases/tag/v{version}", readme)
        link = re.search(r'https://morphe\.software/add-source\?[^)\s]+', readme)[0]
        query = parse_qs(urlparse(link).query)
        self.assertEqual(query["github"], [repository])
        self.assertEqual(query["name"], ["Vibe Patch"])

    def test_bumps_only_the_canonical_version(self):
        for part, expected in [("patch", "2.3.5"), ("minor", "2.4.0"), ("major", "3.0.0")]:
            with self.subTest(part=part):
                properties = self.root / "gradle.properties"
                properties.write_text("org.gradle.caching = true\nversion = 2.3.4\n")
                before = (self.root / "patches-bundle.json").read_bytes()
                self.assertEqual(release.bump_version(self.root, part), expected)
                self.assertEqual(release.read_version(self.root), expected)
                self.assertEqual((self.root / "patches-bundle.json").read_bytes(), before)
                self.assertIn("org.gradle.caching = true", properties.read_text())

    def test_rejects_ambiguous_or_invalid_version(self):
        for value in ["version = 1.0.0\nversion = 2.0.0\n", "version = 01.0.0\n", "", "version = nope\n"]:
            with self.subTest(value=value):
                (self.root / "gradle.properties").write_text(value)
                with self.assertRaises(ValueError):
                    release.bump_version(self.root, "patch")
                self.assertEqual((self.root / "gradle.properties").read_text(), value)

    def test_bump_regenerates_all_metadata_and_readme_idempotently(self):
        old_date = "2000-01-01T00:00:00"
        metadata_path = self.root / "patches-bundle.json"
        metadata = json.loads(metadata_path.read_text())
        metadata["created_at"] = old_date
        metadata_path.write_text(json.dumps(metadata))
        version = release.bump_version(self.root, "patch")
        with patch.object(release.subprocess, "run", side_effect=self.generate):
            self.assertEqual(release.regenerate(self.root), version)
            first = {name: (self.root / name).read_bytes() for name in release.GENERATED_FILES}
            release.regenerate(self.root)
        self.assertEqual(first, {name: (self.root / name).read_bytes() for name in release.GENERATED_FILES})
        metadata = json.loads((self.root / "patches-bundle.json").read_text())
        self.assertEqual(metadata["version"], version)
        self.assertNotEqual(metadata["created_at"], old_date)
        self.assertIn(f"/v{version}/vibe-patch-{version}.mpp", metadata["download_url"])
        self.assertEqual(json.loads((self.root / "patches-list.json").read_text())["version"], version)
        self.assertIn(f"/releases/tag/v{version}", (self.root / "README.md").read_text())

    def test_failed_generator_does_not_update_bundle_or_readme(self):
        before = {name: (self.root / name).read_bytes() for name in ["patches-bundle.json", "README.md"]}
        error = subprocess.CalledProcessError(1, ["bash", "gradlew"])
        with patch.object(release.subprocess, "run", side_effect=error), self.assertRaises(subprocess.CalledProcessError):
            release.regenerate(self.root)
        self.assertEqual(before, {name: (self.root / name).read_bytes() for name in before})

    def test_build_uses_bumped_version_and_keeps_one_bundle(self):
        version = release.bump_version(self.root, "patch")
        bundle = self.archive(version="0.0.0")

        def build(command, **kwargs):
            if command[:2] == ["bash", "compile.sh"]:
                self.assertEqual(release.read_version(self.root), version)
                self.assertFalse(bundle.exists())
            self.generate(command, **kwargs)

        with patch.object(release.subprocess, "run", side_effect=build):
            self.assertEqual(release.regenerate(self.root, build_bundle=True), version)
        self.assertEqual(list(bundle.parent.glob("*.mpp")), [bundle])
        metadata = json.loads((self.root / "patches-bundle.json").read_text())
        self.assertEqual(metadata["version"], version)
        self.assertTrue(metadata["download_url"].endswith(f"/v{version}/{bundle.name}"))

    def test_build_preserves_prepared_metadata_and_version(self):
        version = release.bump_version(self.root, "patch")
        with patch.object(release.subprocess, "run", side_effect=self.generate):
            release.regenerate(self.root)
            prepared = {name: (self.root / name).read_bytes() for name in release.GENERATED_FILES}
            self.assertEqual(release.regenerate(self.root, build_bundle=True), version)
        self.assertEqual(prepared, {name: (self.root / name).read_bytes() for name in release.GENERATED_FILES})
        self.assertTrue((self.root / f"patches/build/libs/vibe-patch-{version}.mpp").is_file())

    def test_workflow_pushes_metadata_before_build_and_publication(self):
        workflow = (ROOT / ".github/workflows/release.yml").read_text()
        steps = re.split(r"(?m)^      - name: ", workflow)[1:]
        prepare = next(i for i, step in enumerate(steps) if "prepare_release.py --bump" in step)
        commit = next(i for i, step in enumerate(steps) if "git commit " in step)
        build = next(i for i, step in enumerate(steps) if "prepare_release.py --build" in step)
        upload = next(i for i, step in enumerate(steps) if "actions/upload-artifact@" in step)
        publish = next(i for i, step in enumerate(steps) if "gh release create " in step)
        self.assertLess(prepare, commit)
        self.assertLess(commit, build)
        self.assertLess(build, upload)
        self.assertLess(upload, publish)
        self.assertNotIn("--build", steps[prepare])
        self.assertNotIn("--bump", steps[build])
        self.assertIn("git push origin HEAD:main", steps[commit])
        self.assertLess(steps[commit].index("git commit "), steps[commit].index("git push "))
        self.assertIn("git diff --exit-code -- " + " ".join(release.GENERATED_FILES), steps[build])
        self.assertIn("COMMIT: ${{ steps.prepared.outputs.commit }}", steps[publish])
        self.assertIn('--target "$COMMIT"', steps[publish])
        self.assertNotIn("continue-on-error:", workflow)
        self.assertNotIn("if: always()", workflow)

    def test_compile_script_runs_release_tasks_and_reports_exact_bundle(self):
        shutil.copyfile(ROOT / "compile.sh", self.root / "compile.sh")
        bundle = self.archive()
        bundle.rename(self.root / "build-fixture.mpp")
        (self.root / "gradlew").write_text(
            'printf "%s\\n" "$@" > gradle-arguments.txt\n'
            f'cp build-fixture.mpp "patches/build/libs/{bundle.name}"\n')
        result = self.run(["bash", "compile.sh", "--no-daemon", ":patches:generatePatchesList",
                           ":patches:checkStringResources"], cwd=self.root, check=True, capture_output=True, text=True,
                          env={key: value for key, value in os.environ.items() if key != "YANOSIK_APK"})
        arguments = (self.root / "gradle-arguments.txt").read_text().splitlines()
        self.assertEqual(arguments[3:], [":patches:buildAndroid", ":patches:test", "--no-daemon",
                                        ":patches:generatePatchesList", ":patches:checkStringResources"])
        self.assertIn(str(bundle), result.stdout)
        self.assertEqual(bundle.read_bytes(), (self.root / "build-fixture.mpp").read_bytes())

    def test_rejects_wrong_bundle_identity_or_missing_dex(self):
        for fields in [{"version": "0.0.0"}, {"name": "Other patches"}, {"dex": False}]:
            with self.subTest(fields=fields):
                before = {name: (self.root / name).read_bytes() for name in ["patches-bundle.json", "README.md"]}

                def build(command, **kwargs):
                    self.generate(command, **kwargs)
                    if command[:2] == ["bash", "compile.sh"]:
                        self.archive(**fields)

                with patch.object(release.subprocess, "run", side_effect=build), self.assertRaises(ValueError):
                    release.regenerate(self.root, build_bundle=True)
                self.assertEqual(before, {name: (self.root / name).read_bytes() for name in before})

    def test_failed_build_removes_stale_bundle_and_preserves_metadata(self):
        bundle = self.archive()
        before = {name: (self.root / name).read_bytes() for name in ["patches-bundle.json", "README.md"]}
        error = subprocess.CalledProcessError(1, ["bash", "compile.sh"])
        with patch.object(release.subprocess, "run", side_effect=error), self.assertRaises(subprocess.CalledProcessError):
            release.regenerate(self.root, build_bundle=True)
        self.assertFalse(bundle.exists())
        self.assertEqual(before, {name: (self.root / name).read_bytes() for name in before})

    def test_rejects_missing_build_output(self):
        with patch.object(release.subprocess, "run"), self.assertRaises(FileNotFoundError):
            release.regenerate(self.root, build_bundle=True)

    def test_check_mode_cannot_build(self):
        result = self.run([sys.executable, str(ROOT / ".github/scripts/prepare_release.py"), "--check", "--build"],
                          cwd=self.root, capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("--check cannot be combined with --build", result.stderr)

    def test_rejects_stale_index_version(self):
        release.bump_version(self.root, "patch")
        with patch.object(release.subprocess, "run"), self.assertRaisesRegex(ValueError, "index"):
            release.regenerate(self.root)

    def test_ci_fails_on_each_generated_diff_without_committing(self):
        self.run(["git", "init", "-q"], cwd=self.root, check=True)
        self.run(["git", "add", *release.GENERATED_FILES], cwd=self.root, check=True)
        self.run(["git", "-c", "user.name=Metadata test", "-c", "user.email=test@example.invalid",
                  "-c", "commit.gpgsign=false",
                  "commit", "-qm", "fixture"], cwd=self.root, check=True)
        head = self.run(["git", "rev-parse", "HEAD"], cwd=self.root, check=True, capture_output=True).stdout
        release.check_generated(self.root)
        for name in release.GENERATED_FILES:
            with self.subTest(name=name):
                path = self.root / name
                before = path.read_bytes()
                path.write_bytes(before + b"\n")
                with patch.object(release.subprocess, "run",
                                  side_effect=lambda command, **kwargs: self.run(command, **kwargs, capture_output=True)) as check:
                    with self.assertRaises(subprocess.CalledProcessError):
                        release.check_generated(self.root)
                    self.assertEqual(check.call_args.args[0][:3], ["git", "diff", "--exit-code"])
                path.write_bytes(before)
        self.assertEqual(head, self.run(["git", "rev-parse", "HEAD"], cwd=self.root,
                                       check=True, capture_output=True).stdout)


if __name__ == "__main__":
    unittest.main()
