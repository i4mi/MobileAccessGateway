The Mobile Access Gateway provides translation from REST to SOAP variants of IHE transaction:


| IHE-Profile | ITI    | Transaction Name                              | IHE Actor                     | Implemented in the Gateway with following actors   | Transaction |
| ----------- | ------ | ---------------------------------------------- | ----------------------------- | -------------------------------------------------- | ---------- |
| PDQm        | ITI-78 | Mobile Patient Demographics Query              | Patient Demographics Supplier | PDQv3 Patient Demographics Consumer                | ITI-47           |
| PIXm        | ITI-83 | Mobile Patient Identifier Cross-reference Query | Patient Identity Manager      | PIX V3 Patient Identifier Cross-reference Consumer | ITI-45           |
| PIXm        | ITI-104 | Patient Identity Feed FHIR                    | Patient Identity Manager      | PIX V3 Patient Identity Source                    |   ITI-44         |
| MHD         | ITI-65 | Provide Document Bundle                        | Document Recipient            | XDS Document Source, X-Service-User                |  ITI-41          |
| MHD         | ITI-66 | Find Document Lists                        | Document Responder            | XDS Document Consumer, X-Service-User              |   ITI-18         |
| MHD         | ITI-67 | Find Document References                       | Document Responder            | XDS Document Consumer, X-Service-User              |   ITI-18         |
| MHD         | ITI-68 | Retrieve Document                              | Document Responder            | XDS Document Consumer, X-Service-User              | ITI-43           |
| IUA         | ITI-71 | Get Access Token                              | IUA Authorization Server           |  X-Service-User              |   Authenticate User /Get X-User Assertion         |
| CMPD         | PHARM-5 | Query Pharmacy Documents                          |   Community Pharmacy Manager          | Querying Actor              |  PHARM-1       |

For Authentication/Authorization you have two different options:

1. Integrate the IdP into your software and exchange the IdP saml2 token over a REST API to a XUA token [link](/integration-primary-system/)
2. Configure the IdP's with the Mobile Access Gateway and use an OAuth token (in development)
