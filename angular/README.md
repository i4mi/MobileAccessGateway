# client frontend to mag

# development setup

if you are not using the devcontainer from this project with [VS Code Remote Development](https://code.visualstudio.com/docs/remote/containers) extension you need to have angular cli and [yarn](https://yarnpkg.com/en/) installed:

```
npm install -g @angular/cli
npm install
ng build --configuration development
```

and then

ng serve --configuration development

## usage

Run `ng serve --configuration development` to start the app, app will be at [http://localhost:4200](http://localhost:4200/).
If you use the Visual Code functionality with Remote containers: Open Folder in container option, you need to start it with `ng serve --host 0.0.0.0`.

if you use localhost and have cross site blocking issues within chrome start chrome directly from command line (osx)

/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --disable-features=CrossSiteDocumentBlockingAlways,CrossSiteDocumentBlockingIfIsolating

## running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## frontend for matchbox

this angular app is directly provided with matchbox

```
ng build --configuration production
rm -rf ../MobileAccessGateway/src/main/resources/static/*
cp -r dist/* ../MobileAccessGateway/src/main/resources/static
```

## Contributing

Have a look at [contributing](CONTRIBUTING.md).

## setup fhir-kit-client

```
yarn add fhir-kit-client
yarn add @types/fhir-kit-client --dev
yarn add debug
yarn add @types/debug --dev

```

see also https://github.com/visionmedia/debug/issues/305
enter in chrome console for debugging the following:
localStorage.debug = 'fhir-kit-client:\*';

localStorage.debug = 'fhir-kit-client:_,app:_';
