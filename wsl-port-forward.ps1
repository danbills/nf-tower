# PowerShell Script to forward port 8000 from Windows host to WSL2
# Run as Administrator in PowerShell

$wslIp = (wsl hostname -I).Trim().Split(" ")[0]
Write-Host "Detected WSL2 IP: $wslIp" -ForegroundColor Cyan

Write-Host "Adding netsh portproxy rule for port 8000..." -ForegroundColor Yellow
netsh interface portproxy add v4tov4 listenport=8000 listenaddress=0.0.0.0 connectport=8000 connectaddress=$wslIp

Write-Host "Active PortProxy Rules:" -ForegroundColor Green
netsh interface portproxy show all
