param(
    [switch] $Runtime,
    [int] $TimeoutSeconds = 180,
    [string] $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'

$resultsDir = Join-Path $RepoRoot 'functional-tests\results'
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

& (Join-Path $PSScriptRoot 'Test-SourceFeatureParity.ps1') -RepoRoot $RepoRoot

function Invoke-FunctionalClientRun {
    param(
        [hashtable] $Entry,
        [int] $TimeoutSeconds,
        [string] $ResultsDir
    )

    $slug = "$($Entry.Version)-$($Entry.Loader)" -replace '[^A-Za-z0-9.-]', '-'
    $stdout = Join-Path $ResultsDir "$slug.out.log"
    $stderr = Join-Path $ResultsDir "$slug.err.log"
    Remove-Item -LiteralPath $stdout, $stderr -ErrorAction SilentlyContinue

    $oldEnabled = $env:SIU_FUNCTIONAL_TESTS
    $oldExit = $env:SIU_FUNCTIONAL_TEST_EXIT
    $oldLoader = $env:SIU_FUNCTIONAL_TEST_LOADER
    $env:SIU_FUNCTIONAL_TESTS = 'true'
    $env:SIU_FUNCTIONAL_TEST_EXIT = 'true'
    $env:SIU_FUNCTIONAL_TEST_LOADER = $Entry.Loader

    try {
        Write-Host "Launching $($Entry.Version) $($Entry.Loader) functional runtime..." -ForegroundColor Cyan
        $process = Start-Process -FilePath '.\gradlew.bat' `
            -ArgumentList @('--no-daemon', '-PincludeFunctionalTests=true', $Entry.Task, '--console=plain') `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -PassThru `
            -WindowStyle Hidden

        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        $summary = $null
        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 2
            $combined = ''
            if (Test-Path -LiteralPath $stdout) {
                $combined += Get-Content -LiteralPath $stdout -Raw
            }
            if (Test-Path -LiteralPath $stderr) {
                $combined += "`n" + (Get-Content -LiteralPath $stderr -Raw)
            }

            $summary = [regex]::Match($combined, 'SIU_FUNCTIONAL_TEST_SUMMARY status=(PASS|FAIL)[^\r\n]*')
            if ($summary.Success) {
                break
            }

            if ($process.HasExited) {
                break
            }
        }

        Stop-ProcessTree -RootProcessId $process.Id

        if (-not $summary -or -not $summary.Success) {
            throw "No SIU_FUNCTIONAL_TEST_SUMMARY found for $($Entry.Version) $($Entry.Loader). Logs: $stdout $stderr"
        }

        Write-Host $summary.Value -ForegroundColor Green
        if ($summary.Groups[1].Value -ne 'PASS') {
            throw "Functional runtime failed for $($Entry.Version) $($Entry.Loader). Logs: $stdout $stderr"
        }
    } finally {
        $env:SIU_FUNCTIONAL_TESTS = $oldEnabled
        $env:SIU_FUNCTIONAL_TEST_EXIT = $oldExit
        $env:SIU_FUNCTIONAL_TEST_LOADER = $oldLoader
    }
}

function Stop-ProcessTree {
    param([int] $RootProcessId)

    $all = Get-CimInstance Win32_Process
    $childrenByParent = @{}
    foreach ($process in $all) {
        if (-not $childrenByParent.ContainsKey($process.ParentProcessId)) {
            $childrenByParent[$process.ParentProcessId] = [System.Collections.Generic.List[int]]::new()
        }
        $childrenByParent[$process.ParentProcessId].Add([int] $process.ProcessId)
    }

    $ids = [System.Collections.Generic.List[int]]::new()
    $stack = [System.Collections.Generic.Stack[int]]::new()
    $stack.Push($RootProcessId)
    while ($stack.Count -gt 0) {
        $id = $stack.Pop()
        $ids.Add($id)
        if ($childrenByParent.ContainsKey($id)) {
            foreach ($child in $childrenByParent[$id]) {
                $stack.Push($child)
            }
        }
    }

    foreach ($id in ($ids | Select-Object -Unique | Sort-Object -Descending)) {
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
}

Push-Location $RepoRoot
try {
    & .\gradlew.bat --no-daemon -PincludeFunctionalTests=true functionalTestCompile --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "functionalTestCompile failed with exit code $LASTEXITCODE"
    }

    if (-not $Runtime) {
        Write-Host 'Compile/static checks completed. Pass -Runtime to launch each client and run the in-game harness.' -ForegroundColor Green
        exit 0
    }

    $matrix = @(
        @{ Version = '1.20.1'; Loader = 'fabric'; Task = ':1.20.1:fabric:runClient' },
        @{ Version = '1.20.1'; Loader = 'forge'; Task = ':1.20.1:forge:runClient' },
        @{ Version = '1.21.1'; Loader = 'fabric'; Task = ':1.21.1:fabric:runClient' },
        @{ Version = '1.21.1'; Loader = 'neoforge'; Task = ':1.21.1:neoforge:runClient' },
        @{ Version = '1.21.11'; Loader = 'fabric'; Task = ':1.21.11:fabric:runClient' },
        @{ Version = '1.21.11'; Loader = 'neoforge'; Task = ':1.21.11:neoforge:runClient' },
        @{ Version = '26.1.2'; Loader = 'fabric'; Task = ':26.1.2:fabric:runClient' },
        @{ Version = '26.1.2'; Loader = 'neoforge'; Task = ':26.1.2:neoforge:runClient' },
        @{ Version = '26.2'; Loader = 'fabric'; Task = ':26.2:fabric:runClient' },
        @{ Version = '26.2'; Loader = 'neoforge'; Task = ':26.2:neoforge:runClient' }
    )

    foreach ($entry in $matrix) {
        Invoke-FunctionalClientRun -Entry $entry -TimeoutSeconds $TimeoutSeconds -ResultsDir $resultsDir
    }
} finally {
    Pop-Location
}
