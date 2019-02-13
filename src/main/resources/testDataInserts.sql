INSERT INTO routine_library (routine_name , routine_body)
                                                                     VALUES ("dicom_routine", "
                                                                     var requestData = JSON.parse(controller.getJson('request_data', null));

                                                                     var dicomParser = controller.getDicom('request_blob', null);

                                                                     var dicom = dicomParser.getStudy();

                                                                     var dicomHeader = JSON.parse(dicom.getHeaderJsonString());

                                                                     requestData.manufacturer = dicomHeader['524400'];
                                                                     requestData.model = dicomHeader['528528'].trim();
                                                                     requestData.version = dicomHeader['570494977'];

                                                                     if (requestData.manufacturer === 'Carl Zeiss Meditec' && requestData.model === 'IOLMaster 700' && requestData.version === '2.11') {
                                                                         requestData.name = dicomHeader['1048592'];
                                                                         requestData.dateOfBirth = dicomHeader['1048624'];
                                                                         requestData.gender = dicomHeader['1048640'].trim();
                                                                         controller.putJson(
                                                                                 'dicom_header',
                                                                                 JSON.stringify(dicomHeader),
                                                                                 'dicom_header',
                                                                                 null,
                                                                                 'dicom');

                                                                         var pdf = dicom.getPdfAsBlob();

                                                                         controller.putPdf('biometry_report',
                                                                                 pdf,
                                                                                 'biometry_report',
                                                                                 null,
                                                                                 'application/pdf');


                                                                         controller.putJson(
                                                                                 'request_data',
                                                                                 JSON.stringify(requestData),
                                                                                 'request_json',
                                                                                 null, 'json');
                                                                         controller.addRoutine('PAS_API');

                                                                         var biometry = JSON.parse(controller.getJson('biometry_data', null));
                                                                         biometry.studyId = dicomHeader['2097168'];
                                                                         biometry.time = dicomHeader['524336'];
                                                                         biometry.date = dicomHeader['4194884'];
                                                                         biometry.goodText = dicom.extractTextFromPage(63 , 328 , 70 , 10 , 1);

                                                                         print(biometry);

                                                                         controller.putJson(
                                                                                 'biometry_data',
                                                                                 JSON.stringify(biometry),
                                                                                 'biometry_report',
                                                                                 null,
                                                                                 'dicom');

                                                                         controller.addRoutine('create_biometry_event');
                                                                     }");

                                                                     INSERT INTO routine_library (routine_name , routine_body)
                                                                     VALUES ("PAS_API" , "print('api pas');");

                                                                     INSERT INTO routine_library (routine_name , routine_body)
                                                                     VALUES ("create_biometry_event" , "print('biometry biometry');");

                                                                     INSERT INTO request_queue (request_queue , maximum_active_threads , busy_yield_ms , idle_yield_ms)
                                                                     VALUES ("dicom_queue" , 5 , 1000 , 1000);

                                                                     INSERT INTO request_type (request_type , title_full , title_short , default_routine_name , default_request_queue)
                                                                     VALUES ("dicom_request" , "dicom request full" , "dicom request short" ,  "dicom_routine" , "dicom_queue");

                                                                     INSERT INTO request (system_message , request_type , request_override_default_queue)
                                                                     VALUES("eyes open eyes open" , "dicom_request" , "dicom_queue");

                                                                     INSERT INTO request (system_message , request_type , request_override_default_queue)
                                                                     VALUES("eyes open eyes open" , "dicom_request" , "dicom_queue");

                                                                     INSERT INTO request (system_message , request_type , request_override_default_queue)
                                                                     VALUES("eyes open eyes open" , "dicom_request" , "dicom_queue");

                                                                     INSERT INTO request (system_message , request_type , request_override_default_queue)
                                                                     VALUES("eyes open eyes open" , "dicom_request" , "dicom_queue");

                                                                     INSERT INTO request (system_message , request_type , request_override_default_queue)
                                                                     VALUES("eyes open eyes open" , "dicom_request" , "dicom_queue");

                                                                     INSERT INTO request_routine (request_id , execute_request_queue , STATUS , routine_name , execute_sequence)
                                                                     VALUES( 2 , "dicom_queue" , "NEW" , "dicom_routine" , 10);

                                                                     INSERT INTO request_routine (request_id , execute_request_queue , STATUS , routine_name , execute_sequence)
                                                                     VALUES( 1 , "dicom_queue" , "NEW" , "dicom_routine" , 10);

                                                                     INSERT INTO request_routine (request_id , execute_request_queue , STATUS , routine_name , execute_sequence)
                                                                     VALUES( 3 , "dicom_queue" , "NEW" , "dicom_routine" , 10);

                                                                     INSERT INTO request_routine (request_id , execute_request_queue , STATUS , routine_name , execute_sequence)
                                                                     VALUES( 4 , "dicom_queue" , "NEW" , "dicom_routine" , 10);

                                                                     INSERT INTO request_routine (request_id , execute_request_queue , STATUS , routine_name , execute_sequence)
                                                                     VALUES( 5 , "dicom_queue" , "NEW" , "dicom_routine" , 10);

                                                                     INSERT INTO attachment_type (attachment_type , title_full , title_short , title_abbreviated)
                                                                     VALUES ("request_json" , "json from attachment" , "js0n" , "JSON");

                                                                     INSERT INTO attachment_type (attachment_type , title_full , title_short , title_abbreviated)
                                                                     VALUES ("biometry_report" , "biometry_report" , "biometry_report" , "biometry");

                                                                     INSERT INTO mime_type (mime_type)
                                                                     VALUES ("json");

                                                                     INSERT INTO mime_type (mime_type)
                                                                     VALUES ("dicom");

                                                                     INSERT INTO mime_type (mime_type)
                                                                     VALUES ("application/pdf");

                                                                     INSERT INTO attachment_type (attachment_type , title_full , title_short , title_abbreviated)
                                                                     VALUES ("dicom_header" , "hedar" , "heder" , "heda");

                                                                     INSERT INTO attachment_type (attachment_type , title_full , title_short , title_abbreviated)
                                                                     VALUES ("dicom" , "hedar" , "heder" , "heda");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , json_data)
                                                                     VALUES (1 , "request_data" , 0 , "request_json" , "json" , "{}");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (1, "dicom_header" , 1 , "dicom_header" , "dicom" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES ( 1 , "request_blob" , 1 ,"dicom" , "application/pdf" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , json_data)
                                                                     VALUES (2 , "request_data" , 0 , "request_json" , "json" , "{}");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (2, "dicom_header" , 1 , "dicom_header" , "dicom" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES ( 2 , "request_blob" , 1 ,"dicom" , "application/pdf" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , json_data)
                                                                     VALUES (3 , "request_data" , 0 , "request_json" , "json" , "{}");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (3, "dicom_header" , 1 , "dicom_header" , "dicom" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES ( 3 , "request_blob" , 1 ,"dicom" , "application/pdf" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , json_data)
                                                                     VALUES (4 , "request_data" , 0 , "request_json" , "json" , "{}");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (4, "dicom_header" , 1 , "dicom_header" , "dicom" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (4 , "request_blob" , 1 ,"dicom" , "application/pdf" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , json_data)
                                                                     VALUES (5 , "request_data" , 0 , "request_json" , "json" , "{}");

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES (5, "dicom_header" , 1 , "dicom_header" , "dicom" , NULL);

                                                                     INSERT INTO attachment_data (request_id , attachment_mnemonic , system_only_managed , attachment_type , mime_type , blob_data)
                                                                     VALUES ( 5 , "request_blob" , 1 ,"dicom" , "application/pdf" , NULL);