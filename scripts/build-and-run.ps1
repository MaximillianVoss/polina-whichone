param([Parameter(ValueFromRemainingArguments=$true)][string[]]$AppArguments)
$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $projectDirectory
try {
    & .\mvnw.cmd -B clean verify
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE" }
    $jarPath = Join-Path $projectDirectory 'target\whichone-1.0.0.jar'
    if (!(Test-Path -LiteralPath $jarPath -PathType Leaf)) { throw "Expected JAR not found: $jarPath" }
    $port = if ($env:PORT) { $env:PORT } else { '8080' }
    Write-Host "Starting WhichOne. Open http://localhost:$port/"
    & java -jar $jarPath @AppArguments
    if ($LASTEXITCODE -ne 0) { throw "Application stopped with exit code $LASTEXITCODE" }
}
finally { Pop-Location }
