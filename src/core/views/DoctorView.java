package core.views;

import core.controllers.utils.Response;
import core.controllers.utils.Status;
import core.controllers.doctor.DoctorController;
import core.controllers.doctor.IDoctorController;
import core.controllers.hospitalization.HospitalizationController;
import core.controllers.hospitalization.IHospitalizationController;
import core.controllers.patient.PatientController;
import core.controllers.patient.IPatientController;
import core.controllers.appointment.AppointmentController;
import core.controllers.appointment.IAppointmentController;
import core.observer.Observer;
import core.observer.EventType;
import core.models.storage.IStorage;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author jjlora
 * @author edangulo
 */

public class DoctorView extends javax.swing.JFrame implements Observer {

    private int x, y;
    private final IStorage database;
    private final IDoctorController doctorController;
    private final IHospitalizationController hospitalizationController;
    private final IPatientController patientController;
    private final IAppointmentController appointmentController;
    private final long loggedUserId;
    private final long doctorId;
    private final boolean isAdminAccess;

    public DoctorView(IStorage database, long loggedUserId, long doctorId, boolean isAdminAccess) {
        this.database = database;
        this.loggedUserId = loggedUserId;
        this.doctorId = doctorId;
        this.isAdminAccess = isAdminAccess;
        this.doctorController = new DoctorController(database);
        this.hospitalizationController = new HospitalizationController(database);
        this.patientController = new PatientController(database);
        this.appointmentController = new AppointmentController(database);
        initComponents();
        if (isAdminAccess)
            btnBack.setVisible(true);
        else    
            btnBack.setVisible(false);
        this.setBackground(new Color(0, 0, 0, 0));
        this.setLocationRelativeTo(null);
        database.addObserver(this);
        loadDoctorData();
        loadComboBoxes();
        refreshAppointmentTable(false);
        refreshPatientComboBox();
    }
    
