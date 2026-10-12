"""Release metadata checks without APK builds, network access or publication."""
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import parse_qs, urlparse

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
        if command[:2] == ["bash", "gradlew"]:
            self.assertEqual(command, ["bash", "gradlew", "--no-daemon", ":patches:generatePatchesList"])
            index_path = self.root / "patches-list.json"
            index = json.loads(index_path.read_text())
            index["version"] = release.read_version(self.root)
            index_path.write_text(json.dumps(index, indent=2) + "\n")
        elif command[-1] != "scripts/validate-migration.py":
            self.run(command, **kwargs, stdout=subprocess.DEVNULL)

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
