cmd /c build\install\but\bin\but backup src/test/resources/test build/backup_
cmd /c build\install\but\bin\but node MD5 src/test/resources/test build/test.yass
cmd /c build\install\but\bin\but print build/test.yass
cmd /c build\install\but\bin\but delta build/test.yass build/test.yass build/delta.txt
