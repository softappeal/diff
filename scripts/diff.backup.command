cd `dirname "$0"`
zip -rq ~/backup_major/major_`date +%Y.%m.%d-%H.%M.%S`.zip . -x ".git/*"
