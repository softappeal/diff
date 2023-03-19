diffDir=src/test/resources/test
set -e
build/install/diff/bin/diff MD5 $diffDir build/test.yass
tar -czf /Users/guru/Desktop/backup-`date +%Y.%m.%d-%H.%M.%S`.tar.gzip $diffDir
echo done
