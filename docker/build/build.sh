#!/bin/bash

echo
echo Building docker image
echo

environment=
module_dir=
artifact=
version=
repository=
proxy=

while getopts "h?d:a:v:e:r:p:" opt; do
    case "$opt" in
    h|\?)
        echo "USAGE"
        echo "  ./build.sh -d <module-dir> -a <artifact-id> -v <version> -e <environment> -r <repository> -p <proxy>"
        echo
        echo "EXAMPLE"
        echo "  ./build.sh -d $(pwd) -a jwt-auth -v 2.12-0.1 -e production -r 989501292634.dkr.ecr.eu-west-1.amazonaws.com -p 'http://proxy-web.xauth.io'"
        echo
        exit 0
        ;;
    d)  module_dir=$OPTARG
        printf "project directory: \e[1;37m$module_dir\e[0m\n"
        ;;
    a)  artifact=$OPTARG
        printf "    artifact name: \e[1;37m$artifact\e[0m\n"
        ;;
    v)  version=$OPTARG
        printf "          version: \e[1;37m$version\e[0m\n"
        ;;
    e)  environment=$OPTARG
        printf "      environment: \e[1;37m$environment\e[0m\n"
        ;;
    r)  repository=$OPTARG
        printf "       repository: \e[1;37m$repository\e[0m\n"
        ;;
    p)  proxy=$OPTARG
        printf "            proxy: \e[1;37m$proxy\e[0m\n"
        ;;
    esac
done

echo

printf "\e[0m"
printf "\e[1;37m > \e[0mcopying distribution archive '$artifact-$version.zip'\n"
cp $module_dir/target/universal/$artifact-$version.zip $module_dir/docker/build

cd $module_dir/docker/build

tag=$environment-$version

printf "\e[0m"
printf "\e[1;37m > \e[0mbuilding image '$artifact:$tag'\e[2;49;39m\n"
docker build --build-arg PROXY=$proxy \
             --build-arg ENVIRONMENT=$environment \
             --build-arg APP_NAME=$artifact \
             --build-arg APP_VERSION=$version \
             -t $artifact:$tag .

printf "\e[0m"
printf "\e[1;37m > \e[0mtagging image '$repository/$artifact:$tag'\e[2;49;39m\n"
docker tag $artifact:$tag $repository/$artifact:$tag

printf "\e[0m"
printf "\e[1;37m > \e[0msigning-in to aws repository\n"
login_command=$(aws ecr get-login --no-include-email --region eu-west-1)
$login_command &> /dev/null

printf "\e[0m"
printf "\e[1;37m > \e[0mpushing image to '$repository/$artifact:$tag'\e[2;49;39m\n"
docker push $repository/$artifact:$tag

printf "\e[0m"
printf "\e[1;37m > \e[0mdeleting local image '$artifact:$tag'\e[2;49;39m\n"
docker rmi $repository/$artifact:$tag
docker rmi $artifact:$tag

printf "\e[0m"
printf "\e[1;37m > \e[0mdeleting local archive '$artifact-$version.zip'\n"
rm $module_dir/docker/build/$artifact-$version.zip

printf '\e[0mdone.\n\n'
