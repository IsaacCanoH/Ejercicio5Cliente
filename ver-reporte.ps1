param(
    [ValidatePattern('^[a-zA-Z0-9_-]+$')]
    [string]$Run = 'final'
)

$ErrorActionPreference = 'Stop'
$taskNode = (Get-Command node -ErrorAction Stop).Source
$taskRunner = Join-Path $PSScriptRoot 'node_modules/playwright/cli.js'
if (-not (Test-Path -LiteralPath $taskRunner)) {
    $taskRunner = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/cli.js'
}
if (-not (Test-Path -LiteralPath $taskRunner)) {
    throw 'Falta Playwright. Ejecuta npm.cmd install en Ejercicio5Cliente.'
}
$taskReport = Join-Path $PSScriptRoot "../output/p2/automaticas/$Run/html"
if (-not (Test-Path -LiteralPath (Join-Path $taskReport 'index.html'))) {
    throw "No existe un reporte para la ejecución $Run."
}
& $taskNode $taskRunner show-report $taskReport --host 127.0.0.1 --port 9323
exit $LASTEXITCODE
