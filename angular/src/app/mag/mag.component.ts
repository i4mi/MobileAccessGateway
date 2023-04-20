import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FhirConfigService } from '../fhirConfig.service';
import { FormControl } from '@angular/forms';
import FhirClient from 'fhir-kit-client';
import { FhirPathService } from '../fhirpath.service';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { v4 as uuidv4 } from 'uuid';
import { Base64 } from 'js-base64';
import { MatTableDataSource } from '@angular/material/table';
import { AuthConfig, OAuthErrorEvent, OAuthService } from 'angular-oauth2-oidc';
import { Router } from '@angular/router';
import { getTokenSourceMapRange } from 'typescript';
import { IDroppedBlob } from '../upload/upload.component';
import { FhirResource } from 'fhir-kit-client';
import { UniqueSelectionDispatcher } from '@angular/cdk/collections';

// adapted from https://stackoverflow.com/a/17415677/16231610
const pad = (num: number) => String(Math.floor(Math.abs(num))).padStart(2, '0');
export const toLocaleDate = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
export const toLocaleHHMM = (date: Date) =>
  `${pad(date.getHours())}:${pad(date.getMinutes())}`;
export const toLocaleTime = (date: Date) =>
  `${toLocaleHHMM(date)}:${pad(date.getSeconds())}`;
export function toLocaleDateTime(date: Date) {
  const timeZoneOffset = -date.getTimezoneOffset();
  const sign = timeZoneOffset >= 0 ? '+' : '-';
  const timeZone = `${pad(timeZoneOffset / 60)}:${pad(timeZoneOffset % 60)}`;
  return `${toLocaleDate(date)}T${toLocaleTime(date)}${sign}${timeZone}`;
}

class UUIReplace {
  descr: string;
  existingUuid: string;
  newUuid: string;
}

@Component({
  selector: 'app-mag',
  templateUrl: './mag.component.html',
  styleUrls: ['./mag.component.scss'],
})
export class MagComponent implements OnInit {
  mag: FhirClient;
  json: string;
  doc: string;

  targetIdentifierValue: string;
  targetIdentifier2Value: string;

  xml: string;
  pdf: string;
  uploadBase64: string;
  uploadContentType: string;
  uploadBundle: fhir.r4.Bundle;

  targetId: string;

  public sourceIdentifierSystem: FormControl;
  public sourceIdentifierValue: FormControl;
  public sourceAddIdentifierSystem: FormControl;
  public sourceAddIdentifierValue: FormControl;
  public sourceManagingOrganizationOid: FormControl;
  public sourceManagingOrganizationName: FormControl;
  public targetIdentifierSystem: FormControl;
  public targetIdentifier2System: FormControl;
  public authenticate: FormControl;
  public documentType: FormControl;
  public documentConfidentiality: FormControl;
  public documentDescription: FormControl;
  public masterIdentifier: FormControl;
  public creationTime: FormControl;
  public languageCode: FormControl;
  public serviceStartFrom: FormControl;
  public serviceStartTo: FormControl;
  public serviceEndFrom: FormControl;
  public serviceEndTo: FormControl;

  public iheSourceId: FormControl;

  public searchGiven: FormControl;
  public searchGivenValue = '';
  public searchFamily: FormControl;
  public searchFamilyValue = '';
  public fhirConfigService: FhirConfigService;

  public replaceUuids = new Array<UUIReplace>();

  bundle: fhir.r4.Bundle;
  patient: fhir.r4.Patient;

  pageIndex = 0;
  dataSource = new MatTableDataSource<fhir.r4.DocumentReference>();
  length = 100;
  pageSize = 10;

  // oid mag = ahdis + .20 ->   2.16.756.5.30.1.145.20

  errMsg: string;
  errMsgAssignPatient: string;

  scopes: object;

  inMhdQueryProgress = false;
  inMhdUploadProgress = false;
  inPixmProgress = false;
  selectedDocumentReference: fhir.r4.DocumentReference;
  replaceDocumentReference: fhir.r4.DocumentReference;

