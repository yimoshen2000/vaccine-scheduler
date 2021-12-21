CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    app_id INT NOT NULL IDENTITY(1,1),
    caregiver_name VARCHAR(255) REFERENCES Caregivers,
    vaccine_name VARCHAR(255) REFERENCES Vaccines,
    patient_name VARCHAR(255) REFERENCES Patients,
    app_time date,
    PRIMARY KEY (app_id)
);