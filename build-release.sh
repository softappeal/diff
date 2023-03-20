set -e

docker container rm --force                 project-container
docker image     rm --force                                         project-image

docker image     build                                        --tag project-image .
docker container create              --name project-container       project-image ./gradlew -Pversion=`git describe`
docker container start --interactive        project-container

docker container cp                         project-container:/project/build/distributions build
