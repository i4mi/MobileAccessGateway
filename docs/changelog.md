## 2024/11/26 v1.0.0

- Fixed an NPE in the assertion route
- Improved mapping between XDS XON and FHIR Organization [165](https://github.com/i4mi/MobileAccessGateway/issues/165)
- Capability statement validation issues [#177](https://github.com/i4mi/MobileAccessGateway/issues/177)
- Add CH PIXm ITI-83 constraints for sourceIdentifier and targetSystem [#170](https://github.com/i4mi/MobileAccessGateway/issues/170)
- Add support for 401 error code [#182](https://github.com/i4mi/MobileAccessGateway/issues/182)
- Allow using the EPR-SPID as Patient id

## 2024/05/15 v070
- support for multiple IDP's [128](https://github.com/i4mi/MobileAccessGateway/issues/128)
- reenable $find-medication-list
- use IdP also for patient access in web app

## 2024/05/08 v064

- Added OAuth URLs to the CapabilityStatement [#135](https://github.com/i4mi/MobileAccessGateway/issues/135)
- Implemented PPQm routes [#126](https://github.com/i4mi/MobileAccessGateway/issues/126)
- Upgraded to IPF 4.8.0
- Removed dependency on IHE-Europe STS simulator
- Support for email in real case scenario with invalid contact infos [#138](https://github.com/i4mi/MobileAccessGateway/pull/138)
- Added missing document entry attributes
- Certificate can be injected by environment variables 
- Fixed coding system OIDs of document author and uploader role codes


## 2024/02/08 v062

- `docker pull europe-west6-docker.pkg.dev/ahdis-ch/ahdis/mag:v062`
- Updated documentation on [https://i4mi.github.io/MobileAccessGateway/](https://i4mi.github.io/MobileAccessGateway/)
  for containers [#132](https://github.com/i4mi/MobileAccessGateway/issues/132)

## Older releases

For older releases see github [tags](https://github.com/i4mi/MobileAccessGateway/tags)
