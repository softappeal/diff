cd `dirname "$0"`
chmod +x bin/diff
# ( 'script' | 'scriptNoDuplicates' ) algorithm [ archivePrefix ]
bin/diff script MD5 ../../backup_
