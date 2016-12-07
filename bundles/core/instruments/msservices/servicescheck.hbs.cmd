for /F "tokens=3 delims=: " %%H in ('sc query "{{ origin.shortName }}" ^| findstr "STATE"') do (
  if /I "%%H" NEQ "RUNNING" (
   echo|set /p=off
  ) else (
   echo|set /p=on
  )
)
