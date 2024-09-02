import { Component } from '@angular/core';
import { FhirConfigService } from './fhirConfig.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  constructor(
    translateService: TranslateService,
    fhirConfigService: FhirConfigService
  ) {
    translateService.setDefaultLang('de');
    translateService.use(translateService.getBrowserLang());

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
