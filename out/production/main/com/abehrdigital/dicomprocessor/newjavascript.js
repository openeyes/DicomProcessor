var requestData = JSON.parse(request.getJson('request_data', null));

var dicomParser = request.getDicom('request_blob', null);

var dicom = dicomParser.getStudy();

var dicomHeader = JSON.parse(dicom.getHeaderJsonString());

requestData.manufacturer = dicomHeader['524400'];
requestData.model = dicomHeader['528528'].trim();
requestData.version = dicomHeader['570494977'];

if (requestData.manufacturer === 'Carl Zeiss Meditec' && requestData.model === 'IOLMaster 700' && requestData.version === '2.11') {
    requestData.name = dicomHeader['1048592'];
    requestData.dateOfBirth = dicomHeader['1048624'];
    requestData.gender = dicomHeader['1048640'].trim();
    request.putJson(
            'dicom_header',
            JSON.stringify(dicomHeader),
            'dicom_header',
            null,
            'dicom');

    var pdf = dicom.getPdfAsBlob();

    request.putPdf('biometry_report',
            pdf,
            'biometry_report',
            null,
            'application/pdf');


    request.putJson(
            'request_data',
            JSON.stringify(requestData),
            'request_json',
            null, 'json');
    request.addRoutine('PAS_API');

    var biometry = JSON.parse(request.getJson('biometry_data', null));
    biometry.studyId = dicomHeader['2097168'];
    biometry.time = dicomHeader['524336'];
    biometry.date = dicomHeader['4194884'];
    biometry.goodText = dicom.extractTextFromPage(63 , 328 , 70 , 10 , 1);

    print(biometry);

    request.putJson(
            'biometry_data',
            JSON.stringify(biometry),
            'biometry_report',
            null,
            'dicom');

    request.addRoutine('create_biometry_event');
}