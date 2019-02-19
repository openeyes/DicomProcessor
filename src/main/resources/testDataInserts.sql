INSERT INTO routine_library (routine_name, routine_body)
VALUES
  (
    "dicom_routine",
    "
 var requestData = JSON.parse(getJson('request_data', null));

 var dicomParser = getDicom('request_blob', null);

 var dicom = dicomParser.getStudy();

 var dicomHeader = JSON.parse(dicom.getHeaderJsonString());

 requestData.manufacturer = dicomHeader['524400'];
 requestData.model = dicomHeader['528528'].trim();
 requestData.version = dicomHeader['570494977'];

     requestData.hosNum = dicomHeader['1048608'];
     requestData.dateOfBirth = dicomHeader['1048624'];
     requestData.gender = dicomHeader['1048640'].trim();
     putJson(
             'dicom_header',
             JSON.stringify(dicomHeader),
             'dicom_header',
             null,
             'dicom');

     var pdf = dicom.getPdfAsBlob();

     putPdf('event_pdf',
             pdf,
             'event_pdf',
             null,
             'application/pdf');


     putJson(
             'request_data',
             JSON.stringify(requestData),
             'request_json',
             null, 'json');
     addRoutine('prototype_event');
     addRoutine('PAS_API');

     var biometry = JSON.parse(getJson('biometry_data', null));
     biometry.studyId = dicomHeader['2097168'];
     biometry.time = dicomHeader['524336'];
     biometry.date = dicomHeader['4194884'];

     putJson(
             'biometry_data',
             JSON.stringify(biometry),
             'biometry_report',
             null,
             'dicom');

     addRoutine('create_biometry_event');
     addRoutine('create_event');
      "
  );

INSERT INTO routine_library (routine_name, routine_body)
VALUES
  (
    "PAS_API",
    "var eventData = JSON.parse(getJson('event_data', null));
     var requestData = JSON.parse(getJson('request_data', null));
     requestData.patientId = getPatientId(requestData.hosNum , requestData.dateOfBirth , requestData.gender);
        putJson(
        'event_data',
        JSON.stringify(eventData),
         'request_json' ,
          null, 'json');
     "
  );

INSERT INTO routine_library (routine_name, routine_body)
VALUES
  (
    "create_biometry_event",
    "print('biometry biometry');"
  );

INSERT INTO routine_library (routine_name, routine_body)
VALUES
  (
    "create_event",
    " var eventData = JSON.parse(getJson('event_data', null));
     var requestData = JSON.parse(getJson('request_data', null));
    var attachmentPdf = getAttachmentDataByAttachmentMnemonicAndBodySite('event_pdf' , null);

          eventData.$$_XID_Map_$$.forEach(function(data){
              if(data.$$_XID_$$ == '$$_attachment_data[1]_$$'){
               data.id = attachmentPdf.getId().toString();
                }
                if(data.$$_XID_$$ == '$$_patient[1]_$$'){
                 data.id = requestData.patientId.toString();
                   }
                   });

      createEvent(JSON.stringify(eventData));"
  );

INSERT INTO routine_library (routine_name, routine_body)
VALUES
  (
    "prototype_event",
    "var eventTemplate = getEmptyEventTemplate();
     putJson(
      'event_data',
     eventTemplate,
     'event_data',
     null, 'json');"
  );

INSERT INTO request_queue (
  request_queue,
  maximum_active_threads,
  busy_yield_ms,
  idle_yield_ms
)
VALUES
  ("dicom_queue", 5, 1000, 1000);

INSERT INTO request_type (
  request_type,
  title_full,
  title_short,
  default_routine_name,
  default_request_queue
)
VALUES
  (
    "dicom_request",
    "dicom request full",
    "dicom request short",
    "dicom_routine",
    "dicom_queue"
  );

INSERT INTO request (
  system_message,
  request_type,
  request_override_default_queue
)
VALUES
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

INSERT INTO request (
  system_message,
  request_type,
  request_override_default_queue
)
VALUES
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

INSERT INTO request (
  system_message,
  request_type,
  request_override_default_queue
)
VALUES
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

INSERT INTO request (
  system_message,
  request_type,
  request_override_default_queue
)
VALUES
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

INSERT INTO request (
  system_message,
  request_type,
  request_override_default_queue
)
VALUES
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

INSERT INTO request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
VALUES
  (
    2,
    "dicom_queue",
    "NEW",
    "dicom_routine",
    10
  );

INSERT INTO request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
VALUES
  (
    1,
    "dicom_queue",
    "NEW",
    "dicom_routine",
    10
  );

INSERT INTO request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
VALUES
  (
    3,
    "dicom_queue",
    "NEW",
    "dicom_routine",
    10
  );

INSERT INTO request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
VALUES
  (
    4,
    "dicom_queue",
    "NEW",
    "dicom_routine",
    10
  );

INSERT INTO request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
VALUES
  (
    5,
    "dicom_queue",
    "NEW",
    "dicom_routine",
    10
  );

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES
  (
    "request_json",
    "json from attachment",
    "js0n",
    "JSON"
  );

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES
  ("event_pdf", "pdf", "pdf", "pdf");

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES ("event_data", "pdf", "pdf", "pdf");

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES
  (
    "biometry_report",
    "biometry_report",
    "biometry_report",
    "biometry"
  );

INSERT INTO mime_type (mime_type)
VALUES
  ("json");

INSERT INTO mime_type (mime_type)
VALUES
  ("dicom");

INSERT INTO mime_type (mime_type)
VALUES
  ("application/pdf");

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES
  (
    "dicom_header",
    "hedar",
    "heder",
    "heda"
  );

INSERT INTO attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
VALUES
  ("dicom", "hedar", "heder", "heda");

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
VALUES
  (
    1,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    1,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    1,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
VALUES
  (
    2,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    2,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    2,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
VALUES
  (
    3,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    3,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    3,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
VALUES
  (
    4,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    4,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    4,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
VALUES
  (
    5,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    5,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    NULL
  );

INSERT INTO attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
VALUES
  (
    5,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    NULL
  );

