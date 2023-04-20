import { Component, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { FhirConfigService } from '../fhirConfig.service';
import Client from 'fhir-kit-client';
import { Router } from '@angular/router';
import * as R from 'ramda';
import { MatTabChangeEvent } from '@angular/material/tabs/tab-group';
import packageJson from '../../../package.json';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit {
  public version: string = packageJson.version;

  constructor(private router: Router) {}

  update(index): void {}

  tabChanged(tabChangeEvent: MatTabChangeEvent): void {
    console.log('tabChangeEvent => ', tabChangeEvent);
    console.log('index => ', tabChangeEvent.index);
    this.update(tabChangeEvent.index);
  }

  async ngOnInit() {
    this.update(0);
  }
}
