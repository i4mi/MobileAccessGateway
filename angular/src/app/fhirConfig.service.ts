import { Injectable } from '@angular/core';
import FhirClient from 'fhir-kit-client';
import { AuthConfig } from 'angular-oauth2-oidc';

@Injectable({
  providedIn: 'root',
})
export class FhirConfigService {
  constructor() {}

  public changeFhirMicroService(server: string) {
    localStorage.setItem('fhirMicroServer', server);
  }

  public changeMagMicroService(server: string) {
    localStorage.setItem('magMicroService', server);
  }

  getFhirMicroService(): string {
    const service = localStorage.getItem('fhirMicroServer');
    return service ? service : '/matchbox/fhir';
  }

  getMobileAccessGatewayService(): string {
    const service = localStorage.getItem('magMicroService');
    return service ? service : '/mag/fhir';
  }

  getMobileAccessGatewayIDPEnumerationUrl(): string {
    const service = localStorage.getItem('magMicroService');
    return service.replace('/fhir', '/camel/idps');
  }

  getMobileAccessGatewayLoginUrl(): string {
    const service = localStorage.getItem('magMicroService');
    return service.replace('/fhir', '/camel/authorize');
  }

  getMobileAccessGatewayTokenEndpoint(): string {
    const service = localStorage.getItem('magMicroService');
    return service.replace('/fhir', '/camel/token');
  }

  getMobileAccessGatewayAssertionEndpoint(): string {
    const service = localStorage.getItem('magMicroService');
    return service.replace('/fhir', '/camel/assertion');
  }

  getRedirectUri(): string {
    return location.origin + location.pathname + '#/mag';
  }

  getClientId(): string {
    if (this.getRedirectUri().indexOf('localhost') >= 0) {
      return 'matchboxdev';
    }
    return 'matchbox';
  }

  getClientSecret() {
    return 'cd8455fc-e294-465a-8c86-35ae468c6b2f';
  }

  getFhirClient() {
    return new FhirClient({ baseUrl: this.getFhirMicroService() });
  }

  getMobileAccessGatewayClient() {
    return new FhirClient({ baseUrl: this.getMobileAccessGatewayService() });
  }

  async getAuthCodeFlowConfigFromMetadata(metadataUrl: string): Promise<AuthConfig> {
    const metadata = await fetch(metadataUrl).then(r => r.json());
    console.log(metadata);
    return {
      loginUrl: metadata.authorization_endpoint,
      tokenEndpoint: metadata.token_endpoint,
      clientId: this.getClientSecret(),
      redirectUri: location.origin + location.pathname,
      responseType: 'code',
      showDebugInformation: true,
      timeoutFactor: 0.75,
    } as AuthConfig;
  }

  getAuthCodeFlowConfigForEHS(): AuthConfig {
    return {
      loginUrl: 'https://ehealthsuisse.ihe-europe.net/iua-simulator/rest/ch/authorize',
      tokenEndpoint: 'https://ehealthsuisse.ihe-europe.net/iua-simulator/rest/ch/token',
      clientId: this.getClientSecret(),
      redirectUri: location.origin + location.pathname,
      customQueryParams: {
        aud: location.origin + location.pathname,
      },
      responseType: 'code',
      showDebugInformation: true,
      timeoutFactor: 0.75,
    } as AuthConfig;
  }

  getAuthCodeFlowConfig(provider: string): AuthConfig {
    const idpAlias = provider ? ("/alias/" + provider) : "";
    return {
      // Url of the Identity Provider

      loginUrl: this.getMobileAccessGatewayLoginUrl() + idpAlias,
      // URL of the SPA to redirect the user to after login
      // redirectUri: window.location.origin + '/index.html',
      redirectUri: this.getRedirectUri(),

      tokenEndpoint: this.getMobileAccessGatewayTokenEndpoint() + idpAlias,

      // The SPA's id. The SPA is registerd with this id at the auth-server
      // clientId: 'server.code',
      clientId: this.getClientId(),
      responseType: 'code',

      // set the scope for the permissions the client should request
      // The first four are defined by OIDC.
      // Important: Request offline_access to get a refresh token
      // The api scope is a usecase specific one
      scope: 'todo',

      dummyClientSecret: this.getClientSecret(),

      showDebugInformation: true,

      // Refresh token after 75% of its live time
      timeoutFactor: 0.75,
    };
  }
}
