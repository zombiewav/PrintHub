<#
  Builds the portable PrintHub for Windows: a PrintHub folder with PrintHub.exe and its own Java inside (the shop's
  computer does not need Java installed), plus the same folder zipped for handing out.

  Output:
    dist\PrintHub\PrintHub.exe                 (the program; keep the whole folder together)
    dist\PrintHub-<version>-portable.zip       (the folder, zipped)

  Needs: JDK 17 or newer (jpackage comes with it), and the libraries in the Maven cache (%USERPROFILE%\.m2), which
  NetBeans downloads the first time it builds the project. No Maven, WiX or other tools.

  Run from the project folder:
    powershell -ExecutionPolicy Bypass -File packaging\build-portable.ps1
#>
param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$m2 = Join-Path $env:USERPROFILE ".m2\repository"

# The libraries in pom.xml (keep these versions the same as there).
$libraries = @(
    "com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar",
    "org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar",
    "org/apache/pdfbox/pdfbox/3.0.3/pdfbox-3.0.3.jar",
    "org/apache/pdfbox/pdfbox-io/3.0.3/pdfbox-io-3.0.3.jar",
    "org/apache/pdfbox/fontbox/3.0.3/fontbox-3.0.3.jar",
    "commons-logging/commons-logging/1.3.3/commons-logging-1.3.3.jar",
    "com/google/zxing/core/3.5.3/core-3.5.3.jar"
)

# The parts of Java PrintHub uses (found with jdeps), so the bundled Java stays small.
$modules = "java.base,java.desktop,java.sql,java.logging,java.naming,java.management,jdk.httpserver,jdk.unsupported,jdk.localedata"

function Find-JdkTool([string]$name) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\$name.exe"))) {
        return (Join-Path $env:JAVA_HOME "bin\$name.exe")
    }
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "Cannot find $name. Install JDK 17 or newer, or set JAVA_HOME to it."
}

$javac = Find-JdkTool "javac"
$jar = Find-JdkTool "jar"
$jpackage = Find-JdkTool "jpackage"

$jars = foreach ($lib in $libraries) {
    $path = Join-Path $m2 $lib
    if (-not (Test-Path $path)) {
        throw "Missing library $lib. Open the project in NetBeans and build it once (it downloads the libraries), then run this again."
    }
    $path
}

$work = Join-Path $root "target\portable"
$classes = Join-Path $work "classes"
$input = Join-Path $work "input"
$dist = Join-Path $root "dist"
$appFolder = Join-Path $dist "PrintHub"
$zip = Join-Path $dist "PrintHub-$Version-portable.zip"

Write-Host "Cleaning old build..."
foreach ($old in @($work, $appFolder, $zip)) {
    if (Test-Path $old) { Remove-Item -Recurse -Force $old }
}
New-Item -ItemType Directory -Force $classes, $input, $dist | Out-Null

Write-Host "Compiling..."
$sources = Get-ChildItem -Recurse -Filter *.java (Join-Path $root "src\main\java") | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }
$argFile = Join-Path $work "sources.txt"
[System.IO.File]::WriteAllLines($argFile, $sources)
& $javac -nowarn --release 17 -encoding UTF-8 -cp ($jars -join ";") -d $classes "@$argFile"
if ($LASTEXITCODE -ne 0) { throw "Compiling failed." }
$resources = Join-Path $root "src\main\resources"
if (Test-Path $resources) { Copy-Item -Recurse -Force (Join-Path $resources "*") $classes }

Write-Host "Packing PrintHub.jar..."
& $jar --create --file (Join-Path $input "PrintHub.jar") --main-class com.printhub.PrintHub -C $classes .
if ($LASTEXITCODE -ne 0) { throw "Making the jar failed." }
foreach ($j in $jars) { Copy-Item $j $input }

# JDK 21 renamed jlink's compression levels ("2" became "zip-6").
$jdkMajor = [int](((& $jpackage --version) -split '\.')[0])
$compress = if ($jdkMajor -ge 21) { "zip-6" } else { "2" }

Write-Host "Making PrintHub.exe with its own Java (this takes a minute)..."
& $jpackage --type app-image --name PrintHub --app-version $Version --vendor "PrintHub" `
    --description "PrintHub - Printing Supplies and Service Management System" `
    --icon (Join-Path $PSScriptRoot "PrintHub.ico") `
    --input $input --main-jar PrintHub.jar --main-class com.printhub.PrintHub `
    --add-modules $modules `
    --jlink-options "--strip-debug --no-man-pages --no-header-files --compress=$compress" `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

$readme = @"
PrintHub $Version
=================

Start: double-click PrintHub.exe. Keep this whole folder together; the .exe needs the other files next to it.
Java is included, so nothing else needs to be installed.

First start: PrintHub asks for your shop's name and creates the owner's admin account. Prices and the usual
supplies are filled in to change later. Before the first order, restock each supply in Inventory with what is on
your shelf (supplies start at 0).

Your data (database and customer files) is kept in:
    %LOCALAPPDATA%\PrintHub
It stays there when you replace this folder with a newer version. Back up that folder to keep your records safe.

Customer portal: in PrintHub open Customer Portal and press Start. If Windows asks, allow PrintHub through the
firewall. Phones on the computer's hotspot (or the same Wi-Fi) scan the QR code shown there.

Demo for a presentation: on the first start, press "Load demo data instead" (sign in with admin / admin123).
Do not use the demo for a real shop.
"@
Set-Content -Path (Join-Path $appFolder "Read me.txt") -Value $readme -Encoding UTF8

Write-Host "Zipping..."
Compress-Archive -Path $appFolder -DestinationPath $zip -CompressionLevel Optimal

$size = "{0:N0} MB" -f ((Get-Item $zip).Length / 1MB)
Write-Host ""
Write-Host "Done."
Write-Host "  Program: $appFolder\PrintHub.exe"
Write-Host "  Zip:     $zip ($size)"