  constructor(
    private data: FhirConfigService,
    private fhirPathService: FhirPathService,
    private http: HttpClient,
    private cd: ChangeDetectorRef,
    private oauthService: OAuthService,
    private router: Router
  ) {
    const oid_mag_ahdis = 'urn:oid:2.16.756.5.30.1.145.20';
    this.mag = data.getMobileAccessGatewayClient();
    this.mag
      .capabilityStatement()
      .then((data: fhir.r4.CapabilityStatement) =>
        this.setJson(JSON.stringify(data, null, 2))
      );
    this.sourceIdentifierSystem = new FormControl();
    this.sourceIdentifierSystem.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceIdentifierSystem',
        'urn:oid:2.16.756.5.30.1.196.3.2.1'
      )
    );
    this.sourceIdentifierValue = new FormControl();
    this.sourceIdentifierValue.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceIdentifierValue',
        'MAGMED001'
      )
    );
    this.sourceAddIdentifierSystem = new FormControl();
    this.sourceAddIdentifierSystem.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceAddIdentifierSystem',
        'urn:oid:2.16.756.5.30.1.196.3.2.1'
      )
    );
    this.sourceAddIdentifierValue = new FormControl();
    this.sourceAddIdentifierValue.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceAddIdentifierValue',
        'MAGMED001'
      )
    );
    this.sourceManagingOrganizationOid = new FormControl();
    this.sourceManagingOrganizationOid.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceManagingOrganizationOid',
        'urn:oid:1.3.6.1.4.1.21367.2017.2.7.109'
      )
    );
    this.sourceManagingOrganizationName = new FormControl();
    this.sourceManagingOrganizationName.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.sourceManagingOrganizationName',
        'TESTORG'
      )
    );
    this.targetIdentifierSystem = new FormControl();
    this.targetIdentifierSystem.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.targetIdentifierSystem',
        'urn:oid:2.16.756.5.30.1.191.1.0.2.1'
      )
    );
    this.targetIdentifier2System = new FormControl();
    this.targetIdentifier2System.setValue(
      this.getLocalStorageItemOrDefault(
        'mag.targetIdentifier2System',
        'urn:oid:2.16.756.5.30.1.127.3.10.3'
      )
    );
    this.serviceStartFrom = new FormControl();
    this.serviceStartFrom.setValue(
      this.getLocalStorageItemOrDefault('mag.serviceStartFrom', '')
    );
    this.serviceStartTo = new FormControl();
    this.serviceStartTo.setValue(
      this.getLocalStorageItemOrDefault('mag.serviceStartTo', '')
    );
    this.serviceEndFrom = new FormControl();
    this.serviceEndFrom.setValue(
      this.getLocalStorageItemOrDefault('mag.serviceEndFrom', '')
    );
    this.serviceEndTo = new FormControl();
    this.serviceEndTo.setValue(
      this.getLocalStorageItemOrDefault('mag.serviceEndTo', '')
    );
    this.authenticate = new FormControl();
    this.authenticate.setValue(
      this.getLocalStorageItemOrDefault('mag.authenticate', 'HCP')
    );
    this.documentType = new FormControl();
    this.documentType.setValue(
      this.getLocalStorageItemOrDefault('mag.documentType', 'APPC')
    );
    this.documentConfidentiality = new FormControl();
    this.documentConfidentiality.setValue(
      this.getLocalStorageItemOrDefault('mag.documentConfidentiality', 'NORM')
    );
    this.targetIdentifierValue = this.getLocalStorageItemOrDefault(
      'mag.targetIdentifierValue',
      ''
    );
    this.targetIdentifier2Value = this.getLocalStorageItemOrDefault(
      'mag.targetIdentifier2Value',
      ''
    );
    this.targetId = this.getLocalStorageItemOrDefault('mag.targetId', '');

    this.iheSourceId = new FormControl();
    this.iheSourceId.setValue(oid_mag_ahdis + '.1');

    this.searchGiven = new FormControl();
    this.searchFamily = new FormControl();
    this.documentDescription = new FormControl();
    this.documentDescription.setValue(
      this.getLocalStorageItemOrDefault('mag.documentType', 'Titel')
    );
    this.masterIdentifier = new FormControl();
    this.masterIdentifier.setValue(uuidv4());
    this.creationTime = new FormControl();
    this.creationTime.setValue(toLocaleDateTime(new Date()));

    this.languageCode = new FormControl();
    this.languageCode.setValue(
      this.getLocalStorageItemOrDefault('mag.languageCode', 'de-CH')
    );

    this.fhirConfigService = data;

    oauthService.configure(this.fhirConfigService.getAuthCodeFlowConfig());
    oauthService.tryLoginCodeFlow().then((_) => {
      this.scopes = this.oauthService.getGrantedScopes();
    });

    oauthService.events.subscribe((event) => {
      if (event instanceof OAuthErrorEvent) {
        console.error(event);
      } else {
        console.warn(event);
      }
    });

    this.pdf = '';
    this.replaceDocumentReference = null;
  }

  cache() {
    this.setLocaleStorageItem(
      'mag.sourceIdentifierSystem',
      this.sourceIdentifierSystem.value
    );
    this.setLocaleStorageItem(
      'mag.sourceIdentifierValue',
      this.sourceIdentifierValue.value
    );
    this.setLocaleStorageItem(
      'mag.targetIdentifierSystem',
      this.targetIdentifierSystem.value
    );
    this.setLocaleStorageItem(
      'mag.targetIdentifier2System',
      this.targetIdentifier2System.value
    );
    this.setLocaleStorageItem(
      'mag.targetIdentifierValue',
      this.targetIdentifierValue
    );
    this.setLocaleStorageItem(
      'mag.targetIdentifier2Value',
      this.targetIdentifier2Value
    );
    this.setLocaleStorageItem('mag.targetId', this.targetId);
    this.setLocaleStorageItem('mag.authenticate', this.authenticate.value);
    this.setLocaleStorageItem('mag.documentType', this.documentType.value);
    this.setLocaleStorageItem(
      'mag.documentConfidentiality',
      this.documentConfidentiality.value
    );
    this.setLocaleStorageItem(
      'mag.sourceAddIdentifierSystem',
      this.sourceAddIdentifierSystem.value
    );
    this.setLocaleStorageItem(
      'mag.sourceAddIdentifierValue',
      this.sourceAddIdentifierValue.value
    );
    this.setLocaleStorageItem(
      'mag.sourceManagingOrganizationOid',
      this.sourceManagingOrganizationOid.value
    );
    this.setLocaleStorageItem(
      'mag.sourceManagingOrganizationName',
      this.sourceManagingOrganizationName.value
    );
    this.setLocaleStorageItem(
      'mag.serviceStartFrom',
      this.serviceStartFrom.value
    );
    this.setLocaleStorageItem('mag.serviceStartTo', this.serviceStartTo.value);
    this.setLocaleStorageItem('mag.serviceEndFrom', this.serviceEndFrom.value);
    this.setLocaleStorageItem('mag.serviceEndTo', this.serviceEndTo.value);
    this.setLocaleStorageItem('mag.languageCode', this.languageCode.value);
  }

  getLocalStorageItemOrDefault(key: string, def: string): string {
    const val: string = localStorage.getItem(key);
    if (val) {
      return val;
    }
    return def;
  }

  setLocaleStorageItem(key: string, value: string): string {
    localStorage.setItem(key, value);
    return value;
  }

  setJson(result: string) {
    this.json = result;
  }
  getJson(): string {
    return this.json;
  }

  getScopes(): string {
    if (this.scopes) {
      return JSON.stringify(this.scopes, null, 2);
    }
    return '';
  }

  ngOnInit() {}

  ngOnDestroy() {}

  setPixmQueryResult(response: any) {
    this.inPixmProgress = false;
    this.setJson(JSON.stringify(response, null, 2));
    this.targetIdentifierValue = this.fhirPathService.evaluateToString(
      response,
      "parameter.valueIdentifier.where(system='" +
        this.targetIdentifierSystem.value +
        "').value"
    );
    this.targetIdentifier2Value = this.fhirPathService.evaluateToString(
      response,
      "parameter.valueIdentifier.where(system='" +
        this.targetIdentifier2System.value +
        "').value"
    );
    if (
      this.targetIdentifierValue != null &&
      this.targetIdentifier2Value == null &&
      'urn:oid:2.16.756.5.30.1.127.3.10.3' === this.sourceIdentifierSystem.value
    ) {
      // epr-spid special case, we know it already and copy it
      this.targetIdentifier2Value = this.sourceIdentifierValue.value;
    }
    this.targetId = this.fhirPathService.evaluateToString(
      response,
      'parameter.valueReference.reference'
    );
    this.cache();
  }

  setPatientFetchResult(response: any) {
    this.inPixmProgress = false;
    this.patient = response as fhir.r4.Patient;
    this.setJson(JSON.stringify(response, null, 2));
    this.cache();
  }

  setPIXmFeedAddResult(response: any) {
    this.inPixmProgress = false;
    this.patient = undefined;
    this.setJson(JSON.stringify(response, null, 2));
    this.cache();
  }

  onPIXmQuery() {
    this.targetIdentifierValue = '';
    this.targetIdentifier2Value = '';
    this.patient = undefined;
    this.inPixmProgress = true;
    this.cache();
    let queryParams =
      'sourceIdentifier=' +
      encodeURIComponent(
        this.sourceIdentifierSystem.value +
          '|' +
          this.sourceIdentifierValue.value
      ) +
      '&targetSystem=' +
      encodeURIComponent(this.targetIdentifierSystem.value) +
      '&targetSystem=' +
      encodeURIComponent(this.targetIdentifier2System.value);
    this.mag
      .operation({
        name: '$ihe-pix?' + queryParams,
        resourceType: 'Patient',
        method: 'GET',
        options: {
          headers: {
            accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
            'content-type':
              'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
          },
        },
      })
      .then((response) => this.setPixmQueryResult(response))
      .catch((error) => {
        this.setJson(JSON.stringify(error, null, 2));
        this.targetIdentifierValue = '';
        this.targetIdentifier2Value = '';
        this.targetId = '';
        this.cache();
        this.inPixmProgress = false;
      });
  }

  getPatient(): Promise<fhir.r4.Patient> {
    const patientId: string = this.targetId.substring(
      this.targetId.indexOf('Patient/') + 8
    );
    return this.mag.read({
      resourceType: 'Patient',
      id: patientId,
      options: {
        headers: {
          accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
          'content-type': 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
        },
      },
    });
  }

  async onFetchPatient() {
    this.patient = undefined;
    this.inPixmProgress = true;
    this.cache();
    try {
      this.patient = await this.getPatient();
      this.setPatientFetchResult(this.patient);
    } catch (error) {
      this.setJson(JSON.stringify(error, null, 2));
      this.cache();
      this.inPixmProgress = false;
    }
  }

  onPIXmFeedAdd() {
    this.inPixmProgress = true;
    // FIXME if fixed https://github.com/i4mi/MobileAccessGateway/issues/83
    if (this.patient.telecom) {
      delete this.patient.telecom;
    }
    // TODO: If we did not get the patient from PDQm we could justuse EPR SPID and MPI-ID and register thje local id
    this.cache();
    // query param gets double encoded on update if we use encodeURIComponent here to
    // http://localhost:9090/mag-pmp2/fhir/Patient?identifier=urn%253Aoid%253A2.16.756.5.30.1.196.3.2.1%257CHCI001
    // instead of
    // http://localhost:9090/mag-pmp2/fhir/Patient?identifier=urn%3Aoid%3A2.16.756.5.30.1.196.3.2.1%7CHCI001
    // let query = {
    //   identifier: encodeURIComponent(
    //     this.sourceAddIdentifierSystem.value +
    //       '|' +
    //       this.sourceAddIdentifierValue.value
    //   ),
    // };
    let query = {
      identifier:
        this.sourceAddIdentifierSystem.value +
        '|' +
        this.sourceAddIdentifierValue.value,
    };
    //    if (this.patient.managingOrganization === undefined) {
    //      this.patient.managingOrganization = {
    //        identifier: {
    //          system: this.sourceManagingOrganizationOid.value,
    //          value: this.sourceManagingOrganizationName.value,
    //        },
    //      };
    //    }
    let org = {
      resourceType: 'Organization',
      id: 'org',
      identifier: [
        {
          system: this.sourceManagingOrganizationOid.value,
        },
      ],
      name: this.sourceManagingOrganizationName.value,
    };
    if (this.patient.contained == null) {
      this.patient.contained = new Array<fhir.r4.Resource>();
    }
    this.patient.contained.push(org);
    this.patient.managingOrganization = {
      reference: '#org',
    };
    this.mag
      .update({
        resourceType: 'Patient',
        body: this.patient,
        searchParams: query,
        options: {
          headers: {
            accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
            'content-type':
              'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
          },
        },
      })
      .then((response) => this.setPIXmFeedAddResult(response))
      .catch((error) => {
        this.setJson(JSON.stringify(error, null, 2));
        this.cache();
        this.inPixmProgress = false;
      });
  }

  onAuthenticate() {
    this.cache();
    this.scopes = null;
    if (this.authenticate.value === 'HCP') {
      let authCodeFlowConfig = this.fhirConfigService.getAuthCodeFlowConfig();
      authCodeFlowConfig.scope = `person_id=${this.targetIdentifier2Value}^^^&2.16.756.5.30.1.127.3.10.3&ISO purpose_of_use=urn:oid:2.16.756.5.30.1.127.3.10.5|NORM subject_role=urn:oid:2.16.756.5.30.1.127.3.10.6|HCP`;
      this.oauthService.configure(authCodeFlowConfig);
      this.oauthService.initCodeFlow();
    } else {
      if (this.authenticate.value === 'TCU') {
        this.getSamlToken().then((value) => (this.json = value));
      } else {
        this.oauthService.logOut();
      }
    }
  }

  getAppcDocument(eprspid: string, uniqueId: string): string {
    // ${eprspid}
    let assertion = `<PolicySet PolicySetId="${uniqueId}" PolicyCombiningAlgId="urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides" xmlns="urn:oasis:names:tc:xacml:2.0:policy:schema:os" xmlns:ns2="urn:hl7-org:v3"><Description>Test policy set</Description><Target><Resources><Resource><ResourceMatch MatchId="urn:hl7-org:v3:function:II-equal"><AttributeValue DataType="urn:hl7-org:v3#II"><ns2:InstanceIdentifier root="2.16.756.5.30.1.127.3.10.3" extension="${eprspid}"/></AttributeValue><ResourceAttributeDesignator AttributeId="urn:e-health-suisse:2015:epr-spid" DataType="urn:hl7-org:v3#II"/></ResourceMatch></Resource></Resources></Target><PolicySet PolicySetId="urn:uuid:e06ccf2e-b9ce-4ab9-8fdc-2fb907711c1e" PolicyCombiningAlgId="urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides"><Description>description emergency</Description><Target><Subjects><Subject><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="HCP" codeSystem="2.16.756.5.30.1.127.3.10.6" displayName="Healthcare professional"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:2.0:subject:role" DataType="urn:hl7-org:v3#CV"/></SubjectMatch><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="EMER" codeSystem="2.16.756.5.30.1.127.3.10.5" displayName="Emergency Access"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse" DataType="urn:hl7-org:v3#CV"/></SubjectMatch></Subject></Subjects></Target><PolicySetIdReference>urn:e-health-suisse:2022:policies:pmp:emedication-access</PolicySetIdReference></PolicySet><PolicySet PolicySetId="urn:uuid:523df8bb-9847-4359-a075-0464998e6891" PolicyCombiningAlgId="urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides"><Description>description hcp</Description><Target><Subjects><Subject><SubjectMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal"><AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">7601002860123</AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:subject:subject-id" DataType="http://www.w3.org/2001/XMLSchema#string"/></SubjectMatch><SubjectMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal"><AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">urn:gs1:gln</AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:subject:subject-id-qualifier" DataType="http://www.w3.org/2001/XMLSchema#string"/></SubjectMatch><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="HCP" codeSystem="2.16.756.5.30.1.127.3.10.6" displayName="Healthcare professional"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:2.0:subject:role" DataType="urn:hl7-org:v3#CV"/></SubjectMatch><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="NORM" codeSystem="2.16.756.5.30.1.127.3.10.5" displayName="Normal Access"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse" DataType="urn:hl7-org:v3#CV"/></SubjectMatch></Subject></Subjects><Environments><Environment><EnvironmentMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:date-greater-than-or-equal"><AttributeValue DataType="http://www.w3.org/2001/XMLSchema#date">2027-02-03</AttributeValue><EnvironmentAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-date" DataType="http://www.w3.org/2001/XMLSchema#date"/></EnvironmentMatch></Environment></Environments></Target><PolicySetIdReference>urn:e-health-suisse:2015:policies:exclusion-list</PolicySetIdReference></PolicySet><PolicySet PolicySetId="urn:uuid:8b612672-0172-47fc-a177-ac7e2760d158" PolicyCombiningAlgId="urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides"><Description>description group</Description><Target><Subjects><Subject><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="HCP" codeSystem="2.16.756.5.30.1.127.3.10.6" displayName="Healthcare professional"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:2.0:subject:role" DataType="urn:hl7-org:v3#CV"/></SubjectMatch><SubjectMatch MatchId="urn:hl7-org:v3:function:CV-equal"><AttributeValue DataType="urn:hl7-org:v3#CV"><ns2:CodedValue code="NORM" codeSystem="2.16.756.5.30.1.127.3.10.5" displayName="Normal Access"/></AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse" DataType="urn:hl7-org:v3#CV"/></SubjectMatch><SubjectMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:anyURI-equal"><AttributeValue DataType="http://www.w3.org/2001/XMLSchema#anyURI">urn:oid:1.2.3</AttributeValue><SubjectAttributeDesignator AttributeId="urn:oasis:names:tc:xspa:1.0:subject:organization-id" DataType="http://www.w3.org/2001/XMLSchema#anyURI"/></SubjectMatch></Subject></Subjects><Environments><Environment><EnvironmentMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:date-less-than-or-equal"><AttributeValue DataType="http://www.w3.org/2001/XMLSchema#date">2032-01-01</AttributeValue><EnvironmentAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-date" DataType="http://www.w3.org/2001/XMLSchema#date"/></EnvironmentMatch></Environment></Environments></Target><PolicySetIdReference>urn:e-health-suisse:2022:policies:pmp:emedication-access</PolicySetIdReference></PolicySet></PolicySet>`;
    return assertion;
  }

  createAppc() {
    this.uploadBase64 = '';
    this.errMsgAssignPatient = '';
    if (this.targetIdentifier2Value == null) {
      this.errMsgAssignPatient = 'Error: select first  Patient with PIXm Query';
      return;
    }
    this.uploadContentType = 'text/xml';
    this.documentType.setValue('APPC');
    this.documentConfidentiality.setValue('NORM');
    this.masterIdentifier.setValue('urn:uuid:' + uuidv4());
    this.xml = this.getAppcDocument(
      this.targetIdentifier2Value,
      this.masterIdentifier.value
    );
    this.setJson(this.xml);
  }

  getSimulatedSamlPmpAssertion(date: string, eprspid: string): string {
    let assertion = `<saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ID="_2cfcc382-7e60-44e0-99b5-18e3f718cbc6" IssueInstant="${date}" Version="2.0" xsi:type="saml2:AssertionType"><saml2:Issuer>xua.hin.ch</saml2:Issuer><ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/><ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/><ds:Reference URI="#_2cfcc382-7e60-44e0-99b5-18e3f718cbc6"><ds:Transforms><ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/><ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"><ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="del xsd"/></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/><ds:DigestValue>UyqzdpLkYUMscBO0bEP6FwnKnlUscVCD70GL3uP6aSY=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>GFShEG4In1usnXJfapND3dvlNP9Nvw4MfuXHzauiKlqzfyGveiaoRvZMO3reKUw08ogOzEssNbOF            uDwITr5LsH1sHJg3q85fWPNHjXJvC3eup1fIKvTs7YzxXkdWruF2ZeDJ970PuPJPc59ljSOA+UFx Z8ZaRINNp6FJcU3Xkqs=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDjTCCAvagAwIBAgICAI0wDQYJKoZIhvcNAQENBQAwRTELMAkGA1UEBhMCQ0gxDDAKBgNVBAoM                    A0lIRTEoMCYGA1UEAwwfZWhlYWx0aHN1aXNzZS5paGUtZXVyb3BlLm5ldCBDQTAeFw0xOTA0MDEx                    MjQxMThaFw0yOTA0MDExMjQxMThaMCkxCzAJBgNVBAYTAkNIMQwwCgYDVQQKDANJSEUxDDAKBgNV                    BAMMA0lEUDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAhmmz4AEhbH+80Nf5QLjvP9/Cukcv                    rk5ONVZ9hQjz2OeBGBiW6TdKrwX6GIY0ue6zN6mRFuRycKi4A0aVWsO+s4ByQPsnnXx4JKXYGkoS                    ny7hgyxHxsEHiBZlMQLoqJ3jKYAR1SgLfdBEghAaDFEKF8hp8hwBMAp/WJY7eaZpXS0CAwEAAaOC                    AaYwggGiMB0GA1UdEQQWMBSCEmlkcC5paGUtZXVyb3BlLm5ldDBKBgNVHR8EQzBBMD+gPaA7hjlo                    dHRwczovL2VoZWFsdGhzdWlzc2UuaWhlLWV1cm9wZS5uZXQvZ3NzL2NybC8yMi9jYWNybC5jcmww                    SAYJYIZIAYb4QgEEBDsWOWh0dHBzOi8vZWhlYWx0aHN1aXNzZS5paGUtZXVyb3BlLm5ldC9nc3Mv                    Y3JsLzIyL2NhY3JsLmNybDBIBglghkgBhvhCAQMEOxY5aHR0cHM6Ly9laGVhbHRoc3Vpc3NlLmlo                    ZS1ldXJvcGUubmV0L2dzcy9jcmwvMjIvY2FjcmwuY3JsMAkGA1UdEwQCMAAwDgYDVR0PAQH/BAQD                    AgTwMBEGCWCGSAGG+EIBAQQEAwIF4DAdBgNVHQ4EFgQU4Kj/ojx2cO5W9/hOlSFUVh8jT1gwHwYD                    VR0jBBgwFoAUKJfv3d4xWGxW8oZG4hHkPjhxXy8wMwYDVR0lBCwwKgYIKwYBBQUHAwIGCCsGAQUF                    BwMEBgorBgEEAYI3FAICBggrBgEFBQcDATANBgkqhkiG9w0BAQ0FAAOBgQAvAQf3kRfC5hMAWFuK                    ZKV7fOLklivFoELOl96i9O29i5wCEeiClubfH9X7nnfvKukhWdi0MFkRZqgLRXN1iDY6iKC6MnZH                    TUN6qgskn6m3S0rsRXN8/My/EM+lmcFR1/IWhHtW+aERI0XoXR8GrY/QSmn3TWgHfO6qLdrUEfvV ew==</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent" NameQualifier="urn:e-health-suisse:2015:epr-spid">${eprspid}</saml2:NameID></saml2:Subject><saml2:Conditions NotBefore="2021-10-14T22:10:49.831Z" NotOnOrAfter="2025-10-14T22:15:49.831582Z"><saml2:AudienceRestriction><saml2:Audience>http://ihe.connectathon.XUA/X-ServiceProvider-IHE-Connectathon</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant="2018-03-28T09:02:43.155Z" SessionNotOnOrAfter="2018-03-28T09:12:43.154Z"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:subject-id" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:string">Iris Musterpatient</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:organization" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"/><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:organization-id" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"/><saml2:Attribute Name="urn:oasis:names:tc:xacml:2.0:subject:role" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue><Role xmlns="urn:hl7-org:v3" code="PAT" codeSystem="2.16.756.5.30.1.127.3.10.6" codeSystemName="ch-ehealth-codesystem-role" displayName="Patient" xsi:type="CE"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue><PurposeOfUse xmlns="urn:hl7-org:v3" code="NORM" codeSystem="2.16.756.5.30.1.127.3.10.5" codeSystemName="ch-ehealth-codesystem-purposeOfUse" displayName="Normal Access" xsi:type="CE"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xacml:2.0:resource:resource-id" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:string">${eprspid}^^^&amp;2.16.756.5.30.1.127.3.10.3&amp;ISO</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:ihe:iti:xca:2010:homeCommunityId" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:anyURI">urn:oid:3.3.3.1</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>`;
    return Base64.encode(assertion);
  }

  getSimulatedSamlPmpTcuAssertion(
    date: string,
    datevalid: string,
    eprspid: string
  ): String {
    let assertion = `<saml2:Assertion ID="_2cfcc382-7e60-44e0-99b5-18e3f718cbc6" IssueInstant="${date}" Version="2.0" xmlns:del="urn:oasis:names:tc:SAML:2.0:conditions:delegation" xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="saml2:AssertionType"><saml2:Issuer>xua.hin.ch</saml2:Issuer><ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/><ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/><ds:Reference URI="#_2cfcc382-7e60-44e0-99b5-18e3f718cbc6"><ds:Transforms><ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/><ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"><ec:InclusiveNamespaces PrefixList="del xsd" xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"/></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/><ds:DigestValue>UyqzdpLkYUMscBO0bEP6FwnKnlUscVCD70GL3uP6aSY=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>KedJuTob5gtvYx9qM3k3gm7kbLBwVbEQRl26S2tmXjqNND7MRGtoew==</ds:SignatureValue></ds:Signature><saml2:Subject><saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent" NameQualifier="urn:gs1:gln">2000040030829</saml2:NameID><saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer"><saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent" NameQualifier="urn:e-health-suisse:technical-user-id">urn:oid:1.3.6.1.4.1.343</saml2:NameID></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore="${date}" NotOnOrAfter="${datevalid}"><saml2:AudienceRestriction><saml2:Audience>urn:e-health-suisse:token-audience:all-communities</saml2:Audience></saml2:AudienceRestriction><saml2:Condition xsi:type="del:DelegationRestrictionType"><del:Delegate><saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent" NameQualifier="urn:e-health-suisse:technical-user-id">urn:oid:1.3.6.1.4.1.343</saml2:NameID></del:Delegate></saml2:Condition></saml2:Conditions><saml2:AuthnStatement AuthnInstant="2018-03-28T09:02:43.155Z" SessionNotOnOrAfter="2018-03-28T09:12:43.154Z"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name="urn:oasis:names:tc:xacml:2.0:subject:role" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue><Role code="HCP" codeSystem="2.16.756.5.30.1.127.3.10.6" codeSystemName="ch-ehealth-codesystem-role" displayName="Healthcare professional" xmlns="urn:hl7-org:v3" xsi:type="CE"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue><PurposeOfUse code="AUTO" codeSystem="2.16.756.5.30.1.127.3.10.5" codeSystemName="ch-ehealth-codesystem-purposeOfUse" displayName="Automatic Upload" xmlns="urn:hl7-org:v3" xsi:type="CE"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xacml:2.0:resource:resource-id" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:string">${eprspid}^^^&amp;2.16.756.5.30.1.127.3.10.3&amp;ISO</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:subject-id" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:string">Oliver Egger</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:organization"><saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xsd:string">Centre hospitalier universitaire vaudois</saml2:AttributeValue><saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xsd:string">ahdis ag</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:oasis:names:tc:xspa:1.0:subject:organization-id"><saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xsd:anyURI">urn:oid:1.1.44.17.205</saml2:AttributeValue><saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xsd:anyURI">urn:oid:2.16.756.5.30.1.145</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name="urn:ihe:iti:xca:2010:homeCommunityId" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"><saml2:AttributeValue xsi:type="xsd:anyURI">urn:oid:2.16.756.5.30.1.191.1.0</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>`;
    return Base64.encode(assertion);
  }

  async getEprikTcuAssertion(eprspid: string): Promise<string> {
    const tcuRequest = await fetch(
      'https://test.ahdis.ch/eprik-cara/camel/tcu',
      {
        cache: 'no-store',
        headers: {
          Accept: 'application/xml',
        },
      }
    );
    const saml2tcu = await tcuRequest.text();
    const xua = await fetch(
      this.fhirConfigService.getMobileAccessGatewayAssertionEndpoint(),
      {
        cache: 'no-store',
        method: 'POST',
        headers: {
          Scope: `person_id=${eprspid}^^^&2.16.756.5.30.1.127.3.10.3&ISO purpose_of_use=urn:oid:2.16.756.5.30.1.127.3.10.5|AUTO subject_role=urn:oid:2.16.756.5.30.1.127.3.10.6|TCU principal_id=2000040030829 principal=Oliver+Egger`,
          Accept: 'application/json;charset=UTF-8',
          'Content-Type': 'application/xml;charset=UTF-8',
        },
        body: saml2tcu,
      }
    );
    const jsonXua = await xua.json();
    return Promise.resolve(jsonXua.access_token);
  }

  setBundle(bundle: fhir.r4.Bundle) {
    if (bundle) {
      this.bundle = <fhir.r4.Bundle>bundle;
      this.length = this.bundle.total;
      this.dataSource.data = this.bundle.entry.map(
        (entry) => entry.resource as fhir.r4.DocumentReference
      );
    } else {
      this.dataSource.data = null;
    }
    this.dataSource.data = this.dataSource.data; // https://stackoverflow.com/questions/46746598/angular-material-how-to-refresh-a-data-source-mat-table
  }

  setDocumentReferenceResult(response: fhir.r4.Bundle) {
    this.setJson(JSON.stringify(response, null, 2));
    this.setBundle(response);
  }

  async findDocumentReferences(): Promise<fhir.r4.Bundle> {
    let query = {
      status: 'current',
      'patient.identifier': encodeURIComponent(
        this.targetIdentifierSystem.value + '|' + this.targetIdentifierValue
      ),
    };
    let samltoken = await this.getSamlToken();
    return this.mag.search({
      resourceType: 'DocumentReference',
      searchParams: query,
      options: {
        headers: {
          accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
          Authorization: 'Bearer ' + samltoken,
        },
      },
    }) as Promise<fhir.r4.Bundle>;
  }

  async onFindDocumentReferences() {
    this.replaceDocumentReference = null;
    this.inMhdQueryProgress = true;
    this.pdf = '';
    try {
      const bundle = await this.findDocumentReferences();
      this.setDocumentReferenceResult(bundle as fhir.r4.Bundle);
      this.inMhdQueryProgress = false;
    } catch (error) {
      this.setJson(JSON.stringify(error, null, 2));
      this.setBundle(null);
      this.inMhdQueryProgress = false;
    }
  }

  async findMedicationList(format?: string): Promise<fhir.r4.Bundle> {
    let queryParams =
      'patient.identifier=' +
      encodeURIComponent(
        this.targetIdentifierSystem.value + '|' + this.targetIdentifierValue
      ) +
      (format ? '&format=' + encodeURIComponent(format) : '');
    if (this.serviceStartFrom.value?.length > 0) {
      queryParams += `&serviceStartFrom=${encodeURIComponent(
        this.serviceStartFrom.value
      )}`;
    }
    if (this.serviceStartTo.value?.length > 0) {
      queryParams += `&serviceStartTo=${encodeURIComponent(
        this.serviceStartTo.value
      )}`;
    }
    if (this.serviceEndFrom.value?.length > 0) {
      queryParams += `&serviceEndFrom=${encodeURIComponent(
        this.serviceEndFrom.value
      )}`;
    }
    if (this.serviceEndTo.value?.length > 0) {
      queryParams += `&serviceEndTo=${encodeURIComponent(
        this.serviceEndTo.value
      )}`;
    }

    const saml = await this.getSamlToken();

    return this.mag.operation({
      name: '$find-medication-list?status=current&' + queryParams,
      resourceType: 'DocumentReference',
      method: 'GET',
      options: {
        headers: {
          accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
          Authorization: 'Bearer ' + saml,
        },
      },
    });
  }

  async onFindMedicationList() {
    this.pdf = '';
    this.inMhdQueryProgress = true;
    this.cache();
    try {
      const bundle = await this.findMedicationList();
      this.setDocumentReferenceResult(bundle);
      if (bundle.entry && bundle.entry.length == 1) {
        await this.downloadDocumentReferenceAttachment(
          bundle.entry[0].resource as fhir.r4.DocumentReference
        );
      }
      this.inMhdQueryProgress = false;
    } catch (error) {
      this.setJson(JSON.stringify(error, null, 2));
      this.setBundle(null);
      this.inMhdQueryProgress = false;
    }
  }

  async onFindMedicationCard() {
    this.pdf = '';
    this.inMhdQueryProgress = true;
    this.cache();
    try {
      const bundle = await this.findMedicationList(
        'urn:oid:2.16.756.5.30.1.127.3.10.10|urn:che:epr:ch-emed:medication-card:2022'
      );
      this.setDocumentReferenceResult(bundle);
      if (bundle.entry && bundle.entry.length == 1) {
        const medcard: string = (await this.downloadDocumentReferenceAttachment(
          bundle.entry[0].resource as fhir.r4.DocumentReference
        )) as string;
        const medcardjson = JSON.parse(medcard);
        this.pdf = this.fhirPathService.evaluateToString(
          medcardjson,
          "entry.resource.where(resourceType='Binary').data"
        );
      }
      this.inMhdQueryProgress = false;
    } catch (error) {
      this.setJson(JSON.stringify(error, null, 2));
      this.setBundle(null);
      this.inMhdQueryProgress = false;
    }
  }

  // temporary fix because we cannot generate the assertion ourselves yet
  async getSamlToken(): Promise<string> {
    if (this.authenticate.value === 'HCP') {
      return Promise.resolve(this.oauthService.getAccessToken());
    }
    if (this.authenticate.value === 'Patient') {
      return Promise.resolve(
        this.getSimulatedSamlPmpAssertion(
          toLocaleDateTime(new Date()),
          this.targetIdentifier2Value
        )
      );
    }
    if (this.authenticate.value === 'TCU') {
      //      const now = new Date();
      //      const expired = new Date(now.getTime() + 5 * 60000);
      //      return this.getSimulatedSamlPmpTcuAssertion(
      //        toLocaleDateTime(now),
      //        toLocaleDateTime(expired),
      //        this.targetIdentifier2Value
      //    );
      return this.getEprikTcuAssertion(this.targetIdentifier2Value);
    }
    return null;
  }

  async downloadDocumentReferenceAttachment(
    entry: fhir.r4.DocumentReference
  ): Promise<string | ArrayBuffer> {
    const url =
      entry.content && entry.content.length > 0
        ? entry.content[0].attachment.url
        : 'undefined';
    let completeUrl = url.replace(
      'http://test.ahdis.ch/mag-pmp/camel/xdsretrieve',
      'https://test.ahdis.ch/mag-pmp/camel/xdsretrieve'
    );
    let completeUrl2 = completeUrl.replace(
      'http://test.ahdis.ch/mag-pmp2/camel/xdsretrieve',
      'https://test.ahdis.ch/mag-pmp2/camel/xdsretrieve'
    );
    const contentType =
      entry.content && entry.content.length > 0
        ? entry.content[0].attachment?.contentType
        : '';
    this.selectedDocumentReference = entry;
    if ('application/pdf' === contentType) {
      const title =
        entry.content && entry.content.length > 0
          ? entry.content[0].attachment.title
          : 'undefined';
      const that = this;
      const saml = await this.getSamlToken();
      const res = await fetch(completeUrl2, {
        cache: 'no-store',
        headers: {
          Authorization: 'Bearer ' + saml,
          Accept: 'application/pdf',
        },
      });
      const blob = await res.blob();
      return new Promise((resolve, reject) => {
        let reader = new FileReader();
        reader.onload = () => {
          that.downloadPdf(reader.result.toString(), title);
          resolve(reader.result);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      });
    } else {
      const saml = await this.getSamlToken();
      const headers = new HttpHeaders().set('Authorization', 'Bearer ' + saml);
      const options = {
        responseType: 'text' as const,
        headers: headers,
      };
      return new Promise((resolve, reject) => {
        this.http.get(completeUrl, options).subscribe({
          next: (body: string) => {
            (this.xml = body), this.setJson(body);
            resolve(this.xml);
          },
          error: (err: Error) => {
            reject(err);
          },
        });
      });
    }
  }

  async onReplace(entryOrig: fhir.r4.DocumentReference) {
    this.replaceDocumentReference = entryOrig;
  }

  async onRemove(entryOrig: fhir.r4.DocumentReference) {
    this.replaceDocumentReference = null;
    let entry = { ...entryOrig };
    this.inMhdQueryProgress = true;

    if (entry.extension == null) {
      entry.extension = new Array<fhir.r4.Extension>();
    }

    entry.extension.push({
      url: 'http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId',
      valueIdentifier: {
        value: this.iheSourceId.value,
      },
    });

    entry.extension.push({
      url:
        'http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType',
      valueCodeableConcept: {
        coding: [
          {
            system: 'http://snomed.info/sct',
            code: '71388002',
            display: 'Procedure (procedure)',
          },
        ],
        text: 'Procedure (procedure)',
      },
    });

    entry.extension.push({
      url:
        'http://fhir.ch/ig/ch-epr-mhealth/StructureDefinition/ch-ext-deletionstatus',
      valueCoding: {
        system:
          'http://fhir.ch/ig/ch-epr-mhealth/CodeSysteme/ch-ehealth-codesystem-deletionstatus',
        code: 'deletionRequested',
      },
    });

    entry.extension.push({
      url:
        'http://fhir.ch/ig/ch-epr-mhealth/StructureDefinition/ch-ext-author-authorrole',
      valueCoding: {
        system: 'urn:oid:2.16.756.5.30.1.127.3.10.6',
        code: 'HCP',
        display: 'Healthcare professional',
      },
    });

    const saml = await this.getSamlToken();

    try {
      let response = await this.mag.update({
        resourceType: 'DocumentReference',
        id: entry.id,
        body: entry,
        options: {
          headers: {
            accept: 'application/fhir+json;fhirVersion=4.0',
            'content-type': 'application/fhir+json;fhirVersion=4.0',
            Authorization: 'Bearer ' + saml,
          },
        },
      });
      this.setJson(JSON.stringify(response, null, 2));
      this.inMhdQueryProgress = false;
      this.onFindDocumentReferences();
    } catch (error) {
      this.setJson(JSON.stringify(error.response.data, null, 2));
      this.inMhdQueryProgress = false;
    }
  }

  async onDownloadDocumentReferenceAttachment(
    entry: fhir.r4.DocumentReference
  ) {
    this.replaceDocumentReference = null;
    this.inMhdQueryProgress = true;
    await this.downloadDocumentReferenceAttachment(entry);
    this.inMhdQueryProgress = false;
  }

  canTransformToPdf(): boolean {
    return this.pdf != '';
  }

  downloadPdf(base64String: string, fileName: string) {
    const link = document.createElement('a');
    link.href = base64String;
    link.download = `${fileName}.pdf`;
    link.click();
  }

  onPdf() {
    const title =
      this.selectedDocumentReference.content &&
      this.selectedDocumentReference.content.length > 0
        ? this.selectedDocumentReference.content[0].attachment.title
        : 'undefined';
    const pdfdata = `data:application/pdf;base64,${this.pdf}`;
    this.downloadPdf(pdfdata, title);
  }

  generateOidFromUuid(): string {
    let guid = uuidv4();
    let guidBytes = `0x${guid.replace(/-/g, '')}`;
    var bigInteger = BigInt(guidBytes);
    return `2.25.${bigInteger.toString()}`;
  }

  getDocumentReferenceType(): fhir.r4.CodeableConcept {
    switch (this.documentType.value) {
      case 'APPC':
      case 'MTP':
      case 'PADV':
      case 'DIS':
      case 'PDF':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '419891008',
              display: 'Record artifact',
            },
          ],
        };
      case 'PRE':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '761938008',
              display: 'Medicinal Prescription record (record artifact)',
            },
          ],
        };
    }
    return null;
  }

  getDocumentConfidentiality(): fhir.r4.CodeableConcept {
    switch (this.documentConfidentiality.value) {
      case 'NORM':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '17621005',
              display: 'Normally accessible',
            },
          ],
        };
      case 'RESTRICTED':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '263856008',
              display: 'Restricted accessible',
            },
          ],
        };
      case 'SECRET':
        return {
          coding: [
            {
              system: 'urn:oid:2.16.756.5.30.1.127.3.4',
              code: '1141000195107',
              display: 'Secret',
            },
          ],
        };
    }
    return null;
  }

  getDocumentReferenceCategory(): fhir.r4.CodeableConcept {
    // classCode
    switch (this.documentType.value) {
      case 'APPC':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '371537001',
              display: 'Consent report (record artifact)',
            },
          ],
        };
      case 'MTP':
      case 'DIS':
      case 'PRE':
      case 'PADV':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '440545006',
              display: 'Prescription record',
            },
          ],
        };
      case 'PDF':
        return {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '405624007',
              display: 'Administrative documentation',
            },
          ],
        };
    }
    return null;
  }

  getDocumentReferenceContentFormat(): fhir.r4.Coding {
    switch (this.documentType.value) {
      case 'APPC':
        return {
          system: 'urn:oid:1.3.6.1.4.1.19376.1.2.3',
          code: 'urn:ihe:iti:appc:2016:consent',
          display: 'Advanced Patient Privacy Consents',
        };
      case 'MTP':
        return {
          system: 'urn:oid:2.16.756.5.30.1.127.3.10.10',
          code: 'urn:che:epr:ch-emed:mtp:2022',
        };
      case 'DIS':
        return {
          system: 'urn:oid:2.16.756.5.30.1.127.3.10.10',
          code: 'urn:che:epr:ch-emed:dis:2022',
        };
      case 'PRE':
        return {
          system: 'urn:oid:2.16.756.5.30.1.127.3.10.10',
          code: 'urn:che:epr:ch-emed:pre:2022',
        };
      case 'PADV':
        return {
          system: 'urn:oid:2.16.756.5.30.1.127.3.10.10',
          code: 'urn:che:epr:ch-emed:padv:2022',
        };
      case 'PDF':
        return {
          system: 'urn:oid:2.16.756.5.30.1.127.3.10.10',
          code: 'urn:che:epr:EPR_Unstructured_Document',
        };
    }
    return null;
  }

  addFile(droppedBlob: IDroppedBlob) {
    this.uploadBase64 = '';
    this.uploadContentType = '';
    this.uploadBundle = null;
    this.json = '';
    this.xml = '';
    this.pdf = '';

    if (
      droppedBlob.contentType === 'application/json' ||
      droppedBlob.name.endsWith('.json') ||
      droppedBlob.contentType === 'text/xml' ||
      droppedBlob.name.endsWith('.xml')
    ) {
      this.addText(droppedBlob);
    } else {
      var reader = new FileReader();
      const that = this;
      reader.readAsDataURL(droppedBlob.blob);
      reader.onloadend = function () {
        let result = reader.result.toString();
        if (result.startsWith('data:application/pdf;base64,')) {
          that.uploadBase64 = result.substring(
            'data:application/pdf;base64,'.length
          );
          that.uploadContentType = 'application/pdf';
          that.documentType.setValue('PDF');
        }
      };
    }
  }

  addText(blob: IDroppedBlob) {
    const reader = new FileReader();
    reader.readAsText(blob.blob);
    const that = this;
    this.documentDescription.setValue(blob.name);
    this.creationTime.setValue(toLocaleDateTime(new Date()));
    reader.onload = () => {
      if (
        blob.contentType === 'application/json' ||
        blob.name.endsWith('.json')
      ) {
        this.uploadContentType = 'application/fhir+json';
        this.autoDetectFormat(<string>reader.result);
      }
      if (blob.contentType === 'text/xml' || blob.name.endsWith('.xml')) {
        this.uploadContentType = 'text/xml';
        that.documentType.setValue('XML');
        that.xml = <string>reader.result;
        that.setJson(that.xml);
      }
    };
  }

  getResource(bundle: fhir.r4.Bundle, reference: string): fhir.r4.Resource {
    return bundle.entry.filter((be) => be.fullUrl.endsWith(reference)).pop()
      ?.resource;
  }

  autoDetectFormat(jsonString: string) {
    this.documentType.setValue('JSON');
    const json = JSON.parse(jsonString);
    this.setJson(jsonString);

    if (json.hasOwnProperty('resourceType')) {
      const res = json as fhir.r4.Resource;
      if ('Bundle' === res.resourceType) {
        const bundle = json as fhir.r4.Bundle;
        this.autoDetectBundle(bundle);
      }
    }
  }

  autoDetectBundle(bundle: fhir.r4.Bundle) {
    this.creationTime.setValue(bundle.timestamp);
    this.masterIdentifier.setValue(bundle.identifier.value);
    if ('document' === bundle.type && bundle.entry?.length > 0) {
      const composition = bundle.entry[0].resource as fhir.r4.Composition;
      const snomedct = composition.type.coding.find(
        (coding) => 'http://snomed.info/sct' === coding.system
      );
      const loinc = composition.type.coding.find(
        (coding) => 'http://loinc.org' === coding.system
      );
      if (composition.language) {
        this.languageCode.setValue(composition.language);
      }
      if (
        loinc &&
        '77603-9' === loinc.code &&
        snomedct &&
        '419891008' === snomedct.code
      ) {
        this.uploadBundle = bundle;
        this.documentType.setValue('MTP');
        return;
      }
      if (
        loinc &&
        '57833-6' === loinc.code &&
        snomedct &&
        '761938008' === snomedct.code
      ) {
        this.uploadBundle = bundle;
        this.documentType.setValue('PRE');
        return;
      }
      if (
        loinc &&
        '60593-1' === loinc.code &&
        snomedct &&
        '419891008' === snomedct.code
      ) {
        this.uploadBundle = bundle;
        this.documentType.setValue('DIS');
        return;
      }
      if (
        loinc &&
        '61356-2' === loinc.code &&
        snomedct &&
        '419891008' === snomedct.code
      ) {
        this.uploadBundle = bundle;
        this.documentType.setValue('PADV');
        return;
      }
    }
  }

  async assignMobileAccessPatient() {
    this.inMhdUploadProgress = true;
    this.errMsgAssignPatient = '';

    if (this.patient == null) {
      this.patient = await this.getPatient();
    }

    if (this.patient == null) {
      this.errMsgAssignPatient =
        "Error: select first 'get Patient' from Mobile Access Gateway";
      return;
    }
    if (this.uploadBundle == null) {
      this.errMsgAssignPatient =
        'Error: need a FHIR Bundle in json format to assign the Patient';
      return;
    }
    const patientEntries = this.uploadBundle.entry.filter(
      (entry) => 'Patient' === entry.resource.resourceType
    );

    // we keep only the local patient identifier
    const patientCopy: fhir.r4.Patient = { ...this.patient };
    patientCopy.identifier = this.patient.identifier.filter(
      (identifier) => identifier.system === this.sourceIdentifierSystem.value
    );
    patientCopy.identifier.find(
      (identifier) => identifier.system === this.sourceIdentifierSystem.value
    ).type = {
      coding: [
        {
          system: 'http://terminology.hl7.org/CodeSystem/v2-0203',
          code: 'MR',
        },
      ],
    };

    // we have sometime multiple names in cara test system, we reduce it to the first one
    patientCopy.name = Array<fhir.r4.HumanName>();
    patientCopy.name[0] = this.patient.name[0];

    patientEntries.forEach((patientEntry) => {
      const id = patientEntry.resource.id;
      patientEntry.resource = { ...patientCopy };
      patientEntry.resource.id = id;
    });

    let jsonString = JSON.stringify(this.uploadBundle, null, 2);

    // create a new uuid and replace all occurences of it in the document, store if for later referring uuid's
    const existingUuid = this.uploadBundle.identifier.value;
    const newUuid = 'urn:uuid:' + uuidv4();

    const index = this.replaceUuids.findIndex(
      (entry) => entry.existingUuid == existingUuid
    );
    if (index == -1) {
      this.replaceUuids.push({
        descr: this.documentType.value,
        existingUuid,
        newUuid,
      });
    } else {
      this.replaceUuids[index] = {
        descr: this.documentType.value,
        existingUuid,
        newUuid,
      };
    }
    this.replaceUuids.forEach(
      (entry) =>
        (jsonString = jsonString.split(entry.existingUuid).join(entry.newUuid))
    );

    this.masterIdentifier.setValue(newUuid);

    this.setJson(jsonString);
    this.inMhdUploadProgress = false;
  }

  async createMhdTransaction() {
    this.inMhdUploadProgress = true;
    let bundleTransaction: fhir.r4.Bundle = {
      resourceType: 'Bundle',
      meta: {
        profile: [
          'http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle',
        ],
      },
      type: 'transaction',
      entry: [
        {
          fullUrl: '$1',
          resource: {
            resourceType: 'Binary',
            contentType: '$1.2',
            data: '$2',
          },
          request: {
            method: 'POST',
            url: 'Binary',
          },
        },
        {
          fullUrl: '$3',
          resource: {
            resourceType: 'List',
            extension: [],
            identifier: [
              {
                use: 'official',
                system: 'urn:ietf:rfc:3986',
                value: '$5',
              },
              {
                use: 'usual',
                system: 'urn:ietf:rfc:3986',
                value: '$6',
              },
            ],
            status: 'current',
            mode: 'working',
            code: {
              coding: [
                {
                  system:
                    'http://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes',
                  code: 'submissionset',
                  display: 'SubmissionSet as a FHIR List',
                },
              ],
            },
            subject: {
              reference: '$7',
            },
            date: '$8',
            entry: [
              {
                item: {
                  reference: '#9',
                },
              },
            ],
          },
          request: {
            method: 'POST',
            url: 'List',
          },
        },
        {
          fullUrl: '#9',
          resource: {
            resourceType: 'DocumentReference',
            contained: [
              {
                resourceType: 'Patient',
                id: '1',
                identifier: [
                  {
                    system: 'urn:oid:2.16.756.5.30.1.191.1.0.12.3.101',
                    value: '$10',
                  },
                  {
                    system: 'urn:oid:2.16.756.5.30.1.191.1.0.2.1',
                    value: '$11',
                  },
                ],
              },
            ],
            extension: [],
            masterIdentifier: {
              value: '$12',
            },
            identifier: [
              {
                use: 'usual',
                system: 'urn:ietf:rfc:3986',
                value: '$14',
              },
            ],
            status: 'current',
            category: [],
            subject: {
              reference: '#7',
            },
            relatesTo: [],
            author: [],
            date: '$8',
            description: 'Upload',
            securityLabel: [],
            content: [
              {
                attachment: {
                  contentType: '$1.2',
                  language: '$1.3',
                  url: '$1',
                  creation: '$8',
                },
              },
            ],
            context: {
              facilityType: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '264358009',
                    display: 'General practice premises(environment)',
                  },
                ],
              },
              practiceSetting: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '394802001',
                    display: 'General medicine(qualifier value)',
                  },
                ],
              },
              sourcePatientInfo: {
                reference: '#1',
              },
            },
          },
          request: {
            method: 'POST',
            url: 'DocumentReference',
          },
        },
      ],
    };

    // Binary
    let fullUrlBinary = 'urn:uuid:' + uuidv4();

    bundleTransaction.entry[0].fullUrl = fullUrlBinary; // $1
    //    bundle.entry[0].resource.data = Base64.encode(this.upload); // $2

    const binary: fhir.r4.Binary = bundleTransaction.entry[0]
      .resource as fhir.r4.Binary;
    binary.contentType = this.uploadContentType; // $1.2

    if (this.uploadBase64?.length > 0) {
      binary.data = this.uploadBase64; // $2
    } else {
      binary.data = Base64.encode(this.json);
    }

    // List
    let uuid3 = uuidv4();
    bundleTransaction.entry[1].fullUrl = 'urn:uid:' + uuid3; // $3
    const list: fhir.r4.List = bundleTransaction.entry[1]
      .resource as fhir.r4.List;

    // $4 http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId
    list.extension.push({
      url: 'http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId',
      valueIdentifier: {
        value: this.iheSourceId.value,
      },
    });
    list.extension.push({
      url:
        'http://fhir.ch/ig/ch-epr-mhealth/StructureDefinition/ch-ext-author-authorrole',
      valueCoding: {
        system: 'urn:oid:2.16.756.5.30.1.127.3.10.6',
        code: 'HCP',
        display: 'Healthcare professional',
      },
    });
    list.extension.push({
      url:
        'http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType',
      valueCodeableConcept: {
        coding: [
          {
            system: 'http://snomed.info/sct',
            code: '71388002',
            display: 'Procedure (procedure)',
          },
        ],
        text: 'Procedure (procedure)',
      },
    });

    let listUniqueId = 'urn:uuid:' + uuidv4();
    list.identifier[0].value = listUniqueId; // $5 identifier.official uniqueId

    let listEntryUuid = 'urn:oid:' + this.generateOidFromUuid();

    list.identifier[1].value = listEntryUuid; // $6 identifier.usual
    list.subject.reference = this.targetId; // $7 https://test.ahdis.ch/mag-pmp/fhir/Patient/2.16.756.5.30.1.191.1.0.2.1-713d79be-058e-4f55-82a8-e1f81f5e0047

    let currentDateTime = toLocaleDateTime(new Date());
    list.date = currentDateTime; // $8 2011-11-29T11:00:00+01:00

    let fullUrlDocumentReference = 'urn:uuid:' + uuidv4();
    list.entry[0].item.reference = fullUrlDocumentReference; // $9 urn:uuid:537f1c0f-6adc-48b2-b7f9-141f7e639972

    // DocumentReference
    bundleTransaction.entry[2].fullUrl = fullUrlDocumentReference; // $9
    const docref: fhir.r4.DocumentReference = bundleTransaction.entry[2]
      .resource as fhir.r4.DocumentReference;

    docref.extension.push({
      url:
        'http://fhir.ch/ig/ch-epr-mhealth/StructureDefinition/ch-ext-author-authorrole',
      valueCoding: {
        system: 'urn:oid:2.16.756.5.30.1.127.3.10.6',
        code: 'HCP',
        display: 'Healthcare professional',
      },
    });

    // if we replace a previous document we need to add the relationship
    // need to update to fromat https://profiles.ihe.net/ITI/MHD/Bundle-ex-comprehensiveProvideDocumentBundleReplace.json.html
    if (this.replaceDocumentReference != null) {
      docref.relatesTo.push({
        code: 'replaces',
        target: {
          reference: this.replaceDocumentReference.identifier[0].value,
        },
      });
    } else {
      delete docref.relatesTo;
    }

    const docrefpat: fhir.r4.Patient = docref.contained[0] as fhir.r4.Patient;

    docrefpat.identifier[0].value = this.sourceIdentifierSystem.value;
    docrefpat.identifier[0].value = this.sourceIdentifierValue.value; // $10

    docrefpat.identifier[1].value = this.targetIdentifierSystem.value;
    docrefpat.identifier[1].value = this.targetIdentifierValue; // $11

    // if we feteched the patient before we also set the names to it
    if (this.patient) {
      docrefpat.birthDate = this.patient.birthDate;
      docrefpat.gender = this.patient.gender;
      docrefpat.name = this.patient.name;
      docrefpat.address = this.patient.address;
    }

    if (
      this.uploadBundle != null &&
      ['PADV', 'DIS', 'MTP', 'PRE', 'PMLC'].includes(this.documentType.value)
    ) {
      const composition = this.uploadBundle.entry[0]
        .resource as fhir.r4.Composition;
      if (composition?.author) {
        if (composition.author[0].reference) {
          const practRole = this.getResource(
            this.uploadBundle,
            composition.author[0].reference
          );
          if (practRole && practRole.resourceType == 'PractitionerRole') {
            let practitionerRole = { ...practRole } as fhir.r4.PractitionerRole;
            delete practitionerRole.text;
            practitionerRole.id = 'practrole';
            const organization = this.getResource(
              this.uploadBundle,
              practitionerRole.organization?.reference
            );
            const practitioner = this.getResource(
              this.uploadBundle,
              practitionerRole.practitioner?.reference
            );
            if (organization) {
              let org = { ...organization } as fhir.r4.Organization;
              org.id = 'org';
              delete org.text;
              practitionerRole.organization.reference = '#org';
              docref.contained.push(org);
            }
            if (practitioner) {
              let pract = { ...practitioner } as fhir.r4.Practitioner;
              pract.id = 'pract';
              delete pract.text;
              practitionerRole.practitioner.reference = '#pract';
              docref.contained.push(pract);
            }
            docref.contained.push(practitionerRole);
            docref.author.push({ reference: '#practrole' });
          }
        }
      }
    }
    if (docref.author.length == 0) {
      delete docref.author;
    }

    //    let docRefUniqueId =
    //      this.masterIdentifier.value.toLocaleLowerCase();
    let docRefUniqueId = this.masterIdentifier.value.toLocaleLowerCase();

    docref.masterIdentifier.value = docRefUniqueId; // $12 urn:uuid:537f1c0f-6adc-48b2-b7f9-141f7e639972 DocumentEntry.uniqueId

    let docRefEntryUuid = 'urn:oid:' + this.generateOidFromUuid();
    docref.identifier[0].value = docRefEntryUuid; // $14 identifier.usual DocumentEntry.entryUuid

    docref.subject.reference = this.targetId; // $7 https://test.ahdis.ch/mag-pmp/fhir/Patient/2.16.756.5.30.1.191.1.0.2.1-713d79be-058e-4f55-82a8-e1f81f5e0047
    docref.date = currentDateTime; // $8 2011-11-29T11:00:00+01:00

    docref.content[0].attachment.url = fullUrlBinary; // $1
    docref.content[0].attachment.contentType = this.uploadContentType; // $1.2
    docref.content[0].attachment.language = this.languageCode.value; // $1.3

    let documentReference: fhir.r4.DocumentReference = bundleTransaction
      .entry[2].resource as fhir.r4.DocumentReference;

    documentReference.date = this.creationTime.value;
    documentReference.type = this.getDocumentReferenceType();
    documentReference.category.push(this.getDocumentReferenceCategory());
    documentReference.content[0].format = this.getDocumentReferenceContentFormat();
    documentReference.content[0].attachment.creation = this.creationTime.value;
    documentReference.description = this.documentDescription.value;
    documentReference.securityLabel[0] = this.getDocumentConfidentiality();

    const saml = await this.getSamlToken();

    this.mag
      .transaction({
        body: bundleTransaction as FhirResource & { type: 'transaction' },
        options: {
          headers: {
            accept: 'application/fhir+json;fhirVersion=4.0;charset=UTF-8',
            Authorization: 'Bearer ' + saml,
          },
        },
      })
      .then((response) => {
        this.setJson(JSON.stringify(response, null, 2));
        this.inMhdUploadProgress = false;
      })
      .catch((error) => {
        this.setJson(JSON.stringify(error, null, 2));
        this.inMhdUploadProgress = false;
      });
  }
}
