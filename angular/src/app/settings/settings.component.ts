import { Component, OnInit } from '@angular/core';
import { FhirConfigService } from '../fhirConfig.service';
import { Subscription } from 'rxjs';
import debug from 'debug';
import { MatTableDataSource } from '@angular/material/table';
import Client from 'fhir-kit-client';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
})
export class SettingsComponent implements OnInit {
  mobileAccessGateways = [
    'https://test.ahdis.ch/mag-cara/fhir',
    'https://test.ahdis.ch/eprik-proxy-cara/camel/mag-cara/fhir',
    'https://test.ahdis.ch/mag-pmp2/fhir',
    'https://test.ahdis.ch/eprik-proxy-cara/camel/mag-pmp/fhir',
    'https://test.ahdis.ch/mag-bfh/fhir',
    'https://test.ahdis.ch/mag-test/fhir',
    'https://test.ahdis.ch/mag-test-emedo/fhir',
    'http://localhost:8080/matchbox/fhir',
    'http://localhost:9090/mag-pmp2/fhir',
    'http://localhost:9090/mag-cara/fhir',
    'http://localhost:18002/eprik-proxy-cara/camel/mag-pmp/fhir',
  ];

  subscriptionFhir: Subscription;
  baseUrlFhir: string;
  subscriptionMag: Subscription;
  baseUrlMag: string;

  client: Client;

  constructor(private data: FhirConfigService, private router: Router) {
    this.client = data.getMobileAccessGatewayClient();
  }

  ngOnInit() {
    this.baseUrlMag = this.data.getMobileAccessGatewayService();
  }

  getMagSelectedValue(): string {
    return this.baseUrlMag;
  }

  setMagSelectedValue(value: string) {
    debug('setting new server to ' + value);
    this.data.changeMagMicroService(value);
  }
}
