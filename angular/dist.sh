ng build --configuration production
cp -r ../MobileAccessGateway/src/main/resources/static/not-authenticated.html dist
rm -rf ../MobileAccessGateway/src/main/resources/static/*
cp -r dist/* ../MobileAccessGateway/src/main/resources/static
