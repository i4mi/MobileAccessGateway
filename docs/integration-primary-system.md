# EPR Primary System Integration

The REST variants of XDS transactions presented here are based on [CH EPR mHealth](http://fhir.ch/ig/ch-epr-mhealth/index.html),
itself based on various IHE profiles.

## Authentication

You should integrate one of the supported IDPs in your application.
The SAML flow is the only one currently supported.

In the REST transaction, the SAML assertion you got shall be [base64-url](https://datatracker.ietf.org/doc/html/rfc4648#section-5)
encoded, prefixed with `Bearer ` and inserted in an `Authorization` HTTP header.

## Audit messages

### Creating audit messages

For all transactions, it is required to send the same audit messages. You can use the regular ITI-20 transaction,
or use the [restful one](https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_RESTful-ATNA.pdf).

See the [mapping from DICOM to FHIR](http://hl7.org/fhir/R4/auditevent-mappings.html#dicom).

<details>
<summary>Examples</summary>

```http
POST /ARR/fhir/AuditEvent HTTP/1.1
Content-Type: application/fhir+xml

<Bundle>
  <entry>
    ...
    <request>
      <method value="POST"/>
    </request>
  </entry>
</Bundle>
```

</details>

### Reading audit messages

You can read the audit messages for a given patient with an ITI-81 transaction.

!!! warning
This transaction is still implemented on a previous CH:ATC specification (March 2020), based on the [IHE Restfull
ATNA supplement rev. 2.2](https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_RESTful-ATNA_Rev2.2_TI_2017-07-21.pdf).
A lot have changed since.

The transaction is a HTTP GET request on the endpoint, with the parameter `entity-id` that contain the patient EPR-SPID,
and `date` to constraint the audit message date. The `Authorization` header uses the prefix `IHE-SAML` and the SAML
assertion is encoded with the [regular base64 alphabet](https://datatracker.ietf.org/doc/html/rfc4648#section-4).

<details>
<summary>Examples</summary>

```http
GET /ARR/fhir/AuditEvent?entity-id=urn%3Aoid%3A2.16.756.5.30.1.127.3.10.3%7C{epr-spid}&date=ge2023-07-10&date=le2023-07-17 HTTP/1.1
Authorization: IHE-SAML Zm9vYmE=
```

</details>

## Patient directory

The patient directory (called MPI) contains identifiers and demographics for all registered patients.
Identifiers include the MPI-PID and EPR-SPID, and identifiers used by primary systems that choose to share them.
Demographics include the given and family names, date of birth, gender, nationality and telecoms.
It can be queried and updated.

### Retrieve patient identifiers

Patient identifiers (commonly the MPI-PID and EPR-SPID) can be queried with an
[ITI-83 (_Mobile Patient Identifier Cross-reference Query_) transaction](http://fhir.ch/ig/ch-epr-mhealth/iti-83.html).

The transaction is an HTTP GET request to the endpoint `/Patient/$ihe-pix`, with the following parameters:

1. sourceIdentifier (_token_, mandatory): the known patient identifier
2. targetSystem (_uri_, optional): to restrict the results to the MPI-PID and/or EPR-SPID.

<details>
<summary>Examples</summary>

```http title="To retrieve all known identifiers from a local identifier ('1234' in the system 2.999.42)"
GET /Patient/$ihe-pix?sourceIdentifier=urn%3Aoid%3A2.999.42%7C1234 HTTP/1.1
```

```http title="To retrieve the EPR-SPID from the MPI-PID"
GET /Patient/$ihe-pix?sourceIdentifier=urn%3Aoid%3A2.16.756.5.30.1.191.1.0.12.3.101%7C{mpi-pid}&targetSystem=urn%3Aoid%3A2.16.756.5.30.1.127.3.10.3 HTTP/1.1
```

</details>

### Retrieve patient demographics

Patient demographics can be queried with an
[ITI-78 (_Mobile Patient Demographics Query_) transaction](http://fhir.ch/ig/ch-epr-mhealth/iti-78.html).

The transaction can be done in two ways, either by specifying the MPI-PID to retrieve a single patient, or by
specifying other information.

=== "With the MPI-PID"
If the MPI-PID is known, the transaction is an HTTP GET request to the endpoint `/Patient/{id}`, where {id} is the MPI
OID and PID, separated by a dash.

    ```http title="Example"
    GET /Patient/2.16.756.5.30.1.191.1.0.2.1-e7963774-9098-445f-9cab-5d52234b52c3 HTTP/1.1
    ```

=== "With other information"
Otherwise, parameters can be used to search patients with other information:

    - `family` and `given` (_string_)
    - `identifier` (_token_)
    - `telecom` (_token_)
    - `birthdate` (_date_)
    - `address` (_string_): to search in any part of the address.
    - `address-city`,
      `address-country`,
      `address-postalcode`,
      `address-state` (_string_)
    - `gender` (_token_)

    ```http title="Example"
    GET /Patient?family=MOHR&given=ALICE&gender=female HTTP/1.1
    ```

### Feed patient information

Feeding patient information can be done with the [ITI-104 (_Patient Identity Feed FHIR_) transaction](http://fhir.ch/ig/ch-epr-mhealth/iti-104.html).

## Document directory

## Professional and organization directory

The HPD (Healthcare Provider Directory) contains information about the healthcare professionals and organizations that
are part of the EPR. Relationships between them (i.e. membership of professionals to organizations, or relationships
between organizations) are also available.

### Searching

Professionals, organizations and relationships can be queried with an
[ITI-90 (_Find Matching Care Services_) transaction](http://fhir.ch/ig/ch-epr-mhealth/iti-90.html). See the
specifications for the complete list of search parameters.

=== "Professionals"

    ```http title="Search for 'Müller'"
    GET /Practitioner?family=Müller HTTP/1.1
    ```

    ```http title="Search by GLN"
    GET /Practitioner?identifier=urn:oid:2.51.1.3|7601000102737 HTTP/1.1
    ```

=== "Organizations"

=== "Relationships"

=== "Multi-resources"

### Updating

The HPD update is not supported in a REST transaction. Please use the
[ITI-59 (_Provider Information Feed_) transaction](https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_HPD.pdf).
