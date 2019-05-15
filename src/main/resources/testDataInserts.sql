

insert into routine_library (routine_name)
values
  (
    "create_event"
  );

insert into routine_library (routine_name)
values
  (
    "prototype_event"
  );

insert into routine_library (routine_name)
values
  (
    "link_attachment_with_event"
  );

insert into request (
  system_message,
  request_type,
  request_override_default_queue
)
values
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

insert into request (
  system_message,
  request_type,
  request_override_default_queue
)
values
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

insert into request (
  system_message,
  request_type,
  request_override_default_queue
)
values
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

insert into request (
  system_message,
  request_type,
  request_override_default_queue
)
values
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

insert into request (
  system_message,
  request_type,
  request_override_default_queue
)
values
  (
    "eyes open eyes open",
    "dicom_request",
    "dicom_queue"
  );

insert into request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
values
  (
    2,
    "dicom_queue",
    "NEW",
    "OCT_SEED",
    10
  );

insert into request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
values
  (
    1,
    "dicom_queue",
    "NEW",
    "DICOM_SEED",
    10
  );

insert into request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
values
  (
    3,
    "dicom_queue",
    "NEW",
    "DICOM_SEED",
    10
  );

insert into request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
values
  (
    4,
    "dicom_queue",
    "NEW",
    "DICOM_SEED",
    10
  );

insert into request_routine (
  request_id,
  execute_request_queue,
  STATUS,
  routine_name,
  execute_sequence
)
values
  (
    5,
    "dicom_queue",
    "NEW",
    "DICOM_SEED",
    10
  );

insert into attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
values
  (
    "request_json",
    "json from attachment",
    "js0n",
    "JSON"
  );

insert into attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
values
  ("event_pdf", "pdf", "pdf", "pdf");

insert into attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
values ("event_data", "pdf", "pdf", "pdf");

insert into attachment_type (
  attachment_type,
  title_full,
  title_short,
  title_abbreviated
)
values
  (
    "biometry_report",
    "biometry_report",
    "biometry_report",
    "biometry"
  );

insert into mime_type (mime_type)
values
  ("json");

insert into mime_type (mime_type)
values
  ("dicom");

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
values
  (
    1,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    1,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    1,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
values
  (
    2,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    2,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    2,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
values
  (
    3,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    3,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    3,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
values
  (
    4,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    4,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    4,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  json_data
)
values
  (
    5,
    "request_data",
    0,
    "request_json",
    "json",
    "{}"
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    5,
    "dicom_header",
    1,
    "dicom_header",
    "dicom",
    null
  );

insert into attachment_data (
  request_id,
  attachment_mnemonic,
  system_only_managed,
  attachment_type,
  mime_type,
  blob_data
)
values
  (
    5,
    "request_blob",
    1,
    "dicom",
    "application/pdf",
    null
  );

insert into event_subtype (
    event_subtype,
    dicom_modality_code,
    icon_name,
    display_name
) values
    (
     'OCT',
     'OAM',
     'i-ImToricIOL',
     'oct event subtype from dicom'
    );

