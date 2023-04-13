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

    let base = location.href;
    if (base.indexOf('#') > 0) {
      base = base.substring(0, base.indexOf('#') - 1) + '/fhir';
    } else {
      base = base + 'fhir';
    }
    console.log('note: using fhir base' + base);
    fhirConfigService.changeMagMicroService(base);
  }
}
