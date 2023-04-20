import { Component, OnInit } from '@angular/core';
import { FhirConfigService } from '../fhirConfig.service';
import { PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import FhirClient from 'fhir-kit-client';
import { SelectorMatcher } from '@angular/compiler';

@Component({
  selector: 'app-patients',
  templateUrl: './patients.component.html',
  styleUrls: ['./patients.component.scss'],
})
export class PatientsComponent implements OnInit {
  searched = false;
  bundle: fhir.r4.Bundle;
  dataSource = new MatTableDataSource<fhir.r4.BundleEntry>();

  length = 100;
  pageSize = 10;
  pageIndex = 0;

  client: FhirClient;

  query = {
    _count: this.pageSize,
    _summary: 'true',
    _sort: 'family',
  };

  pageSizeOptions = [this.pageSize];
  public searchName: FormControl;
  public searchNameValue = '';
  public searchGiven: FormControl;
  public searchGivenValue = '';
  public searchFamily: FormControl;
  public searchFamilyValue = '';

  selectedPatient: fhir.r4.Patient;

  constructor(private data: FhirConfigService) {
    this.client = data.getMobileAccessGatewayClient();

    this.searchName = new FormControl();
    this.searchName.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe((term) => this.search());
    this.searchGiven = new FormControl();
    this.searchGiven.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe((term) => this.search());
    this.searchFamily = new FormControl();
    this.searchFamily.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe((term) => this.search());
    this.search();
  }

  search() {
    let searchQuery: any = { ...this.query };

    if (this.searchName.value) {
      searchQuery = { ...searchQuery, name: this.searchName.value };
    }
    if (this.searchFamily.value) {
      searchQuery = { ...searchQuery, family: this.searchFamily.value };
    }
    if (this.searchGiven.value) {
      searchQuery = { ...searchQuery, given: this.searchGiven.value };
    }

    this.client
      .search({ resourceType: 'Patient', searchParams: searchQuery })
      .then((response) => {
        this.pageIndex = 0;
        this.setBundle(<fhir.r4.Bundle>response);
        return response;
      });
  }

  getPatientFamilyName(entry: fhir.r4.BundleEntry): string {
    const patient = <fhir.r4.Patient>entry.resource;
    if (patient.name && patient.name.length > 0 && patient.name[0].family) {
      return patient.name[0].family;
    }
    return '';
  }

  getPatientGivenNames(entry: fhir.r4.BundleEntry): string {
    const patient = <fhir.r4.Patient>entry.resource;
    if (patient.name && patient.name.length > 0 && patient.name[0].given) {
      return (<fhir.r4.Patient>entry.resource).name[0].given.join(' ');
    }
    return '';
  }

  getPatientBirthDate(entry: fhir.r4.BundleEntry): string {
    const patient = <fhir.r4.Patient>entry.resource;
    if (patient.birthDate) {
      return patient.birthDate;
    }
    return '';
  }

  getPatientAddressLines(entry: fhir.r4.BundleEntry): string {
    const patient = <fhir.r4.Patient>entry.resource;
    if (
      patient.address &&
      patient.address.length > 0 &&
      patient.address[0].line
    ) {
      return patient.address[0].line.join(', ');
    }
    return '';
  }

  getPatientAddressCity(entry: fhir.r4.BundleEntry): string {
    const patient = <fhir.r4.Patient>entry.resource;
    if (
      patient.address &&
      patient.address.length > 0 &&
      patient.address[0].city
    ) {
      return patient.address[0].city;
    }
    return '';
  }

  selectRow(row: fhir.r4.BundleEntry) {
    const selection = row.resource;
    const readObj = { resourceType: 'Patient', id: selection.id };
    this.client.read(readObj).then((response) => {
      this.selectedPatient = <fhir.r4.Patient>response;
    });
  }

  goToPage(event: PageEvent) {
    if (event.pageIndex > this.pageIndex) {
      this.client.nextPage({ bundle: this.bundle }).then((response) => {
        this.pageIndex = event.pageIndex;
        this.setBundle(<fhir.r4.Bundle>response);
        console.log('next page called ');
      });
    } else {
      this.client.prevPage({ bundle: this.bundle }).then((response) => {
        this.pageIndex = event.pageIndex;
        this.setBundle(<fhir.r4.Bundle>response);
        console.log('previous page called ');
      });
    }
  }

  setBundle(bundle: fhir.r4.Bundle) {
    this.bundle = <fhir.r4.Bundle>bundle;
    this.length = this.bundle.total;
    this.dataSource.data = this.bundle.entry;
    this.selectedPatient = undefined;
  }

  ngOnInit() {}
}
