Param(
    [string]$BundleName = "upload-monolith"
)

$repoRoot = $PSScriptRoot
$bundleDir = Join-Path $repoRoot $BundleName

function Copy-Jar {
    param(
        [string]$SourcePattern,
        [string]$DestinationPath,
        [string]$DisplayName
    )
    $jar = Get-ChildItem -Path $SourcePattern -ErrorAction SilentlyContinue |
        Sort-Object -Property LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $jar) {
        Write-Error "$DisplayName 鏈壘鍒帮紝鏈熸湜璺緞: $SourcePattern"
        exit 1
    }
    $destDir = Split-Path -Parent $DestinationPath
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    Copy-Item -LiteralPath $jar.FullName -Destination $DestinationPath -Force
    Write-Host "宸插鍒?$DisplayName => $DestinationPath" -ForegroundColor Yellow
}

if (Test-Path $bundleDir) {
    Remove-Item -Recurse -Force $bundleDir
}

New-Item -ItemType Directory -Path $bundleDir | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleDir 'system') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleDir 'mysql') | Out-Null

# docker-compose.yml
Copy-Item -LiteralPath (Join-Path $repoRoot 'docker-deploy/monolith/docker-compose.yml') -Destination $bundleDir -Force
Copy-Item -LiteralPath (Join-Path $repoRoot '.env') -Destination $bundleDir -Force

# Dockerfile + jar
Copy-Item -LiteralPath (Join-Path $repoRoot 'docker-deploy/monolith/system/Dockerfile') -Destination (Join-Path $bundleDir 'system') -Force
Copy-Jar -SourcePattern (Join-Path $repoRoot 'jeecg-module-system/jeecg-system-start/target/jeecg-system-start-*.jar') `
         -DestinationPath (Join-Path $bundleDir 'system/jeecg-system-start.jar') `
         -DisplayName '鍗曚綋绯荤粺 JAR'

# DB 鍒濆鍖?SQL
$mysqlDir = Join-Path $repoRoot 'docker-deploy/monolith/mysql'
Copy-Item -Path (Join-Path $mysqlDir '*') -Destination (Join-Path $bundleDir 'mysql') -Recurse -Force

# 宸插鍑虹殑闀滃儚锛堝彲閫夛級
$tarPath = Join-Path $repoRoot 'jeecg-monolith.tar'
if (Test-Path $tarPath) {
    Copy-Item -LiteralPath $tarPath -Destination $bundleDir -Force
    Write-Host "宸查檮甯﹂暅鍍忔枃浠? $tarPath" -ForegroundColor Yellow
} else {
    Write-Host "鏈壘鍒?jeecg-monolith.tar锛屽闇€绂荤嚎鍔犺浇璇峰厛杩愯 export-images.ps1 -Mode monolith" -ForegroundColor Cyan
}

Write-Host "鎵撳寘瀹屾垚锛岀洰褰曪細$bundleDir" -ForegroundColor Green

