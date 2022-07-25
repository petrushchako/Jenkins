@echo 
@echo VERSION_NUMBER = %VERSION_NUMBER%
@echo Jenkins job name : %JOB_NAME%
@echo
@echo Project deployed to %Environment%

echo "Generate logs=%LOGS%"
if "%LOGS%"=="true" (
    echo "Logs generated"
) else (
    echo "No logs generated"
)
