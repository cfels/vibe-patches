"""Dependency-free structural checks; not a substitute for compilation or device tests."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'patches/src/main/kotlin/app/morphe/patches'
for removed in ('youtube', 'music', 'reddit'):
    assert not (SOURCE / removed).exists(), f'Stale collection: {removed}'
for removed in ('youtube', 'music', 'shared-youtube', 'reddit'):
    assert not (ROOT / 'extensions' / removed).exists(), f'Stale extension: {removed}'

for directory in (ROOT/'patches/src', ROOT/'extensions'):
    for path in directory.rglob('*'):
        if path.suffix not in ('.kt', '.java', '.xml', '.kts') or 'build' in path.parts:
            continue
        text = path.read_text(encoding='utf-8')
        for stale in ('app.morphe.patches.youtube', 'app.morphe.patches.music',
                      'app.morphe.extension.youtube', 'app.morphe.extension.music',
                      'app/morphe/extension/youtube', 'app/morphe/extension/music',
                      'com.google.android.youtube', 'com.google.android.apps.youtube.music',
                      'shared-youtube', 'SharedYouTube', 'app.morphe.patches.reddit',
                      'app.morphe.extension.reddit', 'app/morphe/extension/reddit',
                      'com.reddit.frontpage', 'localesReddit'):
            assert stale not in text, f'Stale reference {stale} in {path.relative_to(ROOT)}'

xml_count = 0
for path in (ROOT/'patches/src/main/resources').rglob('*.xml'):
    document = ET.parse(path)
    xml_count += 1
    if document.getroot().tag == 'resources':
        keys = [(node.tag, node.get('name')) for node in document.getroot() if node.get('name')]
        assert len(keys) == len(set(keys)), f'Duplicate resource in {path.relative_to(ROOT)}'

data = json.loads((ROOT/'patches-list.json').read_text(encoding='utf-8'))
old_packages = {'com.google.android.youtube', 'com.google.android.apps.youtube.music',
                'com.reddit.frontpage'}
yanosik = []
for patch in data['patches']:
    for target in patch.get('compatiblePackages') or []:
        assert target['packageName'] not in old_packages, 'Removed app still exposed in index'
        if target['packageName'] == 'pl.neptis.yanosik.mobi.android':
            yanosik.append(patch)
            assert [t['version'] for t in target['targets']] == ['26.9.0']
            assert target['targets'][0]['minSdk'] == 29
names = [p['name'] for p in yanosik]
assert len(names) == len(set(names)) == 6, f'Unexpected Yanosik index: {names}'
assert set(names) == {'Hide banner ads', 'Disable startup ad campaigns',
                      'Disable voice ads', 'Suppress Premium promotions',
                          'Override local PRO checks', 'Disable telemetry'}
declared = []
for path in (SOURCE/'yanosik').rglob('*.kt'):
    declared.extend(re.findall(r'bytecodePatch\(\s*name = "([^"]+)"', path.read_text()))
assert sorted(names) == sorted(declared), 'Index/source names differ'
assert not any(path.name == 'reddit' for path in (ROOT/'patches/src/main/resources').rglob('*'))
assert len(data['patches']) == 11
assert len([p for p in data['patches'] if not p.get('compatiblePackages')]) == 5
print(f'PASS migration structure: {len(data["patches"])} indexed patches; {xml_count} XML files')
