set -e
archivePrefix=~/backup_major/backup_major_
cd `dirname "$0"`
bin/diff MD5 .. node.yass
cd ..
tar --disable-copyfile --exclude='.DS_Store' -czf $archivePrefix`date +%Y.%m.%d-%H.%M.%S`.tar.gzip .
echo archive \'$archivePrefix\' written
