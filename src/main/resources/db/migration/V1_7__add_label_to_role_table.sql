ALTER TABLE Roles
ADD label varchar(255) NULL;

UPDATE roles SET label = 'Applicant' WHERE name = 'APPLICANT';
UPDATE roles SET label = 'Administrator', description = 'Admins can create and manage grants and application forms.' WHERE name = 'ADMIN';
UPDATE roles SET label = 'Super administrator', description = 'Super admins are able to manage, edit, and delete users of the Find a grant service.' WHERE name = 'SUPER_ADMIN';
UPDATE roles SET label = 'Find' WHERE name = 'FIND';