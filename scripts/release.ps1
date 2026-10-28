<#
.SYNOPSIS
  Builds the signed release APK and publishes it as a GitHub release.

.EXAMPLE
  ./scripts/release.ps1 -Version 0.2.0 -Notes "Adds the classical catalog."

Before running it: bump versionCode and versionName in app/build.gradle.kts, commit and push.
Requires keystore.properties (not in the repo), the GitHub CLI logged in (gh auth login) and JDK 17.
#>
param(
    [Parameter(Mandatory)] [string] $Version,
    [Parameter(Mandatory)] [string] $Notes
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)

if (-not (Test-Path keystore.properties)) { throw 'keystore.properties not found: the release would be unsigned.' }
if ((git status --porcelain)) { throw 'Commit your changes first: the release must match the tag.' }

$gradle = Get-Content app/build.gradle.kts -Raw
if ($gradle -notmatch "versionName\s*=\s*`"$([regex]::Escape($Version))`"") {
    throw "versionName in app/build.gradle.kts is not $Version."
}

./gradlew.bat :core:test :app:testDebugUnitTest :app:assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Tests or build failed.' }

$apk = "build/QuietRingtones-$Version.apk"
New-Item -ItemType Directory -Force build | Out-Null
Copy-Item app/build/outputs/apk/release/app-release.apk $apk -Force

gh release create "v$Version" $apk --title "QuietRingtones $Version" --notes $Notes
