# =============================================================================
#  Captures the three terminal screenshots the CIS6003 report needs.
#
#  These are REAL screen captures of a REAL console window running the real
#  command. Nothing is rendered or simulated.
#
#  HOW, AND WHY IT IS DONE THIS WAY
#  On Windows 11 the console is hosted by Windows Terminal, so the powershell
#  process that was started owns no window of its own and MainWindowHandle
#  comes back as 0 - there is no window handle to capture. Rather than fight
#  that, the console is opened MAXIMISED so that it covers the whole screen,
#  and the whole screen is captured. The result is the same picture, and it
#  works whichever console host Windows decides to use.
#
#      powershell -ExecutionPolicy Bypass -File capture-terminals.ps1
# =============================================================================

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

$OUT  = "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report\screenshots"
$PROJ = "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic"
$FLAG = "D:\DevTools\shots\done.flag"

function Capture-Command {
    param([string]$Name, [string]$Title, [string]$Command, [int]$TimeoutSeconds = 400)

    Write-Host "  running: $Name ..." -NoNewline
    Remove-Item $FLAG -ErrorAction SilentlyContinue

    $inner = @"
`$Host.UI.RawUI.WindowTitle = '$Title'
Clear-Host
Set-Location '$PROJ'
$Command
New-Item -ItemType File -Path '$FLAG' -Force | Out-Null
"@

    $enc = [Convert]::ToBase64String([System.Text.Encoding]::Unicode.GetBytes($inner))
    $p = Start-Process powershell.exe -PassThru -WindowStyle Maximized `
         -ArgumentList "-NoExit", "-NoProfile", "-EncodedCommand", $enc

    $waited = 0
    while (-not (Test-Path $FLAG) -and $waited -lt $TimeoutSeconds) {
        Start-Sleep -Milliseconds 500
        $waited += 0.5
    }

    if (-not (Test-Path $FLAG)) {
        Write-Host " FAILED - the command did not finish in $TimeoutSeconds s" -ForegroundColor Red
        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
        return
    }

    Start-Sleep -Seconds 3      # let the last lines paint and the window settle

    $b   = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bmp = New-Object System.Drawing.Bitmap $b.Width, $b.Height
    $g   = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($b.Location, [System.Drawing.Point]::Empty, $b.Size)
    $bmp.Save("$OUT\$Name.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()

    Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 400
    Write-Host " captured ($($b.Width) x $($b.Height))" -ForegroundColor Green
}

Write-Host "Capturing terminal evidence..."
Write-Host "A console window will open maximised for each one. Please do not"
Write-Host "click anything until it says Done."
Write-Host ""

# Figure 15 - the whole test suite passing
Capture-Command -Name "24-tests-passing" -Title "CIS6003 - full test suite" -Command @'
Write-Host "CIS6003 Advanced Programming - Sunrise Dental Clinic" -ForegroundColor Cyan
Write-Host "Running the full automated test suite: .\mvnw.cmd test" -ForegroundColor Cyan
Write-Host ""
.\mvnw.cmd test 2>&1 | Select-Object -Last 22
'@

# Figure 14 - the TDD red commit: only test files, nothing in src/main
Capture-Command -Name "25-tdd-red-commit" -Title "CIS6003 - TDD red commit" -TimeoutSeconds 60 -Command @'
Write-Host "CIS6003 Advanced Programming - test-driven development evidence" -ForegroundColor Cyan
Write-Host "The RED commit of step 7: the tests were committed BEFORE the code." -ForegroundColor Cyan
Write-Host "Every file below is a test file. Nothing in src/main was touched." -ForegroundColor Cyan
Write-Host ""
git --no-pager show 2d3904e --stat --format="commit %h    %ad%n%s%n"
'@

# Figure 9 - the work the database does by itself
Capture-Command -Name "26-database-triggers-audit" -Title "CIS6003 - database evidence" -TimeoutSeconds 60 -Command @'
Write-Host "CIS6003 Advanced Programming - advanced database features in use" -ForegroundColor Cyan
$m = "D:\DevTools\mysql-8.4.9-winx64\bin\mysql.exe"
$db = @("-u","root","--protocol=TCP","-h","127.0.0.1","-P","3306","-D","sunrise_dental")
Write-Host ""
Write-Host "The 10 triggers created by triggers.sql:" -ForegroundColor Yellow
& $m @db -e "SELECT trigger_name, event_manipulation AS fires_on, event_object_table AS on_table FROM information_schema.triggers WHERE trigger_schema='sunrise_dental' ORDER BY event_object_table, trigger_name;"
Write-Host "Audit rows written by those triggers, and by no Java code:" -ForegroundColor Yellow
& $m @db -e "SELECT appointment_no, action, old_status, new_status, changed_at FROM appointment_audit ORDER BY audit_id LIMIT 4;"
Write-Host "total_amount is a GENERATED COLUMN - MySQL works it out, Java never writes it:" -ForegroundColor Yellow
& $m @db -e "SELECT bill_no, treatment_cost, consultation_fee, discount, total_amount, payment_status FROM bills;"
'@

Remove-Item $FLAG -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "Done. Written to $OUT"



