build/install/but/bin/but backup src/test/resources/test build/backup_
build/install/but/bin/but node MD5 src/test/resources/test build/test.yass
build/install/but/bin/but print build/test.yass
build/install/but/bin/but delta build/test.yass build/test.yass build/delta.md
