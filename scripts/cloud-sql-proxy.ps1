# Run Cloud SQL Auth Proxy for local dev (Option A — direct JDBC mode)
# Install: https://cloud.google.com/sql/docs/postgres/sql-proxy#install
#
# Usage:
#   .\scripts\cloud-sql-proxy.ps1 -ConnectionName "your-project:us-east4:legally-7f34d-instance"
#
# Then set backend env:
#   DATABASE_MODE=direct
#   DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/legally-7f34d-database

param(
    [Parameter(Mandatory = $true)]
    [string]$ConnectionName,

    [int]$Port = 5432
)

Write-Host "Starting Cloud SQL Auth Proxy on port $Port for $ConnectionName"
Write-Host "Backend DATABASE_URL: jdbc:postgresql://127.0.0.1:$Port/legally-7f34d-database"

cloud-sql-proxy $ConnectionName --port $Port
