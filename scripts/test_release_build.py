"""Release staging checks without network access or GitHub publication."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("prepare_release", ROOT / ".github/scripts/prepare_release.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseBuildTest(unittest.TestCase):
    def test_release_commit_message_stays_bounded(self):
        configuration = json.loads((ROOT / ".releaserc").read_text(encoding="utf-8"))
        plugins = dict(configuration["plugins"])
        git = plugins["@semantic-release/git"]
        # The first fork release can include hundreds of KB of upstream history.
        # git's -m argument must never contain those generated release notes.
        self.assertEqual(git["message"], "chore: Release v${nextRelease.version} [skip ci]")
        self.assertIn("CHANGELOG.md", git["assets"])
        self.assertIn("@semantic-release/release-notes-generator", plugins)
        self.assertIn("@MorpheApp/changelog", plugins)
        self.assertIn("@semantic-release/github", plugins)

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "gradle.properties").write_text("version = 1.0.0\n", encoding="utf-8")
        self.version = "1.2.3"
        self.bundle = self.root / f"patches/build/libs/vibe-patch-{self.version}.mpp"
        self.bundle.parent.mkdir(parents=True)
        self.stage = self.root / "patches/build/release/vibe-patch.mpp"
        metadata = {
            "version": self.version,
            "download_url": f"https://github.com/test/vibe/releases/download/v{self.version}/vibe-patch-{self.version}.mpp",
        }
        (self.root / "patches-bundle.json").write_text(json.dumps(metadata), encoding="utf-8")
        environment = patch.dict(os.environ, GITHUB_REPOSITORY="test/vibe", GITHUB_REF_NAME="main")
        environment.start()
        self.addCleanup(environment.stop)

    def archive(self, version="1.2.3", dex=True):
        with ZipFile(self.bundle, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", f"Name: Vibe Patch\r\nVersion: {version}\r\n")
            if dex:
                archive.writestr("classes.dex", b"test DEX fixture")

    def test_build_uses_new_version_and_stages_exact_bundle(self):
        self.archive()

        def run(command, **kwargs):
            # The build must read the new version before any Gradle task runs.
            self.assertEqual((self.root / "gradle.properties").read_text(), "version = 1.2.3\n")
            self.assertEqual(kwargs["cwd"], self.root)
            self.assertTrue(kwargs["check"])

        with patch.object(release.subprocess, "run", side_effect=run) as build:
            release.prepare(self.version, self.root)
        self.assertEqual(build.call_args_list[0].args[0][:3], ["bash", "compile.sh", "--no-daemon"])
        self.assertEqual(self.stage.read_bytes(), self.bundle.read_bytes())

    def test_rejects_wrong_manifest_version_or_missing_dex(self):
        for version, dex in [("1.2.30", True), ("1.2.3", False)]:
            with self.subTest(version=version, dex=dex):
                self.archive(version, dex)
                with patch.object(release.subprocess, "run"), self.assertRaises(ValueError):
                    release.prepare(self.version, self.root)
                self.assertFalse(self.stage.exists())

    def test_rejects_metadata_pointing_to_the_old_bundle(self):
        self.archive()
        metadata = self.root / "patches-bundle.json"
        metadata.write_text(metadata.read_text().replace("vibe-patch-1.2.3.mpp", "patches-1.2.3.mpp"))
        with patch.object(release.subprocess, "run"), self.assertRaises(ValueError):
            release.prepare(self.version, self.root)
        self.assertFalse(self.stage.exists())

    def test_failed_build_removes_stale_staged_asset(self):
        self.stage.parent.mkdir(parents=True)
        self.stage.write_bytes(b"old asset")
        error = subprocess.CalledProcessError(1, ["bash", "compile.sh"])
        with patch.object(release.subprocess, "run", side_effect=error), self.assertRaises(subprocess.CalledProcessError):
            release.prepare(self.version, self.root)
        self.assertFalse(self.stage.exists())


if __name__ == "__main__":
    unittest.main()