    @Override
    public void update(EventType event, Object data) {
        if (event == EventType.APPOINTMENT_ADDED || event == EventType.APPOINTMENT_UPDATED) {
            refreshAppointmentTable(radioPendingAppts.isSelected());
            loadComboBoxes();
        }
        if (event == EventType.USER_ADDED || event == EventType.USER_UPDATED) {
            refreshPatientComboBox();
            loadDoctorData();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDoctorData() {
        Response resp = doctorController.getDoctorData(doctorId);
        if (resp.getStatus() == Status.OK && resp.getData() instanceof Map) {
            Map<String, String> d = (Map<String, String>) resp.getData();
            txtUpdateFirstname.setText(d.getOrDefault("firstname", ""));
            txtUpdateLastname.setText(d.getOrDefault("lastname", ""));
            txtUpdateUsername.setText(d.getOrDefault("username", ""));
            txtUpdateOffice.setText(d.getOrDefault("assignedOffice", ""));
            txtUpdateLicense.setText(d.getOrDefault("licenceNumber", ""));
            txtUpdatePassword.setText(d.getOrDefault("password", ""));
            txtUpdateConfirmPass.setText(d.getOrDefault("password", ""));
            selectSpecialty(d.getOrDefault("specialty", ""));
        }
    }

    private void selectSpecialty(String specialty) {
        String value = specialty.replace("_", " ");
        for (int i = 0; i < cmbUpdateSpecialty.getItemCount(); i++) {
            String item = cmbUpdateSpecialty.getItemAt(i).replace("&", " ").replaceAll("\\s+", " ").trim().toUpperCase();
            if (item.equals(value)) {
                cmbUpdateSpecialty.setSelectedIndex(i);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshPatientComboBox() {
        cmbSearchPatient.removeAllItems();
        cmbSearchPatient.addItem("Select one");
        Response resp = doctorController.getPatientList();
        if (resp.getStatus() == Status.OK && resp.getData() instanceof List) {
            List<Map<String, String>> patients = (List<Map<String, String>>) resp.getData();
            for (Map<String, String> p : patients) {
                cmbSearchPatient.addItem(p.get("id") + " - " + p.get("firstname") + " " + p.get("lastname"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadComboBoxes() {
        cmbAcceptAppt.removeAllItems();
        cmbRescheduleAppt.removeAllItems();
        cmbCompleteApptSelector.removeAllItems();
        cmbPrescribeAppt.removeAllItems();
        cmbHospitalizeApptSelector.removeAllItems();
        cmbAcceptAppt.addItem("Select one");
        cmbRescheduleAppt.addItem("Select one");
        cmbCompleteApptSelector.addItem("Select one");
        cmbPrescribeAppt.addItem("Select one");
        cmbHospitalizeApptSelector.addItem("Select one");

        Response resp = appointmentController.getDoctorAppointments(doctorId, false);
        if (resp.getStatus() == Status.OK && resp.getData() instanceof List) {
            List<Map<String, String>> appts = (List<Map<String, String>>) resp.getData();
            for (Map<String, String> a : appts) {
                String status = a.getOrDefault("status", "");
                if ("REQUESTED".equals(status)) {
                    cmbAcceptAppt.addItem(a.get("id"));
                }
                if (!"CANCELED".equals(status) && !"COMPLETED".equals(status)) {
                    cmbRescheduleAppt.addItem(a.get("id"));
                }
                if ("PENDING".equals(status)) {
                    cmbCompleteApptSelector.addItem(a.get("id"));
                    cmbPrescribeAppt.addItem(a.get("id"));
                    cmbHospitalizeApptSelector.addItem(a.get("id"));
                }
            }
        }

        cmbCompleteHosp.removeAllItems();
        cmbCompleteHosp.addItem("Select one");
        for (String rt : new String[]{"ICU", "IMC", "GENERAL", "PEDIATRICS", "MATERNITY", "ONCOLOGY"}) {
            cmbCompleteHosp.addItem(rt);
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshAppointmentTable(boolean pendingOnly) {
        Response response = appointmentController.getDoctorAppointments(doctorId, pendingOnly);
        if (response.getStatus() != Status.OK || !(response.getData() instanceof List)) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) tableAppointments.getModel();
        model.setRowCount(0);
        for (Map<String, String> a : (List<Map<String, String>>) response.getData()) {
            model.addRow(new Object[]{
                a.get("id"),
                a.get("datetime"),
                a.get("patientName"),
                a.get("specialty"),
                a.get("type"),
                a.get("status")
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelRound1 = new core.views.PanelRound();
        panelRound2 = new core.views.PanelRound();
        btnClose = new javax.swing.JButton();
        lblTitle = new javax.swing.JLabel();
        btnBack = new javax.swing.JButton();
        tabsDoctor = new javax.swing.JTabbedPane();
        panelAppointmentTable = new javax.swing.JPanel();
        radioAllAppts = new javax.swing.JRadioButton();
        scrollAppointments = new javax.swing.JScrollPane();
        tableAppointments = new javax.swing.JTable();
        radioPendingAppts = new javax.swing.JRadioButton();
        btnLogout = new javax.swing.JButton();
        panelPatientHistory = new javax.swing.JPanel();
        cmbSearchPatient = new javax.swing.JComboBox<>();
        lblSearchPatient = new javax.swing.JLabel();
        scrollPatientHistory = new javax.swing.JScrollPane();
        tablePatientHistory = new javax.swing.JTable();
        btnSearchPatient = new javax.swing.JButton();
        panelProfile = new javax.swing.JPanel();
        lblUpdateFirstname = new javax.swing.JLabel();
        txtUpdateFirstname = new javax.swing.JTextField();
        lblUpdateLastname = new javax.swing.JLabel();
        txtUpdateLastname = new javax.swing.JTextField();
        lblUpdateSpecialty = new javax.swing.JLabel();
        lblUpdateLicense = new javax.swing.JLabel();
        txtUpdateLicense = new javax.swing.JTextField();
        lblUpdateOffice = new javax.swing.JLabel();
        txtUpdateOffice = new javax.swing.JTextField();
        lblUpdateUser = new javax.swing.JLabel();
        txtUpdateUsername = new javax.swing.JTextField();
        txtUpdatePassword = new javax.swing.JTextField();
        lblUpdatePassword = new javax.swing.JLabel();
        lblUpdateConfirmPass = new javax.swing.JLabel();
        txtUpdateConfirmPass = new javax.swing.JTextField();
        cmbUpdateSpecialty = new javax.swing.JComboBox<>();
        btnSaveProfile = new javax.swing.JButton();
        panelAppointmentActions = new javax.swing.JPanel();
        lblAcceptApptId = new javax.swing.JLabel();
        lblAcceptApptSection = new javax.swing.JLabel();
        cmbAcceptAppt = new javax.swing.JComboBox<>();
        sepVertical1 = new javax.swing.JSeparator();
        btnAcceptAppt = new javax.swing.JButton();
        lblRescheduleSection = new javax.swing.JLabel();
        lblRescheduleAppt = new javax.swing.JLabel();
        cmbRescheduleAppt = new javax.swing.JComboBox<>();
        btnConfirmReschedule = new javax.swing.JButton();
        lblRescheduleTime = new javax.swing.JLabel();
        txtRescheduleDate = new javax.swing.JTextField();
        lblRescheduleReason = new javax.swing.JLabel();
        txtRescheduleTime = new javax.swing.JTextField();
        sepVertical2 = new javax.swing.JSeparator();
        lblCompleteApptSection = new javax.swing.JLabel();
        lblCompleteApptId = new javax.swing.JLabel();
        cmbCompleteApptSelector = new javax.swing.JComboBox<>();
        lblCompleteDiagnosis = new javax.swing.JLabel();
        lblCompleteObservations = new javax.swing.JLabel();
        lblCompleteTreatment = new javax.swing.JLabel();
        lblCompleteFollowUp = new javax.swing.JLabel();
        btnCompleteAppt = new javax.swing.JButton();
        lblHospSection = new javax.swing.JLabel();
        lblHospReason = new javax.swing.JLabel();
        lblHospDate = new javax.swing.JLabel();
        txtHospitalizationEntryDate = new javax.swing.JTextField();
        lblHospDuration = new javax.swing.JLabel();
        txtHospitalizationEstDuration = new javax.swing.JTextField();
        lblHospObservations = new javax.swing.JLabel();
        scrollHospObservations = new javax.swing.JScrollPane();
        txtAreaHospitalizationObservations = new javax.swing.JTextArea();
        btnGenerateHosp = new javax.swing.JButton();
        cmbCompleteHosp = new javax.swing.JComboBox<>();
        radioRequestsFilter = new javax.swing.JRadioButton();
        radioPatientFilter = new javax.swing.JRadioButton();
        scrollDiagnosis = new javax.swing.JScrollPane();
        txtAreaCompleteDiagnosis = new javax.swing.JTextArea();
        scrollObservations = new javax.swing.JScrollPane();
        txtAreaCompleteObservations = new javax.swing.JTextArea();
        scrollTreatment = new javax.swing.JScrollPane();
        txtAreaCompleteTreatment = new javax.swing.JTextArea();
        scrollFollowUp = new javax.swing.JScrollPane();
        txtAreaCompleteFollowUp = new javax.swing.JTextArea();
        sepVertical3 = new javax.swing.JSeparator();
        btnCancelHospitalization = new javax.swing.JButton();
        cmbHospitalizeApptSelector = new javax.swing.JComboBox<>();
        scrollHospReason = new javax.swing.JScrollPane();
        txtAreaHospitalizationReason = new javax.swing.JTextArea();
        panelPrescriptions = new javax.swing.JPanel();
        lblPrescribeApptLabel = new javax.swing.JLabel();
        lblPrescribeMedName = new javax.swing.JLabel();
        txtPrescribeMedication = new javax.swing.JTextField();
        lblPrescribeDose = new javax.swing.JLabel();
        txtPrescribeDosage = new javax.swing.JTextField();
        lblPrescribeRoute = new javax.swing.JLabel();
        txtPrescribeRoute = new javax.swing.JTextField();
        lblPrescribeFrequency = new javax.swing.JLabel();
        txtPrescribeFrequency = new javax.swing.JTextField();
        lblPrescribeDuration = new javax.swing.JLabel();
        txtPrescribeDuration = new javax.swing.JTextField();
        lblPrescribeAdditional = new javax.swing.JLabel();
        txtPrescribeAdditional = new javax.swing.JTextField();
        scrollMedications = new javax.swing.JScrollPane();
        tableMedications = new javax.swing.JTable();
        btnAddMedication = new javax.swing.JButton();
        btnPrescribe = new javax.swing.JButton();
        cmbPrescribeAppt = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);

        panelRound1.setRadius(50);

        panelRound2.setRadius(50);
        panelRound2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                panelRound2MouseDragged(evt);
            }
        });
        panelRound2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                panelRound2MousePressed(evt);
            }
        });

        btnClose.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnClose.setText("X");
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnClose.setFocusable(false);
        btnClose.setRequestFocusEnabled(false);
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        lblTitle.setFont(new java.awt.Font("Yu Gothic UI", 0, 14)); // NOI18N
        lblTitle.setText("DOCTOR VIEW");

        btnBack.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelRound2Layout = new javax.swing.GroupLayout(panelRound2);
        panelRound2.setLayout(panelRound2Layout);
        panelRound2Layout.setHorizontalGroup(
            panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRound2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addGap(32, 32, 32)
                .addComponent(btnBack)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addGap(19, 19, 19))
        );
        panelRound2Layout.setVerticalGroup(
            panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btnClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btnBack))
        );

        radioAllAppts.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioAllAppts.setText("Total appointments");
        radioAllAppts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioAllApptsActionPerformed(evt);
            }
        });

        tableAppointments.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Date", "Patient", "Specialty", "Type", "Status"
            }
        ));
        scrollAppointments.setViewportView(tableAppointments);

        radioPendingAppts.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioPendingAppts.setText("Pending appointments");
        radioPendingAppts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioPendingApptsActionPerformed(evt);
            }
        });

        btnLogout.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnLogout.setText("Logout");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAppointmentTableLayout = new javax.swing.GroupLayout(panelAppointmentTable);
        panelAppointmentTable.setLayout(panelAppointmentTableLayout);
        panelAppointmentTableLayout.setHorizontalGroup(
            panelAppointmentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAppointmentTableLayout.createSequentialGroup()
                .addGroup(panelAppointmentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnLogout)
                    .addGroup(panelAppointmentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAppointmentTableLayout.createSequentialGroup()
                            .addGap(16, 16, 16)
                            .addComponent(radioAllAppts)
                            .addGap(18, 18, 18)
                            .addComponent(radioPendingAppts))
                        .addGroup(panelAppointmentTableLayout.createSequentialGroup()
                            .addGap(108, 108, 108)
                            .addComponent(scrollAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 1035, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(152, Short.MAX_VALUE))
        );
        panelAppointmentTableLayout.setVerticalGroup(
            panelAppointmentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAppointmentTableLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(panelAppointmentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioAllAppts)
                    .addComponent(radioPendingAppts))
                .addGap(18, 18, 18)
                .addComponent(scrollAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 504, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addComponent(btnLogout)
                .addGap(23, 23, 23))
        );

        tabsDoctor.addTab("Appointments visualization", panelAppointmentTable);

        cmbSearchPatient.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbSearchPatient.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        lblSearchPatient.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblSearchPatient.setText("Patient");

        tablePatientHistory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Date", "Doctor", "Specialty", "Type", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scrollPatientHistory.setViewportView(tablePatientHistory);

        btnSearchPatient.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnSearchPatient.setText("Search");
        btnSearchPatient.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchPatientActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelPatientHistoryLayout = new javax.swing.GroupLayout(panelPatientHistory);
        panelPatientHistory.setLayout(panelPatientHistoryLayout);
        panelPatientHistoryLayout.setHorizontalGroup(
            panelPatientHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPatientHistoryLayout.createSequentialGroup()
                .addGroup(panelPatientHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPatientHistoryLayout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(lblSearchPatient)
                        .addGap(18, 18, 18)
                        .addComponent(cmbSearchPatient, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelPatientHistoryLayout.createSequentialGroup()
                        .addGap(63, 63, 63)
                        .addComponent(scrollPatientHistory, javax.swing.GroupLayout.PREFERRED_SIZE, 1133, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(99, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPatientHistoryLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnSearchPatient)
                .addGap(601, 601, 601))
        );
        panelPatientHistoryLayout.setVerticalGroup(
            panelPatientHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPatientHistoryLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(panelPatientHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSearchPatient)
                    .addComponent(cmbSearchPatient, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(scrollPatientHistory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(44, 44, 44)
                .addComponent(btnSearchPatient)
                .addContainerGap(67, Short.MAX_VALUE))
        );

        tabsDoctor.addTab("History Appointments of a patient", panelPatientHistory);

        lblUpdateFirstname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateFirstname.setText("Firstname");

        txtUpdateFirstname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateLastname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateLastname.setText("Lastname");

        txtUpdateLastname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateSpecialty.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateSpecialty.setText("Specialty");

        lblUpdateLicense.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateLicense.setText("License Number");

        txtUpdateLicense.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateOffice.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateOffice.setText("Assigned office");

        txtUpdateOffice.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateUser.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateUser.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblUpdateUser.setText("User");

        txtUpdateUsername.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        txtUpdatePassword.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdatePassword.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdatePassword.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblUpdatePassword.setText("Password");

        lblUpdateConfirmPass.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateConfirmPass.setText("Password confirmation");

        txtUpdateConfirmPass.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        cmbUpdateSpecialty.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbUpdateSpecialty.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one", "General Medicine", "Cardiology", "Pediatrics", "Neurology", "Traumatology & Orthopedics", "Gynecology & Obstetrics", "Dermatology", "Psychiatry", "Oncology", "Ophthalmology", "Internal Medicine" }));

        btnSaveProfile.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnSaveProfile.setText("Save");
        btnSaveProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveProfileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelProfileLayout = new javax.swing.GroupLayout(panelProfile);
        panelProfile.setLayout(panelProfileLayout);
        panelProfileLayout.setHorizontalGroup(
            panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProfileLayout.createSequentialGroup()
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(211, 211, 211)
                        .addComponent(lblUpdateFirstname)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtUpdateFirstname, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblUpdateLastname)
                        .addGap(18, 18, 18)
                        .addComponent(txtUpdateLastname, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblUpdateSpecialty)
                        .addGap(18, 18, 18)
                        .addComponent(cmbUpdateSpecialty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(351, 351, 351)
                        .addComponent(lblUpdateLicense)
                        .addGap(18, 18, 18)
                        .addComponent(txtUpdateLicense, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblUpdateOffice)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtUpdateUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(558, 558, 558)
                        .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtUpdatePassword, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtUpdateOffice, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                                .addComponent(lblUpdateUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblUpdatePassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(521, 521, 521)
                        .addComponent(lblUpdateConfirmPass))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(576, 576, 576)
                        .addComponent(btnSaveProfile))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(561, 561, 561)
                        .addComponent(txtUpdateConfirmPass, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(269, Short.MAX_VALUE))
        );
        panelProfileLayout.setVerticalGroup(
            panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProfileLayout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdateFirstname)
                    .addComponent(txtUpdateFirstname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateLastname)
                    .addComponent(txtUpdateLastname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateSpecialty)
                    .addComponent(cmbUpdateSpecialty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdateLicense)
                    .addComponent(txtUpdateLicense, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtUpdateUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateOffice))
                .addGap(30, 30, 30)
                .addComponent(lblUpdateUser)
                .addGap(18, 18, 18)
                .addComponent(txtUpdateOffice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblUpdatePassword)
                .addGap(27, 27, 27)
                .addComponent(txtUpdatePassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblUpdateConfirmPass)
                .addGap(18, 18, 18)
                .addComponent(txtUpdateConfirmPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(btnSaveProfile)
                .addContainerGap(161, Short.MAX_VALUE))
        );

        tabsDoctor.addTab("Modify info", panelProfile);

        lblAcceptApptId.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblAcceptApptId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAcceptApptId.setText("Appointment ID");

        lblAcceptApptSection.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblAcceptApptSection.setText("Accept medical appointment");

        cmbAcceptAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbAcceptAppt.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        sepVertical1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        btnAcceptAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnAcceptAppt.setText("Accept");
        btnAcceptAppt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptApptActionPerformed(evt);
            }
        });

        lblRescheduleSection.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblRescheduleSection.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRescheduleSection.setText("Reschedule medical appointment");

        lblRescheduleAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblRescheduleAppt.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRescheduleAppt.setText("Appointment");

        cmbRescheduleAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbRescheduleAppt.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        btnConfirmReschedule.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnConfirmReschedule.setText("Accept");
        btnConfirmReschedule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmRescheduleActionPerformed(evt);
            }
        });

        lblRescheduleTime.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblRescheduleTime.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRescheduleTime.setText("New time appointment");

        txtRescheduleDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblRescheduleReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblRescheduleReason.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRescheduleReason.setText("Reason for appointment");

        txtRescheduleTime.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        sepVertical2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        lblCompleteApptSection.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteApptSection.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteApptSection.setText("Complete medical appointment");

        lblCompleteApptId.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteApptId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteApptId.setText("Appointment");

        cmbCompleteApptSelector.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbCompleteApptSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        lblCompleteDiagnosis.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteDiagnosis.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteDiagnosis.setText("Diagnosis");

        lblCompleteObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteObservations.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteObservations.setText("Observations");

        lblCompleteTreatment.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteTreatment.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteTreatment.setText("Recommended treatment");

        lblCompleteFollowUp.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCompleteFollowUp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCompleteFollowUp.setText("Follow-up indication");

        btnCompleteAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnCompleteAppt.setText("Complete");
        btnCompleteAppt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteApptActionPerformed(evt);
            }
        });

        lblHospSection.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospSection.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospSection.setText("Hospitalization");

        lblHospReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospReason.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospReason.setText("Reason for hospitalization");

        lblHospDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospDate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospDate.setText("Date of entry");

        txtHospitalizationEntryDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblHospDuration.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospDuration.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospDuration.setText("Estimated duration");

        txtHospitalizationEstDuration.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblHospObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospObservations.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospObservations.setText("Observations");

        txtAreaHospitalizationObservations.setColumns(20);
        txtAreaHospitalizationObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaHospitalizationObservations.setRows(5);
        scrollHospObservations.setViewportView(txtAreaHospitalizationObservations);

        btnGenerateHosp.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnGenerateHosp.setText("Generate");
        btnGenerateHosp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerateHospActionPerformed(evt);
            }
        });

        cmbCompleteHosp.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbCompleteHosp.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        radioRequestsFilter.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioRequestsFilter.setText("Requests");

        radioPatientFilter.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioPatientFilter.setText("Patient ID");

        txtAreaCompleteDiagnosis.setColumns(20);
        txtAreaCompleteDiagnosis.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaCompleteDiagnosis.setRows(5);
        scrollDiagnosis.setViewportView(txtAreaCompleteDiagnosis);

        txtAreaCompleteObservations.setColumns(20);
        txtAreaCompleteObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaCompleteObservations.setRows(5);
        scrollObservations.setViewportView(txtAreaCompleteObservations);

        txtAreaCompleteTreatment.setColumns(20);
        txtAreaCompleteTreatment.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaCompleteTreatment.setRows(5);
        scrollTreatment.setViewportView(txtAreaCompleteTreatment);

        txtAreaCompleteFollowUp.setColumns(20);
        txtAreaCompleteFollowUp.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaCompleteFollowUp.setRows(5);
        scrollFollowUp.setViewportView(txtAreaCompleteFollowUp);

        sepVertical3.setOrientation(javax.swing.SwingConstants.VERTICAL);

        btnCancelHospitalization.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnCancelHospitalization.setText("Cancel");
        btnCancelHospitalization.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelHospitalizationActionPerformed(evt);
            }
        });

        cmbHospitalizeApptSelector.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbHospitalizeApptSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        txtAreaHospitalizationReason.setColumns(20);
        txtAreaHospitalizationReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaHospitalizationReason.setRows(5);
        scrollHospReason.setViewportView(txtAreaHospitalizationReason);

        javax.swing.GroupLayout panelAppointmentActionsLayout = new javax.swing.GroupLayout(panelAppointmentActions);
        panelAppointmentActions.setLayout(panelAppointmentActionsLayout);
        panelAppointmentActionsLayout.setHorizontalGroup(
            panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(26, 26, 26)
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                        .addComponent(btnAcceptAppt)
                                        .addGap(87, 87, 87))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                        .addComponent(cmbAcceptAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(67, 67, 67))))
                            .addComponent(lblAcceptApptId, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(sepVertical1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1))
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(lblAcceptApptSection)
                        .addGap(22, 22, 22)))
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblRescheduleSection, javax.swing.GroupLayout.PREFERRED_SIZE, 306, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblRescheduleAppt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblRescheduleTime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblRescheduleReason, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 303, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                            .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                    .addGap(90, 90, 90)
                                    .addComponent(cmbRescheduleAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                    .addGap(99, 99, 99)
                                    .addComponent(txtRescheduleDate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                    .addGap(98, 98, 98)
                                    .addComponent(txtRescheduleTime, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                    .addGap(112, 112, 112)
                                    .addComponent(btnConfirmReschedule)))
                            .addGap(91, 91, 91))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sepVertical2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGap(112, 112, 112)
                        .addComponent(btnCompleteAppt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(lblCompleteApptId, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lblCompleteApptSection, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                        .addGap(99, 99, 99)
                                        .addComponent(cmbCompleteApptSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 25, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblCompleteDiagnosis, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblCompleteObservations, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(lblCompleteFollowUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblCompleteTreatment, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                        .addGap(42, 42, 42)
                                        .addComponent(scrollDiagnosis, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                        .addGap(41, 41, 41)
                                        .addComponent(scrollObservations, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                        .addGap(42, 42, 42)
                                        .addComponent(scrollTreatment, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                        .addGap(43, 43, 43)
                                        .addComponent(scrollFollowUp, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addComponent(sepVertical3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(7, 7, 7)
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblHospSection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHospDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHospDuration, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHospObservations, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(121, 121, 121)
                                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtHospitalizationEntryDate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtHospitalizationEstDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addComponent(btnCancelHospitalization)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnGenerateHosp))
                            .addComponent(scrollHospObservations, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(56, Short.MAX_VALUE))
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(cmbCompleteHosp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(37, 37, 37)
                                .addComponent(radioRequestsFilter)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                .addComponent(radioPatientFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(19, 19, 19))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                                .addComponent(cmbHospitalizeApptSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(29, 29, 29))))
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblHospReason, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAppointmentActionsLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(scrollHospReason, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47))))
        );
        panelAppointmentActionsLayout.setVerticalGroup(
            panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sepVertical1)
            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sepVertical2)
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(lblCompleteApptSection)
                        .addGap(10, 10, 10)
                        .addComponent(lblCompleteApptId)
                        .addGap(18, 18, 18)
                        .addComponent(cmbCompleteApptSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblCompleteDiagnosis)
                        .addGap(18, 18, 18)
                        .addComponent(scrollDiagnosis, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblCompleteObservations)
                        .addGap(18, 18, 18)
                        .addComponent(scrollObservations, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblCompleteTreatment)
                        .addGap(18, 18, 18)
                        .addComponent(scrollTreatment, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblCompleteFollowUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollFollowUp, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCompleteAppt)
                        .addGap(12, 12, 12))
                    .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                        .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addComponent(lblAcceptApptSection)
                                .addGap(18, 18, 18)
                                .addComponent(lblAcceptApptId)
                                .addGap(18, 18, 18)
                                .addComponent(cmbAcceptAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(31, 31, 31)
                                .addComponent(btnAcceptAppt))
                            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addComponent(lblRescheduleSection)
                                .addGap(18, 18, 18)
                                .addComponent(lblRescheduleAppt)
                                .addGap(18, 18, 18)
                                .addComponent(cmbRescheduleAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblRescheduleTime)
                                .addGap(18, 18, 18)
                                .addComponent(txtRescheduleDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblRescheduleReason)
                                .addGap(18, 18, 18)
                                .addComponent(txtRescheduleTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(btnConfirmReschedule)))
                        .addGap(18, 18, Short.MAX_VALUE))))
            .addGroup(panelAppointmentActionsLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(lblHospSection)
                .addGap(18, 18, 18)
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioRequestsFilter)
                    .addComponent(radioPatientFilter))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbCompleteHosp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbHospitalizeApptSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(lblHospReason)
                .addGap(16, 16, 16)
                .addComponent(scrollHospReason, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblHospDate)
                .addGap(18, 18, 18)
                .addComponent(txtHospitalizationEntryDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblHospDuration)
                .addGap(18, 18, 18)
                .addComponent(txtHospitalizationEstDuration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblHospObservations)
                .addGap(18, 18, 18)
                .addComponent(scrollHospObservations, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(panelAppointmentActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGenerateHosp)
                    .addComponent(btnCancelHospitalization))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(sepVertical3, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        tabsDoctor.addTab("Request/Appointments", panelAppointmentActions);

        lblPrescribeApptLabel.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeApptLabel.setText("Appointment ID");

        lblPrescribeMedName.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeMedName.setText("Medication name");

        txtPrescribeMedication.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblPrescribeDose.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeDose.setText("Dose");

        txtPrescribeDosage.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblPrescribeRoute.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeRoute.setText("Administration route");

        txtPrescribeRoute.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblPrescribeFrequency.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeFrequency.setText("Frecuency");

        txtPrescribeFrequency.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblPrescribeDuration.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeDuration.setText("Treatment duration");

        txtPrescribeDuration.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblPrescribeAdditional.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblPrescribeAdditional.setText("Additional instructions");

        txtPrescribeAdditional.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        tableMedications.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Appointment ID", "Medication name", "Dose", "Administration route", "Treatment duration", "Additional instructions", "Frecuency"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scrollMedications.setViewportView(tableMedications);

        btnAddMedication.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnAddMedication.setText("Add");
        btnAddMedication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddMedicationActionPerformed(evt);
            }
        });

        btnPrescribe.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnPrescribe.setText("Prescribe");
        btnPrescribe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrescribeActionPerformed(evt);
            }
        });

        cmbPrescribeAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbPrescribeAppt.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        javax.swing.GroupLayout panelPrescriptionsLayout = new javax.swing.GroupLayout(panelPrescriptions);
        panelPrescriptions.setLayout(panelPrescriptionsLayout);
        panelPrescriptionsLayout.setHorizontalGroup(
            panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                        .addGap(62, 62, 62)
                        .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(scrollMedications, javax.swing.GroupLayout.PREFERRED_SIZE, 1125, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                                .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                                        .addComponent(lblPrescribeApptLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cmbPrescribeAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(9, 9, 9)
                                        .addComponent(lblPrescribeMedName))
                                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                                        .addComponent(lblPrescribeDuration)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtPrescribeDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                                        .addComponent(lblPrescribeAdditional)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtPrescribeAdditional, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblPrescribeFrequency)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtPrescribeFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                                        .addComponent(txtPrescribeMedication, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblPrescribeDose)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtPrescribeDosage, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblPrescribeRoute)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(txtPrescribeRoute, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAddMedication))))
                    .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                        .addGap(583, 583, 583)
                        .addComponent(btnPrescribe)))
                .addContainerGap(108, Short.MAX_VALUE))
        );
        panelPrescriptionsLayout.setVerticalGroup(
            panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPrescriptionsLayout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPrescribeApptLabel)
                    .addComponent(lblPrescribeMedName)
                    .addComponent(txtPrescribeMedication, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPrescribeDose)
                    .addComponent(txtPrescribeDosage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPrescribeRoute)
                    .addComponent(txtPrescribeRoute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddMedication)
                    .addComponent(cmbPrescribeAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelPrescriptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPrescribeDuration)
                    .addComponent(txtPrescribeDuration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPrescribeAdditional)
                    .addComponent(txtPrescribeAdditional, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPrescribeFrequency)
                    .addComponent(txtPrescribeFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(scrollMedications, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47)
                .addComponent(btnPrescribe)
                .addContainerGap(64, Short.MAX_VALUE))
        );

        tabsDoctor.addTab("Prescribe medications", panelPrescriptions);

        javax.swing.GroupLayout panelRound1Layout = new javax.swing.GroupLayout(panelRound1);
        panelRound1.setLayout(panelRound1Layout);
        panelRound1Layout.setHorizontalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound1Layout.createSequentialGroup()
                .addGroup(panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelRound2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabsDoctor))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        panelRound1Layout.setVerticalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound1Layout.createSequentialGroup()
                .addComponent(panelRound2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabsDoctor))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelRound1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelRound1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void panelRound2MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelRound2MousePressed
        x = evt.getX();
        y = evt.getY();
    }//GEN-LAST:event_panelRound2MousePressed

    private void panelRound2MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelRound2MouseDragged
        this.setLocation(this.getLocation().x + evt.getX() - x, this.getLocation().y + evt.getY() - y);
    }//GEN-LAST:event_panelRound2MouseDragged

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        System.exit(0);
    }//GEN-LAST:event_btnCloseActionPerformed

    private void radioPendingApptsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioPendingApptsActionPerformed
        radioAllAppts.setSelected(false);
        refreshAppointmentTable(true);
    }//GEN-LAST:event_radioPendingApptsActionPerformed

    private void btnSaveProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveProfileActionPerformed
        String firstname = txtUpdateFirstname.getText();
        String lastname = txtUpdateLastname.getText();
        String password = txtUpdatePassword.getText();
        String confirmPassword = txtUpdateConfirmPass.getText();
        String username = txtUpdateUsername.getText();
        String assignedOffice = txtUpdateOffice.getText();
        String licenseNumber = txtUpdateLicense.getText();
        String specialtyStr = cmbUpdateSpecialty.getItemAt(cmbUpdateSpecialty.getSelectedIndex());
        
        Response response = doctorController.updateDoctor(
            doctorId, username, firstname, lastname, password,
            confirmPassword, specialtyStr, licenseNumber, assignedOffice
        );
        
        if (response.getStatus() == Status.OK) {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Ã‰xito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSaveProfileActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        LoginView login = new LoginView(database);
        database.removeObserver(this);
        this.dispose();
        login.setVisible(true);
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        AdminView admin = new AdminView(database, loggedUserId);
        database.removeObserver(this);
        this.dispose();
        admin.setVisible(true);
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnCancelHospitalizationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelHospActionPerformed
        // Limpiar campos de la sección de hospitalización directamente
        txtHospitalizationEntryDate.setText("");
        txtHospitalizationEstDuration.setText("");
        txtAreaHospitalizationReason.setText("");
        txtAreaHospitalizationObservations.setText("");
        cmbCompleteHosp.setSelectedIndex(0);
        cmbHospitalizeApptSelector.setSelectedIndex(0);
    }//GEN-LAST:event_btnCancelHospActionPerformed

    private void btnGenerateHospActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateHospActionPerformed
        if (cmbHospitalizeApptSelector.getSelectedIndex() <= 0 || cmbCompleteHosp.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita y un tipo de habitacion.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String appointmentId = cmbHospitalizeApptSelector.getItemAt(cmbHospitalizeApptSelector.getSelectedIndex());
        
        Response response = hospitalizationController.hospitalizeFromAppointment(
            appointmentId,
            txtHospitalizationEntryDate.getText(),
            txtAreaHospitalizationReason.getText(),
            cmbCompleteHosp.getItemAt(cmbCompleteHosp.getSelectedIndex()),
            txtAreaHospitalizationObservations.getText()
        );
        if (response.getStatus() == Status.OK) {
            txtHospitalizationEntryDate.setText("");
            txtAreaHospitalizationReason.setText("");
            txtAreaHospitalizationObservations.setText("");
        }
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Exito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnGenerateHospActionPerformed

    @SuppressWarnings("unchecked")
    private void btnSearchPatientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchPatientActionPerformed
        if (cmbSearchPatient.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long selectedPatientId = Long.parseLong(cmbSearchPatient.getItemAt(cmbSearchPatient.getSelectedIndex()).split(" - ")[0]);
        DefaultTableModel model = (DefaultTableModel) tablePatientHistory.getModel();
        model.setRowCount(0);
        
        Response resp = appointmentController.getPatientAppointments(selectedPatientId);
        if (resp.getStatus() == Status.OK && resp.getData() instanceof List) {
            List<Map<String, String>> appts = (List<Map<String, String>>) resp.getData();
            for (Map<String, String> a : appts) {
                model.addRow(new Object[]{
                    a.get("id"),
                    a.get("datetime"),
                    a.get("doctorName"),
                    a.get("specialty"),
                    a.get("type"),
                    a.get("status")
                });
            }
        }
    }//GEN-LAST:event_btnSearchPatientActionPerformed

    private void radioAllApptsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioAllApptsActionPerformed
        radioPendingAppts.setSelected(false);
        refreshAppointmentTable(false);
    }//GEN-LAST:event_radioAllApptsActionPerformed

    private void btnAcceptApptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptApptActionPerformed
        if (cmbAcceptAppt.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String appointmentId = cmbAcceptAppt.getItemAt(cmbAcceptAppt.getSelectedIndex());
        Response response = appointmentController.acceptAppointment(appointmentId);
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Exito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnAcceptApptActionPerformed

    private void btnCompleteApptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteApptActionPerformed
        if (cmbCompleteApptSelector.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String idAppointment = cmbCompleteApptSelector.getItemAt(cmbCompleteApptSelector.getSelectedIndex());
        String diagnosis = txtAreaCompleteDiagnosis.getText();
        String observations = txtAreaCompleteObservations.getText();
        String recommendedTrea = txtAreaCompleteTreatment.getText();
        String followUp = txtAreaCompleteFollowUp.getText();
        
        Response response = appointmentController.completeAppointment(idAppointment, diagnosis, observations, recommendedTrea, followUp);
        if (response.getStatus() == Status.OK) {
            txtAreaCompleteDiagnosis.setText("");
            txtAreaCompleteObservations.setText("");
            txtAreaCompleteTreatment.setText("");
            txtAreaCompleteFollowUp.setText("");
        }
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Exito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnCompleteApptActionPerformed

    private void btnPrescribeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrescribeActionPerformed
        DefaultTableModel model = (DefaultTableModel) tableMedications.getModel();
        int rowCount = model.getRowCount();
        
        // Contar filas válidas (no nulas)
        int validRows = 0;
        for (int i = 0; i < rowCount; i++) {
            if (model.getValueAt(i, 0) != null) {
                validRows++;
            }
        }

        if (validRows == 0) {
            JOptionPane.showMessageDialog(this, "No hay medicamentos válidos agregados para prescribir.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        java.util.List<Integer> rowsToRemove = new java.util.ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            Object apptIdObj = model.getValueAt(i, 0);
            Object medNameObj = model.getValueAt(i, 1);
            if (apptIdObj == null || medNameObj == null) {
                continue;
            }
            String appointmentId = apptIdObj.toString().trim();
            String medicationName = medNameObj.toString().trim();
            if (appointmentId.isEmpty() || medicationName.isEmpty()) {
                continue;
            }

            String doseStr = model.getValueAt(i, 2).toString().trim();
            String administrationRoute = model.getValueAt(i, 3).toString().trim();
            String treatmentDurationStr = model.getValueAt(i, 4).toString().trim();
            String additionalInstructions = model.getValueAt(i, 5).toString().trim();
            String frequencyStr = model.getValueAt(i, 6).toString().trim();

            Response response = appointmentController.prescribeMedication(
                appointmentId, medicationName, doseStr, administrationRoute,
                treatmentDurationStr, additionalInstructions, frequencyStr
            );

            if (response.getStatus() == Status.OK) {
                successCount++;
                rowsToRemove.add(i);
            } else {
                failCount++;
                errorMessages.append("Med: ").append(medicationName).append(" - ").append(response.getMessage()).append("\n");
            }
        }

        // Eliminar filas exitosas de la tabla en orden inverso
        for (int i = rowsToRemove.size() - 1; i >= 0; i--) {
            model.removeRow(rowsToRemove.get(i));
        }

        if (failCount == 0) {
            JOptionPane.showMessageDialog(this, "Todas las prescripciones (" + successCount + ") fueron agregadas con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Se agregaron " + successCount + " prescripciones con éxito.\n" +
                "Fallaron " + failCount + " prescripciones:\n" + errorMessages.toString(),
                "Resultado de la Prescripción", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnPrescribeActionPerformed

    private void btnAddMedicationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRescheduleApptActionPerformed
        // Este botón se llama 'Add' y está en el panel de prescripciones (Prescribe medications).
        // Debe validar y agregar los datos del medicamento a la tabla tableMedications antes de prescribir.
        if (cmbPrescribeAppt.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String appointmentId = cmbPrescribeAppt.getItemAt(cmbPrescribeAppt.getSelectedIndex());
        String medicationName = txtPrescribeMedication.getText().trim();
        String dose = txtPrescribeDosage.getText().trim();
        String route = txtPrescribeRoute.getText().trim();
        String duration = txtPrescribeDuration.getText().trim(); // En el diseño este campo tiene la duración
        String additional = txtPrescribeAdditional.getText().trim();
        String frequency = txtPrescribeFrequency.getText().trim(); // En el diseño este campo tiene la frecuencia

        if (medicationName.isEmpty() || dose.isEmpty() || route.isEmpty() || duration.isEmpty() || frequency.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor complete todos los campos del medicamento.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tableMedications.getModel();
        // Si la primera fila tiene datos nulos (por defecto en NetBeans), limpiamos la tabla antes de agregar
        if (model.getRowCount() > 0 && model.getValueAt(0, 0) == null) {
            model.setRowCount(0);
        }

        model.addRow(new Object[]{
            appointmentId,
            medicationName,
            dose,
            route,
            duration,
            additional,
            frequency
        });

        // Limpiar campos temporales de texto de medicamentos
        txtPrescribeMedication.setText("");
        txtPrescribeDosage.setText("");
        txtPrescribeRoute.setText("");
        txtPrescribeFrequency.setText("");
        txtPrescribeDuration.setText("");
        txtPrescribeAdditional.setText("");
    }//GEN-LAST:event_btnRescheduleApptActionPerformed

    private void btnConfirmRescheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptHospActionPerformed
        // Este botón ejecuta la acción de reprogramar cita en el panel Complete/Accept/Reschedule
        if (cmbRescheduleAppt.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita para reprogramar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String appointmentId = cmbRescheduleAppt.getItemAt(cmbRescheduleAppt.getSelectedIndex());
        String newTime = txtRescheduleDate.getText().trim(); // En el diseño este campo tiene la nueva hora
        String reason = txtRescheduleTime.getText().trim(); // En el diseño este campo tiene la razón

        Response response = appointmentController.rescheduleAppointment(appointmentId, newTime, reason);
        if (response.getStatus() == Status.OK) {
            txtRescheduleDate.setText("");
            txtRescheduleTime.setText("");
        }
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Éxito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnAcceptHospActionPerformed




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnPrescribe;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnCancelHospitalization;
    private javax.swing.JButton btnAcceptAppt;
    private javax.swing.JButton btnConfirmReschedule;
    private javax.swing.JButton btnCompleteAppt;
    private javax.swing.JButton btnGenerateHosp;
    private javax.swing.JButton btnAddMedication;
    private javax.swing.JButton btnSearchPatient;
    private javax.swing.JButton btnSaveProfile;
    private javax.swing.JComboBox<String> cmbUpdateSpecialty;
    private javax.swing.JComboBox<String> cmbAcceptAppt;
    private javax.swing.JComboBox<String> cmbRescheduleAppt;
    private javax.swing.JComboBox<String> cmbCompleteApptSelector;
    private javax.swing.JComboBox<String> cmbSearchPatient;
    private javax.swing.JComboBox<String> cmbCompleteHosp;
    private javax.swing.JComboBox<String> cmbPrescribeAppt;
    private javax.swing.JComboBox<String> cmbHospitalizeApptSelector;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblUpdatePassword;
    private javax.swing.JLabel lblUpdateConfirmPass;
    private javax.swing.JLabel lblAcceptApptSection;
    private javax.swing.JLabel lblAcceptApptId;
    private javax.swing.JLabel lblRescheduleSection;
    private javax.swing.JLabel lblRescheduleAppt;
    private javax.swing.JLabel lblRescheduleTime;
    private javax.swing.JLabel lblRescheduleReason;
    private javax.swing.JLabel lblCompleteApptSection;
    private javax.swing.JLabel lblUpdateFirstname;
    private javax.swing.JLabel lblCompleteApptId;
    private javax.swing.JLabel lblCompleteDiagnosis;
    private javax.swing.JLabel lblCompleteObservations;
    private javax.swing.JLabel lblCompleteTreatment;
    private javax.swing.JLabel lblCompleteFollowUp;
    private javax.swing.JLabel lblHospSection;
    private javax.swing.JLabel lblHospReason;
    private javax.swing.JLabel lblHospDate;
    private javax.swing.JLabel lblHospDuration;
    private javax.swing.JLabel lblUpdateLastname;
    private javax.swing.JLabel lblHospObservations;
    private javax.swing.JLabel lblPrescribeApptLabel;
    private javax.swing.JLabel lblPrescribeMedName;
    private javax.swing.JLabel lblPrescribeDose;
    private javax.swing.JLabel lblPrescribeRoute;
    private javax.swing.JLabel lblPrescribeFrequency;
    private javax.swing.JLabel lblPrescribeDuration;
    private javax.swing.JLabel lblPrescribeAdditional;
    private javax.swing.JLabel lblSearchPatient;
    private javax.swing.JLabel lblUpdateSpecialty;
    private javax.swing.JLabel lblUpdateLicense;
    private javax.swing.JLabel lblUpdateOffice;
    private javax.swing.JLabel lblUpdateUser;
    private javax.swing.JPanel panelAppointmentActions;
    private javax.swing.JPanel panelPrescriptions;
    private javax.swing.JPanel panelProfile;
    private javax.swing.JPanel panelAppointmentTable;
    private javax.swing.JPanel panelPatientHistory;
    private javax.swing.JRadioButton radioAllAppts;
    private javax.swing.JRadioButton radioPendingAppts;
    private javax.swing.JRadioButton radioRequestsFilter;
    private javax.swing.JRadioButton radioPatientFilter;
    private javax.swing.JScrollPane scrollHospObservations;
    private javax.swing.JScrollPane scrollHospReason;
    private javax.swing.JScrollPane scrollMedications;
    private javax.swing.JScrollPane scrollAppointments;
    private javax.swing.JScrollPane scrollPatientHistory;
    private javax.swing.JScrollPane scrollDiagnosis;
    private javax.swing.JScrollPane scrollObservations;
    private javax.swing.JScrollPane scrollTreatment;
    private javax.swing.JScrollPane scrollFollowUp;
    private javax.swing.JSeparator sepVertical1;
    private javax.swing.JSeparator sepVertical2;
    private javax.swing.JSeparator sepVertical3;
    private javax.swing.JTabbedPane tabsDoctor;
    private javax.swing.JTable tableMedications;
    private javax.swing.JTable tableAppointments;
    private javax.swing.JTable tablePatientHistory;
    private javax.swing.JTextArea txtAreaHospitalizationObservations;
    private javax.swing.JTextArea txtAreaCompleteDiagnosis;
    private javax.swing.JTextArea txtAreaCompleteObservations;
    private javax.swing.JTextArea txtAreaCompleteTreatment;
    private javax.swing.JTextArea txtAreaCompleteFollowUp;
    private javax.swing.JTextArea txtAreaHospitalizationReason;
    private javax.swing.JTextField txtUpdateFirstname;
    private javax.swing.JTextField txtUpdateConfirmPass;
    private javax.swing.JTextField txtRescheduleDate;
    private javax.swing.JTextField txtRescheduleTime;
    private javax.swing.JTextField txtUpdateLastname;
    private javax.swing.JTextField txtHospitalizationEntryDate;
    private javax.swing.JTextField txtHospitalizationEstDuration;
    private javax.swing.JTextField txtPrescribeMedication;
    private javax.swing.JTextField txtPrescribeDosage;
    private javax.swing.JTextField txtPrescribeRoute;
    private javax.swing.JTextField txtPrescribeFrequency;
    private javax.swing.JTextField txtPrescribeDuration;
    private javax.swing.JTextField txtPrescribeAdditional;
    private javax.swing.JTextField txtUpdateLicense;
    private javax.swing.JTextField txtUpdateOffice;
    private javax.swing.JTextField txtUpdateUsername;
    private javax.swing.JTextField txtUpdatePassword;
    private core.views.PanelRound panelRound1;
    private core.views.PanelRound panelRound2;
    // End of variables declaration//GEN-END:variables
}


