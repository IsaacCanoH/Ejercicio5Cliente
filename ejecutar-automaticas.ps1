param(
    [ValidatePattern('^[a-zA-Z0-9_-]+$')]
    [string]$Run = 'final',
    [string]$FrontendUrl = 'http://localhost:8080',
    [string]$BackendUrl = 'http://localhost:8081',
    [string]$Browser = 'chrome',
    [string]$Filtro = ''
)

$ErrorActionPreference = 'Stop'
$taskNode = (Get-Command node -ErrorAction Stop).Source
$taskRunner = Join-Path $PSScriptRoot 'node_modules/playwright/cli.js'
if (-not (Test-Path -LiteralPath $taskRunner)) {
    $taskModules = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'
    $taskRunner = Join-Path $taskModules 'playwright/cli.js'
    if (-not (Test-Path -LiteralPath $taskRunner)) {
        throw 'Falta Playwright. Ejecuta npm.cmd install en Ejercicio5Cliente.'
    }
    $env:NODE_PATH = $taskModules
}

# Comprobaciones de disponibilidad antes de crear datos de prueba.
$null = Invoke-WebRequest -Uri "$FrontendUrl/login" -UseBasicParsing -TimeoutSec 10
$null = Invoke-WebRequest -Uri "$BackendUrl/buscarProyectos" -Method Post -UseBasicParsing -TimeoutSec 10
$env:P2_RUN = $Run
$env:FRONTEND_URL = $FrontendUrl
$env:BACKEND_URL = $BackendUrl
$env:P2_BROWSER = $Browser
$taskArguments = @($taskRunner, 'test', '--config', (Join-Path $PSScriptRoot 'playwright.config.js'))
if ($Filtro) { $taskArguments += @('--grep', $Filtro) }
& $taskNode @taskArguments
exit $LASTEXITCODE
