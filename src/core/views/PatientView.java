/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package core.views;

import core.controllers.utils.Response;
import core.controllers.utils.Status;
import core.controllers.patient.PatientController;
import core.controllers.patient.IPatientController;
import core.controllers.hospitalization.HospitalizationController;
import core.controllers.hospitalization.IHospitalizationController;
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
public class PatientView extends javax.swing.JFrame implements Observer {

    private int x, y;
    private final IPatientController patientController;
    private final IHospitalizationController hospitalizationController;
    private final IAppointmentController appointmentController;
    private final long loggedUserId;
    private final long patientId;
    private final boolean isAdminAccess;
    // Database reference kept only for Observer subscription and navigation
    private final IStorage database;

    public PatientView(IStorage database, long loggedUserId, long patientId, boolean isAdminAccess) {
        this.database = database;
        this.loggedUserId = loggedUserId;
        this.patientId = patientId;
        this.isAdminAccess = isAdminAccess;
        this.patientController = new PatientController(database);
        this.hospitalizationController = new HospitalizationController(database);
        this.appointmentController = new AppointmentController(database);
        initComponents();
        btnBack.setVisible(isAdminAccess);
        this.setBackground(new Color(0, 0, 0, 0));
        this.setLocationRelativeTo(null);
        database.addObserver(this);
        
        cmbApptSpecialty.addActionListener(e -> {
            String selectedSpec = (String) cmbApptSpecialty.getSelectedItem();
            if (selectedSpec == null) return;
            cmbApptDoctor.removeAllItems();
            cmbApptDoctor.addItem("Select one");
            Response doctorResp = patientController.getDoctorList();
            if (doctorResp.getStatus() == Status.OK && doctorResp.getData() instanceof List) {
                List<Map<String, String>> doctors = (List<Map<String, String>>) doctorResp.getData();
                for (Map<String, String> d : doctors) {
                    if (selectedSpec.equals("Select one") || selectedSpec.equals(d.get("specialty"))) {
                        cmbApptDoctor.addItem(d.get("id"));
                    }
                }
            }
        });
        
        loadPatientData();
        loadDoctorComboBoxes();
        refreshAppointmentTable();
    }

