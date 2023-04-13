/// <reference path=".,/../../../fhir.r4/index.d.ts" />

import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-patient-detail',
  templateUrl: './patient-detail.component.html',
  styleUrls: ['./patient-detail.component.scss'],
})
export class PatientDetailComponent implements OnInit {
  @Input() patient: fhir.r4.Patient;

  constructor() {}

  ngOnInit() {}
}
