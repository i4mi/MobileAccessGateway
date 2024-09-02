import { Component } from '@angular/core';
import { FhirConfigService } from './fhirConfig.service';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  constructor(
    translateService: TranslateService,
    fhirConfigService: FhirConfigService,
    router: Router,
  ) {
    translateService.setDefaultLang('de');
    translateService.use(translateService.getBrowserLang());

    // When the user loads the front page and there is an OAuth token in the URL, redirect to the Mag page
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('code') && urlParams.has('state')) {
      router.navigate(['/mag'], {
        queryParams: {
          code: urlParams.get('code'),
          state: urlParams.get('state'),
        },
      });
    }

    let base = location.origin + location.pathname;
    if (base.endsWith('/')) {
      base += 'fhir';
    } else {
      base += '/fhir';
    }
    console.log('note: using fhir base ' + base);
    fhirConfigService.changeMagMicroService(base);
  }
}
