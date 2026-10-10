param(
    [Parameter(Mandatory)][string]$ReferenceApk,
    [Parameter(Mandatory)][string]$MorpheRuntimeJar,
    [Parameter(Mandatory)][string]$KotlinHome,
    [string]$JavaExecutable = 'java'
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$apkPath = (Resolve-Path -LiteralPath $ReferenceApk).Path
$runtimePath = (Resolve-Path -LiteralPath $MorpheRuntimeJar).Path
$compilerHome = (Resolve-Path -LiteralPath $KotlinHome).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($runtimePath)
try {
    $versionEntry = $archive.GetEntry('app/morphe/patcher/version.properties')
    if ($null -eq $versionEntry) { throw 'Runtime does not contain Morphe Patcher.' }
    $reader = [System.IO.StreamReader]::new($versionEntry.Open())
    try { $version = $reader.ReadToEnd().Trim() } finally { $reader.Dispose() }
    if ($version -ne 'version=1.14.0') { throw "Expected Morphe Patcher 1.14.0, got $version" }
} finally { $archive.Dispose() }
$outputRoot = Join-Path $repoRoot 'patches/build/yanosik-standalone'
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$compiledJar = Join-Path $outputRoot 'validation.jar'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'patches/src/main/kotlin/app/morphe/patches/yanosik') -Recurse -Filter '*.kt' | ForEach-Object FullName)
$sources += @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'patches/src/test/kotlin/app/morphe/patches/yanosik') -Filter '*.kt' |
    Where-Object Name -ne 'YanosikPatchTest.kt' | ForEach-Object FullName)
$patchResources = Join-Path $repoRoot 'patches/src/main/resources'
& $JavaExecutable -cp "$compilerHome/lib/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler `
    -kotlin-home $compilerHome -Xcontext-parameters -jvm-target 17 `
    -classpath $runtimePath -d $compiledJar @sources
if ($LASTEXITCODE -ne 0) { throw "Kotlin compilation failed: $LASTEXITCODE" }
& $JavaExecutable -Xmx3g -cp "$compiledJar;$runtimePath;$patchResources" `
    app.morphe.patches.yanosik.ValidateYanosikKt $apkPath (Join-Path $outputRoot 'workspace')
if ($LASTEXITCODE -ne 0) { throw "Yanosik DEX validation failed: $LASTEXITCODE" }
