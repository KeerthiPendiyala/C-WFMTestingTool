ALTER TABLE uploaded_document DROP CONSTRAINT ck_uploaded_document_source_type;
ALTER TABLE uploaded_document
    ADD CONSTRAINT ck_uploaded_document_source_type
    CHECK (source_type IN ('PDF', 'DOCX', 'DOC', 'XLS', 'CSV', 'OTHER'));

ALTER TABLE requirement DROP CONSTRAINT ck_requirement_source_type;
ALTER TABLE requirement
    ADD CONSTRAINT ck_requirement_source_type
    CHECK (source_type IN ('MANUAL', 'PDF', 'DOCX', 'DOC', 'XLS', 'CSV', 'OTHER', 'AI'));
