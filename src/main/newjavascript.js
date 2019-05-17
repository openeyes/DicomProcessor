
 var requestData = JSON.parse(controller.getText('request_data', null));

 var dicomParser = controller.getDicom('request_blob', null);

 var dicom = dicomParser.getStudy();

 var dicomHeader = JSON.parse(dicom.getHeaderJsonString());

 requestData.manufacturer = dicomHeader['524400'];
 requestData.model = dicomHeader['528528'].trim();
 requestData.version = dicomHeader['570494977'];

 var fullName = dicomHeader['1048592'].split('^');
	 requestData.lastName = fullName[0];
	 requestData.firstName = fullName[1];
	 requestData.dateOfBirth = dicomHeader['1048624'];
	 requestData.gender = dicomHeader['1048640'].trim();
	 controller.putJson(
			 'dicom_header',
			 JSON.stringify(dicomHeader),
			 'dicom_header',
			 null,
			 'dicom');

var pdf = dicom.getPdfAsBlob();

controller.putPdf('event_pdf',
			 pdf,
			 'event_pdf',
			 null,
			 'application/pdf');


controller.putJson(
	 'request_data',
	 JSON.stringify(requestData),
	 'request_json',
	 null, 'json');
	 controller.addRoutine('prototype_event');
controller.addRoutine('PAS_API');

	 var biometry = JSON.parse(controller.getText('biometry_data', null));
	 biometry.studyId = dicomHeader['2097168'];
	 biometry.time = dicomHeader['524336'];
	 biometry.date = dicomHeader['4194884'];

	 controller.putJson(
			 'biometry_data',
			 JSON.stringify(biometry),
			 'biometry_report',
			 null,
			 'dicom');

	 controller.addRoutine('create_biometry_event');
	 controller.addRoutine('create_event');
 