    @Override
    public void update(EventType event, Object data) {
        if (event == EventType.APPOINTMENT_ADDED || event == EventType.APPOINTMENT_UPDATED) {
            refreshAppointmentTable();
        }
        if (event == EventType.USER_ADDED || event == EventType.USER_UPDATED) {
            loadDoctorComboBoxes();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPatientData() {
        Response resp = patientController.getPatientData(patientId);
        if (resp.getStatus() == Status.OK && resp.getData() instanceof Map) {
            Map<String, String> data = (Map<String, String>) resp.getData();
            txtUpdateFirstname.setText(data.getOrDefault("firstname", ""));
            txtUpdateLastname.setText(data.getOrDefault("lastname", ""));
            txtUpdateBirthdate.setText(data.getOrDefault("birthdate", ""));
            txtUpdateEmail.setText(data.getOrDefault("email", ""));
            txtUpdatePhone.setText(data.getOrDefault("phone", ""));
            txtUpdateAddress.setText(data.getOrDefault("address", ""));
            txtUpdateUsername.setText(data.getOrDefault("username", ""));
            boolean isFemale = "true".equals(data.getOrDefault("gender", "false"));
            cmbUpdateGender.setSelectedIndex(isFemale ? 2 : 1);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDoctorComboBoxes() {
        // Temporarily remove listener to avoid triggering it while loading
        java.awt.event.ActionListener[] listeners = cmbApptSpecialty.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) {
            cmbApptSpecialty.removeActionListener(l);
        }

        cmbApptSpecialty.removeAllItems();
        cmbApptSpecialty.addItem("Select one");
        cmbApptDoctor.removeAllItems();
        cmbApptDoctor.addItem("Select one");
        
        // Get doctor list from controller (no direct DB access)
        Response doctorResp = patientController.getDoctorList();
        if (doctorResp.getStatus() == Status.OK && doctorResp.getData() instanceof List) {
            List<Map<String, String>> doctors = (List<Map<String, String>>) doctorResp.getData();
            java.util.Set<String> specialties = new java.util.HashSet<>();
            for (Map<String, String> d : doctors) {
                specialties.add(d.get("specialty"));
                cmbApptDoctor.addItem(d.get("id"));
            }
            for (String s : specialties) {
                if (s != null && !s.isEmpty()) {
                    cmbApptSpecialty.addItem(s);
                }
            }
        }
        
        for (java.awt.event.ActionListener l : listeners) {
            cmbApptSpecialty.addActionListener(l);
        }
        
        // Room types
        // Room types
        cmbHospRoom.removeAllItems();
        cmbHospRoom.addItem("Select one");
        for (String rt : new String[]{"ICU", "IMC", "GENERAL", "PEDIATRICS", "MATERNITY", "ONCOLOGY"}) {
            cmbHospRoom.addItem(rt);
        }
        // Refresh cancel combo
        refreshCancelCombo();
    }

    @SuppressWarnings("unchecked")
    private void refreshCancelCombo() {
        cmbCancelAppt.removeAllItems();
        cmbCancelAppt.addItem("Select one");
        Response apptResp = appointmentController.getPatientAppointments(patientId);
        if (apptResp.getStatus() == Status.OK && apptResp.getData() instanceof List) {
            List<Map<String, String>> appts = (List<Map<String, String>>) apptResp.getData();
            for (Map<String, String> a : appts) {
                String status = a.getOrDefault("status", "");
                if (!"CANCELED".equals(status) && !"COMPLETED".equals(status)) {
                    cmbCancelAppt.addItem(a.get("id"));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshAppointmentTable() {
        Response resp = appointmentController.getPatientAppointments(patientId);
        if (resp.getStatus() == Status.OK && resp.getData() instanceof List) {
            List<Map<String, String>> appointments = (List<Map<String, String>>) resp.getData();
            DefaultTableModel model = (DefaultTableModel) tableAppointments.getModel();
            model.setRowCount(0);
            for (Map<String, String> a : appointments) {
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
        // Refresh cancel combo through controller too
        refreshCancelCombo();
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
        tabsMain = new javax.swing.JTabbedPane();
        panelApptHistory = new javax.swing.JPanel();
        scrollAppointments = new javax.swing.JScrollPane();
        tableAppointments = new javax.swing.JTable();
        btnRefresh = new javax.swing.JButton();
        btnLogout = new javax.swing.JButton();
        panelProfile = new javax.swing.JPanel();
        lblUpdateFirstname = new javax.swing.JLabel();
        txtUpdateFirstname = new javax.swing.JTextField();
        lblUpdateLastname = new javax.swing.JLabel();
        txtUpdateLastname = new javax.swing.JTextField();
        lblUpdateBirthdate = new javax.swing.JLabel();
        txtUpdateBirthdate = new javax.swing.JTextField();
        lblUpdateGender = new javax.swing.JLabel();
        lblUpdateEmail = new javax.swing.JLabel();
        txtUpdateEmail = new javax.swing.JTextField();
        lblUpdatePhone = new javax.swing.JLabel();
        txtUpdatePhone = new javax.swing.JTextField();
        lblUpdateAddress = new javax.swing.JLabel();
        txtUpdateAddress = new javax.swing.JTextField();
        txtUpdatePassword = new javax.swing.JTextField();
        lblUpdatePassword = new javax.swing.JLabel();
        lblUpdateConfirmPass = new javax.swing.JLabel();
        txtUpdateConfirmPass = new javax.swing.JTextField();
        btnSaveProfile = new javax.swing.JButton();
        lblUpdateUsername = new javax.swing.JLabel();
        txtUpdateUsername = new javax.swing.JTextField();
        cmbUpdateGender = new javax.swing.JComboBox<>();
        panelRequestCancel = new javax.swing.JPanel();
        lblApptRequestTitle = new javax.swing.JLabel();
        radioBySpecialty = new javax.swing.JRadioButton();
        radioByDoctor = new javax.swing.JRadioButton();
        sepVertical1 = new javax.swing.JSeparator();
        lblApptDate = new javax.swing.JLabel();
        txtApptDate = new javax.swing.JTextField();
        txtApptTime = new javax.swing.JTextField();
        lblApptTime = new javax.swing.JLabel();
        lblApptType = new javax.swing.JLabel();
        lblApptReason = new javax.swing.JLabel();
        cmbApptModality = new javax.swing.JComboBox<>();
        btnCreateAppt = new javax.swing.JButton();
        sepVertical2 = new javax.swing.JSeparator();
        lblHospRequestTitle = new javax.swing.JLabel();
        lblHospReason = new javax.swing.JLabel();
        lblHospDoctor = new javax.swing.JLabel();
        cmbApptDoctor = new javax.swing.JComboBox<>();
        txtHospDate = new javax.swing.JTextField();
        lblHospDate = new javax.swing.JLabel();
        lblHospRoom = new javax.swing.JLabel();
        cmbHospRoom = new javax.swing.JComboBox<>();
        lblHospObservations = new javax.swing.JLabel();
        scrollHospObservations = new javax.swing.JScrollPane();
        txtAreaHospObservations = new javax.swing.JTextArea();
        btnRequestHosp = new javax.swing.JButton();
        lblCancelTitle = new javax.swing.JLabel();
        lblCancelId = new javax.swing.JLabel();
        lblCancelObservations = new javax.swing.JLabel();
        scrollUnused = new javax.swing.JScrollPane();
        txtAreaUnused = new javax.swing.JTextArea();
        btnCancelAppt = new javax.swing.JButton();
        scrollHospReason = new javax.swing.JScrollPane();
        txtAreaHospReason = new javax.swing.JTextArea();
        scrollApptReason = new javax.swing.JScrollPane();
        txtAreaApptReason = new javax.swing.JTextArea();
        cmbCancelAppt = new javax.swing.JComboBox<>();
        cmbApptSpecialty = new javax.swing.JComboBox<>();

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
        lblTitle.setText("PATIENT VIEW");

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
                .addGap(15, 15, 15)
                .addComponent(lblTitle)
                .addGap(29, 29, 29)
                .addComponent(btnBack)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addGap(19, 19, 19))
        );
        panelRound2Layout.setVerticalGroup(
            panelRound2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRound2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnClose))
            .addGroup(panelRound2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnBack)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(lblTitle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tableAppointments.setAutoCreateRowSorter(true);
        tableAppointments.setModel(new javax.swing.table.DefaultTableModel(
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
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scrollAppointments.setViewportView(tableAppointments);

        btnRefresh.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnLogout.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnLogout.setText("Logout");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelApptHistoryLayout = new javax.swing.GroupLayout(panelApptHistory);
        panelApptHistory.setLayout(panelApptHistoryLayout);
        panelApptHistoryLayout.setHorizontalGroup(
            panelApptHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelApptHistoryLayout.createSequentialGroup()
                .addGap(62, 62, 62)
                .addComponent(scrollAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 1167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(51, Short.MAX_VALUE))
            .addGroup(panelApptHistoryLayout.createSequentialGroup()
                .addGap(602, 602, 602)
                .addComponent(btnRefresh)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnLogout)
                .addGap(78, 78, 78))
        );
        panelApptHistoryLayout.setVerticalGroup(
            panelApptHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelApptHistoryLayout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addComponent(scrollAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58)
                .addGroup(panelApptHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRefresh)
                    .addComponent(btnLogout))
                .addContainerGap(71, Short.MAX_VALUE))
        );

        tabsMain.addTab("Appointment history", panelApptHistory);

        lblUpdateFirstname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateFirstname.setText("Firstname");

        txtUpdateFirstname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateLastname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateLastname.setText("Lastname");

        txtUpdateLastname.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateBirthdate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateBirthdate.setText("Birthdate");

        txtUpdateBirthdate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateGender.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateGender.setText("Gender");

        lblUpdateEmail.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateEmail.setText("Email");

        txtUpdateEmail.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdatePhone.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdatePhone.setText("Phone");

        txtUpdatePhone.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdateAddress.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateAddress.setText("Address");

        txtUpdateAddress.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        txtUpdatePassword.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblUpdatePassword.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdatePassword.setText("Password");

        lblUpdateConfirmPass.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateConfirmPass.setText("Password confirmation");

        txtUpdateConfirmPass.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        btnSaveProfile.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnSaveProfile.setText("Save");
        btnSaveProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveProfileActionPerformed(evt);
            }
        });

        lblUpdateUsername.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblUpdateUsername.setText("User");

        txtUpdateUsername.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        cmbUpdateGender.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbUpdateGender.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one", "Female", "Male" }));

        javax.swing.GroupLayout panelProfileLayout = new javax.swing.GroupLayout(panelProfile);
        panelProfile.setLayout(panelProfileLayout);
        panelProfileLayout.setHorizontalGroup(
            panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProfileLayout.createSequentialGroup()
                .addGap(81, 81, 81)
                .addComponent(lblUpdateFirstname)
                .addGap(18, 18, 18)
                .addComponent(txtUpdateFirstname, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(lblUpdateLastname)
                .addGap(18, 18, 18)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addComponent(lblUpdatePhone)
                        .addGap(18, 18, 18)
                        .addComponent(txtUpdatePhone, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblUpdateAddress)
                        .addGap(18, 18, 18)
                        .addComponent(txtUpdateAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addComponent(txtUpdateLastname, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblUpdateBirthdate)
                        .addGap(18, 18, 18)
                        .addComponent(txtUpdateBirthdate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblUpdateGender)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbUpdateGender, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(lblUpdateEmail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                        .addComponent(txtUpdateEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(141, 141, 141))
            .addGroup(panelProfileLayout.createSequentialGroup()
                .addGap(516, 516, 516)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(btnSaveProfile))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addComponent(txtUpdateConfirmPass, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblUpdateConfirmPass)
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(lblUpdatePassword))
                    .addGroup(panelProfileLayout.createSequentialGroup()
                        .addGap(38, 38, 38)
                        .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtUpdateUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(panelProfileLayout.createSequentialGroup()
                                    .addGap(39, 39, 39)
                                    .addComponent(lblUpdateUsername)))
                            .addComponent(txtUpdatePassword, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelProfileLayout.setVerticalGroup(
            panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProfileLayout.createSequentialGroup()
                .addGap(95, 95, 95)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdateFirstname)
                    .addComponent(txtUpdateFirstname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateLastname)
                    .addComponent(txtUpdateLastname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateBirthdate)
                    .addComponent(txtUpdateBirthdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateGender)
                    .addComponent(lblUpdateEmail)
                    .addComponent(txtUpdateEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbUpdateGender, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelProfileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdatePhone)
                    .addComponent(txtUpdatePhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUpdateAddress)
                    .addComponent(txtUpdateAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(66, 66, 66)
                .addComponent(lblUpdateUsername)
                .addGap(18, 18, 18)
                .addComponent(txtUpdateUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblUpdatePassword)
                .addGap(18, 18, 18)
                .addComponent(txtUpdatePassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblUpdateConfirmPass)
                .addGap(18, 18, 18)
                .addComponent(txtUpdateConfirmPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36)
                .addComponent(btnSaveProfile)
                .addContainerGap(68, Short.MAX_VALUE))
        );

        tabsMain.addTab("Modify info", panelProfile);

        lblApptRequestTitle.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblApptRequestTitle.setText("Request medical appointment");

        radioBySpecialty.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioBySpecialty.setText("Specialty");
        radioBySpecialty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioBySpecialtyActionPerformed(evt);
            }
        });

        radioByDoctor.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        radioByDoctor.setText("Doctor");
        radioByDoctor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioByDoctorActionPerformed(evt);
            }
        });

        sepVertical1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        lblApptDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblApptDate.setText("Appointment date");

        txtApptDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        txtApptTime.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblApptTime.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblApptTime.setText("Appointment time");

        lblApptType.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblApptType.setText("Appointment type");

        lblApptReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblApptReason.setText("Appointment reason");

        cmbApptModality.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbApptModality.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one", "Remote", "In-person" }));

        btnCreateAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnCreateAppt.setText("Create");
        btnCreateAppt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateApptActionPerformed(evt);
            }
        });

        sepVertical2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        lblHospRequestTitle.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospRequestTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospRequestTitle.setText("Request hospitalization");

        lblHospReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospReason.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospReason.setText("Hospitalization reason");

        lblHospDoctor.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospDoctor.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospDoctor.setText("Attending doctor");

        cmbApptDoctor.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbApptDoctor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        txtHospDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N

        lblHospDate.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospDate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospDate.setText("Estimated date of admission");
        lblHospDate.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblHospRoom.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospRoom.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospRoom.setText("Desired room type");

        cmbHospRoom.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbHospRoom.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        lblHospObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblHospObservations.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHospObservations.setText("Observations");

        txtAreaHospObservations.setColumns(20);
        txtAreaHospObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaHospObservations.setRows(5);
        scrollHospObservations.setViewportView(txtAreaHospObservations);

        btnRequestHosp.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnRequestHosp.setText("Create");
        btnRequestHosp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRequestHospActionPerformed(evt);
            }
        });

        lblCancelTitle.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCancelTitle.setText("Cancel appointment");

        lblCancelId.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCancelId.setText("ID appointment");

        lblCancelObservations.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        lblCancelObservations.setText("Observations");

        txtAreaUnused.setColumns(20);
        txtAreaUnused.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaUnused.setRows(5);
        scrollUnused.setViewportView(txtAreaUnused);

        btnCancelAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        btnCancelAppt.setText("Cancel");
        btnCancelAppt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelApptActionPerformed(evt);
            }
        });

        txtAreaHospReason.setColumns(20);
        txtAreaHospReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaHospReason.setRows(5);
        scrollHospReason.setViewportView(txtAreaHospReason);

        txtAreaApptReason.setColumns(20);
        txtAreaApptReason.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        txtAreaApptReason.setRows(5);
        scrollApptReason.setViewportView(txtAreaApptReason);

        cmbCancelAppt.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbCancelAppt.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        cmbApptSpecialty.setFont(new java.awt.Font("Yu Gothic UI", 0, 18)); // NOI18N
        cmbApptSpecialty.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select one" }));

        javax.swing.GroupLayout panelRequestCancelLayout = new javax.swing.GroupLayout(panelRequestCancel);
        panelRequestCancel.setLayout(panelRequestCancelLayout);
        panelRequestCancelLayout.setHorizontalGroup(
            panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(panelRequestCancelLayout.createSequentialGroup()
                            .addGap(44, 44, 44)
                            .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addComponent(radioBySpecialty)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(radioByDoctor))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(63, 63, 63)
                                    .addComponent(txtApptDate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(47, 47, 47)
                                    .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(lblApptTime)
                                        .addComponent(lblApptDate)
                                        .addComponent(cmbApptSpecialty, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(63, 63, 63)
                                    .addComponent(txtApptTime, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(38, 38, 38)
                                    .addComponent(lblApptReason))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(46, 46, 46)
                                    .addComponent(lblApptType))
                                .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                    .addGap(55, 55, 55)
                                    .addComponent(cmbApptModality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(panelRequestCancelLayout.createSequentialGroup()
                            .addGap(42, 42, 42)
                            .addComponent(lblApptRequestTitle)))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(scrollApptReason, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGap(122, 122, 122)
                        .addComponent(btnCreateAppt)))
                .addGap(69, 69, 69)
                .addComponent(sepVertical1, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelRequestCancelLayout.createSequentialGroup()
                            .addGap(211, 211, 211)
                            .addComponent(btnRequestHosp))
                        .addGroup(panelRequestCancelLayout.createSequentialGroup()
                            .addGap(127, 127, 127)
                            .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(lblHospReason, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(scrollHospReason, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(lblHospRequestTitle, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                                .addComponent(lblHospDoctor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRequestCancelLayout.createSequentialGroup()
                            .addGap(127, 127, 127)
                            .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblHospObservations, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblHospDate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(scrollHospObservations, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblHospRoom, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGap(190, 190, 190)
                        .addComponent(cmbApptDoctor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGap(200, 200, 200)
                        .addComponent(txtHospDate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGap(191, 191, 191)
                        .addComponent(cmbHospRoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 126, Short.MAX_VALUE)
                .addComponent(sepVertical2, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63)
                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollUnused, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(lblCancelTitle))
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addGap(77, 77, 77)
                                .addComponent(btnCancelAppt))
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addGap(47, 47, 47)
                                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(cmbCancelAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblCancelId)))
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addGap(60, 60, 60)
                                .addComponent(lblCancelObservations)))
                        .addGap(49, 49, 49)))
                .addGap(81, 81, 81))
        );
        panelRequestCancelLayout.setVerticalGroup(
            panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sepVertical1)
            .addComponent(sepVertical2)
            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addComponent(lblHospRequestTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                        .addComponent(lblHospReason)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollHospReason, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblHospDoctor)
                        .addGap(18, 18, 18)
                        .addComponent(cmbApptDoctor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblHospDate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtHospDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(lblHospRoom)
                        .addGap(18, 18, 18)
                        .addComponent(cmbHospRoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblHospObservations)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(scrollHospObservations, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnRequestHosp)
                        .addGap(15, 15, 15))
                    .addGroup(panelRequestCancelLayout.createSequentialGroup()
                        .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addComponent(lblApptRequestTitle)
                                .addGap(18, 18, 18)
                                .addGroup(panelRequestCancelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(radioBySpecialty)
                                    .addComponent(radioByDoctor))
                                .addGap(18, 18, 18)
                                .addComponent(cmbApptSpecialty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblApptDate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtApptDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(13, 13, 13)
                                .addComponent(lblApptTime)
                                .addGap(18, 18, 18)
                                .addComponent(txtApptTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblApptReason)
                                .addGap(24, 24, 24)
                                .addComponent(scrollApptReason, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panelRequestCancelLayout.createSequentialGroup()
                                .addComponent(lblCancelTitle)
                                .addGap(39, 39, 39)
                                .addComponent(lblCancelId)
                                .addGap(18, 18, 18)
                                .addComponent(cmbCancelAppt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblCancelObservations)
                                .addGap(18, 18, 18)
                                .addComponent(scrollUnused, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(56, 56, 56)
                                .addComponent(btnCancelAppt)))
                        .addGap(18, 18, 18)
                        .addComponent(lblApptType)
                        .addGap(18, 18, 18)
                        .addComponent(cmbApptModality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(40, 40, 40)
                        .addComponent(btnCreateAppt)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        tabsMain.addTab("Request/Cancel", panelRequestCancel);

        javax.swing.GroupLayout panelRound1Layout = new javax.swing.GroupLayout(panelRound1);
        panelRound1.setLayout(panelRound1Layout);
        panelRound1Layout.setHorizontalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelRound2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(tabsMain)
        );
        panelRound1Layout.setVerticalGroup(
            panelRound1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRound1Layout.createSequentialGroup()
                .addComponent(panelRound2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabsMain))
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

    private void btnCancelApptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelApptActionPerformed
        if (cmbCancelAppt.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String appointmentId = cmbCancelAppt.getItemAt(cmbCancelAppt.getSelectedIndex());
        Response response = appointmentController.cancelAppointment(appointmentId);
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Éxito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnCancelApptActionPerformed

    private void btnSaveProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveProfileActionPerformed
        String firstname = txtUpdateFirstname.getText();
        String lastname = txtUpdateLastname.getText();
        boolean gender = (cmbUpdateGender.getSelectedIndex() == 2);
        String birth = txtUpdateBirthdate.getText();
        String address = txtUpdateAddress.getText();
        String phone = txtUpdatePhone.getText();
        String email = txtUpdateEmail.getText();
        String username = txtUpdateUsername.getText();
        String password = txtUpdatePassword.getText();
        String comPassword = txtUpdateConfirmPass.getText();
        
        Response response = patientController.updatePatient(
            patientId, username, firstname, lastname,
            password, comPassword, email, birth, gender, phone, address
        );
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Éxito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
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

    private void radioBySpecialtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioBySpecialtyActionPerformed
        if (radioByDoctor.isSelected()) {
            radioByDoctor.setSelected(false);
        }
        cmbApptSpecialty.removeAllItems();
        cmbApptSpecialty.addItem("Select one");
        // Specialty names as strings — no model import needed
        for (String spec : new String[]{
            "GENERAL MEDICINE", "CARDIOLOGY", "NEUROLOGY", "ORTHOPEDICS",
            "GYNECOLOGY", "PEDIATRICS", "DERMATOLOGY", "ONCOLOGY"
        }) {
            cmbApptSpecialty.addItem(spec);
        }
    }//GEN-LAST:event_radioBySpecialtyActionPerformed

    @SuppressWarnings("unchecked")
    private void radioByDoctorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioByDoctorActionPerformed
        if (radioBySpecialty.isSelected()) {
            radioBySpecialty.setSelected(false);
        }
        cmbApptSpecialty.removeAllItems();
        cmbApptSpecialty.addItem("Select one");
        // Use controller — no direct DB/model access
        Response resp = patientController.getDoctorList();
        if (resp.getStatus() == Status.OK && resp.getData() instanceof List) {
            List<Map<String, String>> doctors = (List<Map<String, String>>) resp.getData();
            for (Map<String, String> d : doctors) {
                cmbApptSpecialty.addItem(d.get("firstname") + " " + d.get("lastname") + " [" + d.get("id") + "]");
            }
        }
    }//GEN-LAST:event_radioByDoctorActionPerformed

    private void btnCreateApptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateApptActionPerformed
        if (cmbApptSpecialty.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un doctor o especialidad.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dateStr = txtApptDate.getText();
        String timeStr = txtApptTime.getText();
        String reason = txtAreaApptReason.getText();
        boolean inPerson = (cmbApptModality.getSelectedIndex() == 1);
        
        Response response;
        if (radioBySpecialty.isSelected()) {
            // By specialty
            String specialty = cmbApptSpecialty.getItemAt(cmbApptSpecialty.getSelectedIndex());
            response = appointmentController.requestAppointmentBySpecialty(patientId, specialty, dateStr, timeStr, reason, inPerson);
        } else {
            // By doctor - extract ID from combo text like "Name [ID]"
            String selected = cmbApptSpecialty.getItemAt(cmbApptSpecialty.getSelectedIndex());
            String idPart = selected.substring(selected.lastIndexOf("[") + 1, selected.lastIndexOf("]"));
            long doctorId = Long.parseLong(idPart);
            response = appointmentController.requestAppointmentByDoctor(patientId, doctorId, dateStr, timeStr, reason, inPerson);
        }
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Éxito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnCreateApptActionPerformed


    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        refreshAppointmentTable();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnRequestHospActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRequestHospActionPerformed
        if (cmbApptDoctor.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un doctor.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cmbHospRoom.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un tipo de habitacion.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long doctorId = Long.parseLong(cmbApptDoctor.getItemAt(cmbApptDoctor.getSelectedIndex()));
        String dateStr = txtHospDate.getText();
        String roomType = cmbHospRoom.getItemAt(cmbHospRoom.getSelectedIndex()).toUpperCase();
        String reason = txtAreaHospReason.getText();
        String observations = txtAreaHospObservations.getText();
        
        Response response = hospitalizationController.requestHospitalization(
            patientId, doctorId, dateStr, reason, roomType, observations
        );
        JOptionPane.showMessageDialog(this, response.getMessage(),
            response.getStatus() == Status.OK ? "Éxito" : "Error",
            response.getStatus() == Status.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_btnRequestHospActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCreateAppt;
    private javax.swing.JButton btnRequestHosp;
    private javax.swing.JButton btnCancelAppt;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnSaveProfile;
    private javax.swing.JComboBox<String> cmbApptModality;
    private javax.swing.JComboBox<String> cmbApptDoctor;
    private javax.swing.JComboBox<String> cmbHospRoom;
    private javax.swing.JComboBox<String> cmbCancelAppt;
    private javax.swing.JComboBox<String> cmbApptSpecialty;
    private javax.swing.JComboBox<String> cmbUpdateGender;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblUpdatePassword;
    private javax.swing.JLabel lblUpdateConfirmPass;
    private javax.swing.JLabel lblUpdateUsername;
    private javax.swing.JLabel lblApptRequestTitle;
    private javax.swing.JLabel lblApptDate;
    private javax.swing.JLabel lblApptTime;
    private javax.swing.JLabel lblApptType;
    private javax.swing.JLabel lblApptReason;
    private javax.swing.JLabel lblHospRequestTitle;
    private javax.swing.JLabel lblHospReason;
    private javax.swing.JLabel lblUpdateFirstname;
    private javax.swing.JLabel lblHospDoctor;
    private javax.swing.JLabel lblHospDate;
    private javax.swing.JLabel lblHospRoom;
    private javax.swing.JLabel lblHospObservations;
    private javax.swing.JLabel lblCancelTitle;
    private javax.swing.JLabel lblCancelId;
    private javax.swing.JLabel lblCancelObservations;
    private javax.swing.JLabel lblUpdateLastname;
    private javax.swing.JLabel lblUpdateBirthdate;
    private javax.swing.JLabel lblUpdateGender;
    private javax.swing.JLabel lblUpdateEmail;
    private javax.swing.JLabel lblUpdatePhone;
    private javax.swing.JLabel lblUpdateAddress;
    private javax.swing.JPanel panelProfile;
    private javax.swing.JPanel panelRequestCancel;
    private javax.swing.JPanel panelApptHistory;
    private javax.swing.JRadioButton radioBySpecialty;
    private javax.swing.JRadioButton radioByDoctor;
    private javax.swing.JScrollPane scrollHospObservations;
    private javax.swing.JScrollPane scrollUnused;
    private javax.swing.JScrollPane scrollAppointments;
    private javax.swing.JScrollPane scrollHospReason;
    private javax.swing.JScrollPane scrollApptReason;
    private javax.swing.JSeparator sepVertical1;
    private javax.swing.JSeparator sepVertical2;
    private javax.swing.JTabbedPane tabsMain;
    private javax.swing.JTable tableAppointments;
    private javax.swing.JTextArea txtAreaHospObservations;
    private javax.swing.JTextArea txtAreaUnused;
    private javax.swing.JTextArea txtAreaHospReason;
    private javax.swing.JTextArea txtAreaApptReason;
    private javax.swing.JTextField txtUpdateFirstname;
    private javax.swing.JTextField txtUpdateConfirmPass;
    private javax.swing.JTextField txtUpdateUsername;
    private javax.swing.JTextField txtApptDate;
    private javax.swing.JTextField txtApptTime;
    private javax.swing.JTextField txtHospDate;
    private javax.swing.JTextField txtUpdateLastname;
    private javax.swing.JTextField txtUpdateBirthdate;
    private javax.swing.JTextField txtUpdateEmail;
    private javax.swing.JTextField txtUpdatePhone;
    private javax.swing.JTextField txtUpdateAddress;
    private javax.swing.JTextField txtUpdatePassword;
    private core.views.PanelRound panelRound1;
    private core.views.PanelRound panelRound2;
    // End of variables declaration//GEN-END:variables
}


