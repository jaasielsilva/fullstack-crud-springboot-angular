[CmdletBinding()]
param(
    [string]$PrometheusUrl = "http://127.0.0.1:9090",
    [string[]]$ExpectedJobs = @("erp-api-prod", "erp-api-hml"),
    [int]$TimeoutSeconds = 90,
    [int]$PollIntervalSeconds = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-TargetJobName {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Target
    )

    if ($null -ne $Target.labels -and $null -ne $Target.labels.job -and $Target.labels.job) {
        return [string]$Target.labels.job
    }

    if ($null -ne $Target.discoveredLabels -and $null -ne $Target.discoveredLabels.job -and $Target.discoveredLabels.job) {
        return [string]$Target.discoveredLabels.job
    }

    return $null
}

function Get-TargetSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl
    )

    $uri = "$($BaseUrl.TrimEnd('/'))/api/v1/targets"
    $response = Invoke-RestMethod -Uri $uri -Method Get

    if ($response.status -ne "success") {
        throw "Prometheus respondeu status '$($response.status)' para $uri."
    }

    return @($response.data.activeTargets)
}

function Get-TrackedTargets {
    param(
        [Parameter(Mandatory = $true)]
        [psobject[]]$Targets,
        [Parameter(Mandatory = $true)]
        [string[]]$Jobs
    )

    $tracked = foreach ($target in $Targets) {
        $jobName = Get-TargetJobName -Target $target
        if ($jobName -and $jobName -in $Jobs) {
            [pscustomobject]@{
                Job       = $jobName
                Health    = [string]$target.health
                ScrapeUrl = [string]$target.scrapeUrl
                LastError = [string]$target.lastError
            }
        }
    }

    return @($tracked | Sort-Object Job -Unique)
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

do {
    try {
        $allTargets = Get-TargetSnapshot -BaseUrl $PrometheusUrl
        $trackedTargets = Get-TrackedTargets -Targets $allTargets -Jobs $ExpectedJobs
    }
    catch {
        Write-Warning $_.Exception.Message
        $trackedTargets = @()
    }

    foreach ($job in $ExpectedJobs) {
        $target = $trackedTargets | Where-Object { $_.Job -eq $job } | Select-Object -First 1
        if ($null -eq $target) {
            Write-Host ("[{0}] MISSING" -f $job)
            continue
        }

        $details = if ([string]::IsNullOrWhiteSpace($target.LastError)) {
            $target.ScrapeUrl
        }
        else {
            "{0} | lastError={1}" -f $target.ScrapeUrl, $target.LastError
        }

        Write-Host ("[{0}] {1} -> {2}" -f $target.Job, $target.Health.ToUpperInvariant(), $details)
    }

    $allUp = $true
    foreach ($job in $ExpectedJobs) {
        $target = $trackedTargets | Where-Object { $_.Job -eq $job } | Select-Object -First 1
        if ($null -eq $target -or $target.Health -ne "up") {
            $allUp = $false
            break
        }
    }

    if ($allUp) {
        Write-Host "Todos os targets esperados estao UP."
        exit 0
    }

    if ((Get-Date) -ge $deadline) {
        break
    }

    Start-Sleep -Seconds $PollIntervalSeconds
}
while ($true)

throw "Timeout apos $TimeoutSeconds segundos aguardando os targets $($ExpectedJobs -join ', ') ficarem UP em $PrometheusUrl."
