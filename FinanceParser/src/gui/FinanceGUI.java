package gui;

import db.DBConnection;
import parser.AxisSmsParser;
import parser.BankSmsParser;
import parser.CategoryEngine;
import db.SmsDAO;
import report.ChartDataExporter;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;

public class FinanceGUI extends JFrame {

    // ── REFINED DARK PALETTE ──────────────────────────────────────
    private static final Color BG        = new Color(12, 14, 22);
    private static final Color SIDEBAR   = new Color(16, 19, 30);
    private static final Color CARD      = new Color(22, 27, 42);
    private static final Color CARD2     = new Color(30, 36, 56);
    private static final Color BORDER    = new Color(45, 55, 85);
    private static final Color ACCENT    = new Color(99, 179, 255);
    private static final Color ACCENT2   = new Color(140, 210, 255);
    private static final Color GREEN     = new Color(50, 210, 100);
    private static final Color RED       = new Color(255, 75, 65);
    private static final Color ORANGE    = new Color(255, 170, 30);
    private static final Color PURPLE    = new Color(190, 100, 240);
    private static final Color TEAL      = new Color(50, 220, 200);
    private static final Color TEXT      = new Color(225, 232, 255);
    private static final Color TEXT2     = new Color(130, 150, 200);
    private static final Color ROW1      = new Color(20, 25, 42);
    private static final Color ROW2      = new Color(26, 32, 52);
    private static final Color SEL       = new Color(55, 100, 200);
    private static final Color HDR       = new Color(14, 17, 30);

    // ── FONTS ─────────────────────────────────────────────────────
    private static final Font FT  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FH  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FB  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FS  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FM  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FBG = new Font("Segoe UI", Font.BOLD, 30);
    private static final Font FMD = new Font("Segoe UI", Font.BOLD, 14);

    // ── ACTIVE USER STATE ─────────────────────────────────────────
    private int    activeUserId   = 1;
    private String activeUserName = "Aryan Sharma";
    private JLabel userBadge;
    private JLabel userCardLabel;

    // ── UI STATE ──────────────────────────────────────────────────
    private JPanel   contentArea;
    private JLabel   statusLabel, clockLabel;
    private JTextArea logArea, smsInputArea, smsFileArea, sqlInputArea;
    private DefaultTableModel txModel, catModel, monthModel, budgetModel, sqlResultModel;
    private JTable   txTable;
    private JLabel   statTxCount, statTxTotal, statSmsCount, statCatCount;
    private JButton[] navBtns;
    private int      activeNav = 0;
    private javax.swing.Timer clockTimer;

    private static final String[] NAV = {
            "  Dashboard",
            "  Transactions",
            "  Add SMS",
            "  SMS File",
            "  Reports",
            "  Budget",
            "  SQL Runner",
            "  Manage",
            "  Users",
            "  Log"
    };

    private static final String[] NAV_ICONS = {
            "\u2302", "\u2261", "\u2709", "\u2398", "\u25A4",
            "\u20B9", "\u2394", "\u2699", "\u263A", "\u2710"
    };

    // ─────────────────────────── CONSTRUCTOR ─────────────────────
    public FinanceGUI() {
        super("Finance Parser — Interactive Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 880);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(BG);
        add(contentArea, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        showPanel(0);
        startClock();
        SwingUtilities.invokeLater(() -> {
            refreshStats();
            log("Finance Parser started. User: " + activeUserName + " (ID=" + activeUserId + ")");
        });
    }

    // ════════════════════════ TOP BAR ════════════════════════════
    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(10, 13, 22));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(0, 20, 0, 20)));
        p.setPreferredSize(new Dimension(0, 56));

        // Left — branding
        JPanel left = fp(FlowLayout.LEFT, 10, 0);
        JLabel icon  = new JLabel("$");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        icon.setForeground(ACCENT);
        icon.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        icon.setOpaque(true);
        icon.setBackground(new Color(99, 179, 255, 25));

        JLabel title = new JLabel("Finance Parser");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(ACCENT);

        JLabel sep = new JLabel("  |  ");
        sep.setForeground(BORDER);

        JLabel dash = new JLabel("Interactive Dashboard");
        dash.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dash.setForeground(TEXT2);

        left.add(icon); left.add(title); left.add(sep); left.add(dash);

        // Right
        JPanel right = fp(FlowLayout.RIGHT, 12, 0);

        userBadge = new JLabel();
        updateUserBadge();

        clockLabel = new JLabel();
        clockLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        clockLabel.setForeground(TEXT2);

        JButton switchUserBtn = btn(" Switch User ", ACCENT, false);
        JButton refreshBtn    = btn("  Refresh  ", GREEN, true);
        refreshBtn.addActionListener(e -> {
            refreshStats();
            refreshAllTables();
            log("All data refreshed for " + activeUserName);
        });
        switchUserBtn.addActionListener(e -> showSwitchUserDialog());

        right.add(userBadge);
        right.add(switchUserBtn);
        right.add(clockLabel);
        right.add(refreshBtn);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private void updateUserBadge() {
        if (userBadge == null) return;
        userBadge.setText("  " + activeUserName + "  (ID " + activeUserId + ")  ");
        userBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userBadge.setForeground(GREEN);
        userBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        userBadge.setOpaque(true);
        userBadge.setBackground(new Color(50, 210, 100, 22));
    }

    // ════════════════════════ SIDEBAR ════════════════════════════
    private JPanel buildSidebar() {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBackground(SIDEBAR);
        s.setPreferredSize(new Dimension(220, 0));
        s.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        s.add(Box.createVerticalStrut(16));

        // User card
        JPanel userCard = new JPanel(new BorderLayout(10, 0));
        userCard.setOpaque(false);
        userCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                BorderFactory.createEmptyBorder(10, 14, 10, 10)));
        userCard.setMaximumSize(new Dimension(220, 62));

        JLabel uIcon = new JLabel("U");
        uIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));
        uIcon.setForeground(ACCENT);
        uIcon.setHorizontalAlignment(SwingConstants.CENTER);
        uIcon.setPreferredSize(new Dimension(30, 30));
        uIcon.setBorder(BorderFactory.createLineBorder(ACCENT, 1, true));
        uIcon.setOpaque(true);
        uIcon.setBackground(new Color(99, 179, 255, 20));

        userCardLabel = new JLabel(
                "<html><b style='color:rgb(225,232,255)'>" + activeUserName + "</b>" +
                        "<br><small style='color:rgb(130,150,200)'>User ID: " + activeUserId + "</small></html>");
        userCardLabel.setFont(FB);

        userCard.add(uIcon, BorderLayout.WEST);
        userCard.add(userCardLabel, BorderLayout.CENTER);
        s.add(userCard);
        s.add(Box.createVerticalStrut(12));

        // Separator
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(BORDER);
        sep1.setMaximumSize(new Dimension(220, 1));
        s.add(sep1);
        s.add(Box.createVerticalStrut(8));

        navBtns = new JButton[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            final int idx = i;
            JButton b = new JButton(NAV_ICONS[i] + NAV[i]);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setMaximumSize(new Dimension(220, 42));
            b.setPreferredSize(new Dimension(220, 42));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            styleNav(b, false);
            b.addActionListener(e -> showPanel(idx));
            navBtns[i] = b;
            s.add(b);

            if (i == 4 || i == 7) {
                JSeparator sep = new JSeparator();
                sep.setForeground(BORDER);
                sep.setMaximumSize(new Dimension(220, 1));
                s.add(sep);
                s.add(Box.createVerticalStrut(4));
            }
        }

        s.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("   v2.0  Java + MySQL");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ver.setForeground(new Color(50, 65, 100));
        s.add(ver);
        s.add(Box.createVerticalStrut(12));
        return s;
    }

    private void styleNav(JButton b, boolean active) {
        if (active) {
            b.setBackground(new Color(30, 58, 120));
            b.setForeground(ACCENT);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                    BorderFactory.createEmptyBorder(0, 17, 0, 0)));
            b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        } else {
            b.setBackground(SIDEBAR);
            b.setForeground(TEXT2);
            b.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }

    private void showPanel(int idx) {
        activeNav = idx;
        for (int i = 0; i < navBtns.length; i++) styleNav(navBtns[i], i == idx);
        contentArea.removeAll();
        JPanel panel = switch (idx) {
            case 0 -> buildDashboard();
            case 1 -> buildTransactionsPanel();
            case 2 -> buildAddSmsPanel();
            case 3 -> buildSmsFilePanel();
            case 4 -> buildReportsPanel();
            case 5 -> buildBudgetPanel();
            case 6 -> buildSqlRunnerPanel();
            case 7 -> buildManagePanel();
            case 8 -> buildUsersPanel();
            case 9 -> buildLogPanel();
            default -> buildDashboard();
        };
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    // ══════════════════════ USER SWITCHING ═══════════════════════
    private void showSwitchUserDialog() {
        JDialog dlg = new JDialog(this, "Switch User", true);
        dlg.setSize(520, 420);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(CARD);

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(CARD);
        p.setBorder(pad(16));

        JLabel title = new JLabel("  Select Active User");
        title.setFont(FT);
        title.setForeground(ACCENT);

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Phone", "Created"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = buildTable(m);
        try (ResultSet rs = DBConnection.getConnection().createStatement()
                .executeQuery("SELECT * FROM USER ORDER BY user_id")) {
            while (rs.next()) m.addRow(new Object[]{
                    rs.getInt("user_id"), rs.getString("name"),
                    rs.getString("email"), rs.getString("phone"), rs.getDate("created_at")
            });
        } catch (SQLException e) { log("Load users: " + e.getMessage()); }

        JScrollPane sc = scroll(t);
        sc.setPreferredSize(new Dimension(480, 200));

        JPanel btnRow = fp(FlowLayout.RIGHT, 10, 0);
        JButton selectBtn = btn("  Switch to Selected User  ", GREEN, true);
        JButton cancelBtn = btn("  Cancel  ", TEXT2, false);

        selectBtn.addActionListener(e -> {
            int row = t.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dlg, "Please select a user first."); return; }
            activeUserId   = (int) m.getValueAt(row, 0);
            activeUserName = (String) m.getValueAt(row, 1);
            updateUserBadge();
            updateSidebarUser();
            refreshStats();
            refreshAllTables();
            log("Switched to user: " + activeUserName + " (ID=" + activeUserId + ")");
            dlg.dispose();
            showPanel(activeNav);
        });
        cancelBtn.addActionListener(e -> dlg.dispose());
        btnRow.add(cancelBtn);
        btnRow.add(selectBtn);

        p.add(title, BorderLayout.NORTH);
        p.add(sc, BorderLayout.CENTER);
        p.add(btnRow, BorderLayout.SOUTH);
        dlg.add(p);
        dlg.setVisible(true);
    }

    private void updateSidebarUser() {
        if (userCardLabel != null) {
            userCardLabel.setText(
                    "<html><b style='color:rgb(225,232,255)'>" + activeUserName + "</b>" +
                            "<br><small style='color:rgb(130,150,200)'>User ID: " + activeUserId + "</small></html>");
        }
    }

    // ═══════════════════════ 0: DASHBOARD ════════════════════════
    private JPanel buildDashboard() {
        JPanel p = bg(new BorderLayout(0, 16));
        p.setBorder(pad(22));

        JLabel title = new JLabel("Dashboard  —  " + activeUserName);
        title.setFont(FT);
        title.setForeground(TEXT);

        // Stat cards row
        JPanel cards = op(new GridLayout(1, 4, 14, 0));
        cards.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        statTxCount  = new JLabel("--"); statTxTotal  = new JLabel("--");
        statSmsCount = new JLabel("--"); statCatCount = new JLabel("--");
        cards.add(statCard("TX",  "Total Transactions",  statTxCount,  ACCENT));
        cards.add(statCard("INR", "Total Spending (INR)", statTxTotal,  GREEN));
        cards.add(statCard("SMS", "SMS Records",          statSmsCount, PURPLE));
        cards.add(statCard("CAT", "Categories Used",      statCatCount, TEAL));

        // Bottom row
        JPanel bottom = op(new GridLayout(1, 2, 14, 0));

        // Recent transactions
        JPanel recentCard = cardPanel("  Recent Transactions (Last 10)");
        recentCard.setLayout(new BorderLayout(0, 6));
        recentCard.setBorder(BorderFactory.createCompoundBorder(recentCard.getBorder(), pad(10)));
        DefaultTableModel rm = new DefaultTableModel(new String[]{"Bank","Type","Amount","Date","Merchant","Category"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable rt = buildTable(rm);
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT b.bank_name,b.tx_type,b.amount,b.tx_date,b.merchant_clean,c.category_name " +
                        "FROM BANK_SMS b LEFT JOIN TRANSACTION t ON b.transaction_id=t.transaction_id " +
                        "LEFT JOIN CATEGORY c ON t.category_id=c.category_id " +
                        "WHERE t.user_id=? ORDER BY b.sms_id DESC LIMIT 10")) {
            ps.setInt(1, activeUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rm.addRow(new Object[]{
                    nvl(rs.getString("bank_name"),"-"),
                    rs.getString("tx_type"),
                    rs.getObject("amount") != null ? String.format("%.2f", rs.getDouble("amount")) : "-",
                    rs.getDate("tx_date"),
                    nvl(rs.getString("merchant_clean"),"-"),
                    nvl(rs.getString("category_name"),"-")
            });
        } catch (SQLException ignored) {}
        recentCard.add(scroll(rt), BorderLayout.CENTER);

        // Quick Actions — fully visible coloured buttons
        JPanel qaCard = cardPanel("  Quick Actions");
        qaCard.setLayout(new GridLayout(3, 2, 10, 10));
        qaCard.setBorder(BorderFactory.createCompoundBorder(qaCard.getBorder(), pad(14)));

        String[][]  qas    = {
                {"Add SMS",       "2"},
                {"Parse File",    "3"},
                {"Reports",       "4"},
                {"Budget",        "5"},
                {"SQL Runner",    "6"},
                {"Export Charts", ""},
        };
        Color[] qaColors = { GREEN, ACCENT, PURPLE, ORANGE, TEAL, new Color(255, 100, 100) };
        String[] qaSymbols = { "+", "F", "R", "$", "S", "C" };

        for (int i = 0; i < qas.length; i++) {
            final String nav = qas[i][1];
            final String lbl = qas[i][0];
            final Color  c   = qaColors[i];

            JButton qb = new JButton(qaSymbols[i] + "  " + lbl);
            qb.setFont(new Font("Segoe UI", Font.BOLD, 13));
            // Always white text, always coloured background — never invisible
            qb.setForeground(Color.WHITE);
            qb.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 210));
            qb.setOpaque(true);
            qb.setBorderPainted(true);
            qb.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c.darker(), 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            qb.setFocusPainted(false);
            qb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Hover: saturate to full color
            qb.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    qb.setBackground(c);
                    qb.setForeground(Color.WHITE);
                }
                public void mouseExited(MouseEvent e) {
                    qb.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 210));
                    qb.setForeground(Color.WHITE);
                }
            });

            if (nav.isEmpty()) {
                qb.addActionListener(e -> {
                    ChartDataExporter.exportAll(activeUserId, "2024-10");
                    ChartDataExporter.writeHtmlDashboard(activeUserId, "charts/index.html");
                    log("Charts exported for " + activeUserName);
                });
            } else {
                final int navIdx = Integer.parseInt(nav);
                qb.addActionListener(e -> showPanel(navIdx));
            }
            qaCard.add(qb);
        }

        bottom.add(recentCard);
        bottom.add(qaCard);

        p.add(title,  BorderLayout.NORTH);
        p.add(cards,  BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel statCard(String abbr, String label, JLabel valLbl, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(CARD);
        // Top accent bar
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        BorderFactory.createEmptyBorder(16, 20, 16, 20))));

        JLabel abbrLbl = new JLabel(abbr);
        abbrLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        abbrLbl.setForeground(accent);
        abbrLbl.setOpaque(true);
        abbrLbl.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
        abbrLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        JPanel top = fp(FlowLayout.LEFT, 6, 0);
        top.setOpaque(false);
        top.add(abbrLbl);

        JLabel lblW = new JLabel(label.toUpperCase());
        lblW.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblW.setForeground(TEXT2);

        valLbl.setFont(FBG);
        valLbl.setForeground(accent);

        p.add(top,    BorderLayout.NORTH);
        p.add(valLbl, BorderLayout.CENTER);
        p.add(lblW,   BorderLayout.SOUTH);
        return p;
    }

    private void refreshStats() {
        SwingUtilities.invokeLater(() -> {
            try {
                PreparedStatement ps1 = DBConnection.getConnection().prepareStatement(
                        "SELECT COUNT(*) FROM TRANSACTION WHERE user_id=?");
                ps1.setInt(1, activeUserId);
                ResultSet r1 = ps1.executeQuery();
                if (r1.next() && statTxCount != null) statTxCount.setText(String.valueOf(r1.getInt(1)));

                PreparedStatement ps2 = DBConnection.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(amount),0) FROM TRANSACTION WHERE user_id=?");
                ps2.setInt(1, activeUserId);
                ResultSet r2 = ps2.executeQuery();
                if (r2.next() && statTxTotal != null) statTxTotal.setText(String.format("%.0f", r2.getDouble(1)));

                ResultSet r3 = DBConnection.getConnection().createStatement()
                        .executeQuery("SELECT COUNT(*) FROM BANK_SMS");
                if (r3.next() && statSmsCount != null) statSmsCount.setText(String.valueOf(r3.getInt(1)));

                PreparedStatement ps4 = DBConnection.getConnection().prepareStatement(
                        "SELECT COUNT(DISTINCT category_id) FROM TRANSACTION WHERE user_id=?");
                ps4.setInt(1, activeUserId);
                ResultSet r4 = ps4.executeQuery();
                if (r4.next() && statCatCount != null) statCatCount.setText(String.valueOf(r4.getInt(1)));
            } catch (SQLException ignored) {}
        });
    }

    // ══════════════════ 1: TRANSACTIONS ══════════════════════════
    private JPanel buildTransactionsPanel() {
        JPanel p = bg(new BorderLayout(0, 10));
        p.setBorder(pad(20));

        JPanel hdr = op(new BorderLayout());
        JLabel title = new JLabel("Transactions  —  " + activeUserName);
        title.setFont(FT);
        title.setForeground(TEXT);

        JPanel btnRow = fp(FlowLayout.RIGHT, 8, 0);
        JButton addBtn  = btn("  + Add  ",     GREEN,  true);
        JButton editBtn = btn("  Edit  ",       ORANGE, false);
        JButton delBtn  = btn("  Delete  ",     RED,    false);
        JButton refBtn  = btn("  Refresh  ",    ACCENT, true);
        btnRow.add(refBtn); btnRow.add(addBtn); btnRow.add(editBtn); btnRow.add(delBtn);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(btnRow, BorderLayout.EAST);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(CARD);
        filterBar.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        JTextField search = darkField(18);
        search.setToolTipText("Search merchant/bank/UPI...");
        JComboBox<String> typeF = combo("All Types", "DEBIT", "CREDIT");
        JComboBox<String> bankF = combo("All Banks", "Axis Bank", "SBI", "HDFC", "ICICI", "Kotak");
        JButton applyF = btn(" Apply ", ACCENT, true);
        JButton clearF = btn(" Clear ", TEXT2, false);
        filterBar.add(lbl("Search:")); filterBar.add(search);
        filterBar.add(lbl("Type:")); filterBar.add(typeF);
        filterBar.add(lbl("Bank:")); filterBar.add(bankF);
        filterBar.add(applyF); filterBar.add(clearF);

        String[] cols = {"SMS ID","Bank","Type","Amount","Date","Time","Merchant","Category","UPI Ref","Account","Status"};
        txModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        txTable = buildTable(txModel);
        JScrollPane sc = scroll(txTable);

        refBtn.addActionListener(e  -> loadTransactions());
        addBtn.addActionListener(e  -> showAddTransactionDialog());
        editBtn.addActionListener(e -> editSelectedTransaction());
        delBtn.addActionListener(e  -> deleteSelectedTransaction());
        applyF.addActionListener(e  -> filterTransactions(search.getText(),
                (String) typeF.getSelectedItem(), (String) bankF.getSelectedItem()));
        clearF.addActionListener(e  -> { search.setText(""); typeF.setSelectedIndex(0); bankF.setSelectedIndex(0); loadTransactions(); });

        JPanel top2 = op(new BorderLayout(0, 8));
        top2.add(hdr, BorderLayout.NORTH);
        top2.add(filterBar, BorderLayout.SOUTH);
        p.add(top2, BorderLayout.NORTH);
        p.add(sc, BorderLayout.CENTER);
        loadTransactions();
        return p;
    }

    private void loadTransactions() {
        if (txModel == null) return;
        txModel.setRowCount(0);
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT b.sms_id,b.bank_name,b.tx_type,b.amount,b.tx_date,b.tx_time," +
                        "b.merchant_clean,c.category_name,b.upi_ref,b.account_last4,b.parse_status " +
                        "FROM BANK_SMS b LEFT JOIN TRANSACTION t ON b.transaction_id=t.transaction_id " +
                        "LEFT JOIN CATEGORY c ON t.category_id=c.category_id " +
                        "WHERE t.user_id=? ORDER BY b.sms_id DESC")) {
            ps.setInt(1, activeUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) txModel.addRow(new Object[]{
                    rs.getInt("sms_id"), nvl(rs.getString("bank_name"),"-"),
                    nvl(rs.getString("tx_type"),"-"),
                    rs.getObject("amount") != null ? String.format("%.2f", rs.getDouble("amount")) : "-",
                    rs.getDate("tx_date"), rs.getTime("tx_time"),
                    nvl(rs.getString("merchant_clean"),"-"), nvl(rs.getString("category_name"),"-"),
                    nvl(rs.getString("upi_ref"),"-"), nvl(rs.getString("account_last4"),"-"),
                    nvl(rs.getString("parse_status"),"-")
            });
        } catch (SQLException e) { log("Load tx: " + e.getMessage()); }
        refreshStats();
    }

    private void filterTransactions(String q, String type, String bank) {
        if (txModel == null) return;
        txModel.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT b.sms_id,b.bank_name,b.tx_type,b.amount,b.tx_date,b.tx_time," +
                        "b.merchant_clean,c.category_name,b.upi_ref,b.account_last4,b.parse_status " +
                        "FROM BANK_SMS b LEFT JOIN TRANSACTION t ON b.transaction_id=t.transaction_id " +
                        "LEFT JOIN CATEGORY c ON t.category_id=c.category_id WHERE t.user_id=?");
        List<Object> params = new ArrayList<>();
        params.add(activeUserId);
        if (q != null && !q.isBlank()) {
            sql.append(" AND (b.merchant_clean LIKE ? OR b.bank_name LIKE ? OR b.upi_ref LIKE ?)");
            params.add("%" + q + "%"); params.add("%" + q + "%"); params.add("%" + q + "%");
        }
        if (type != null && !type.startsWith("All")) { sql.append(" AND b.tx_type=?"); params.add(type); }
        if (bank != null && !bank.startsWith("All")) { sql.append(" AND b.bank_name LIKE ?"); params.add("%" + bank + "%"); }
        sql.append(" ORDER BY b.sms_id DESC");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) txModel.addRow(new Object[]{
                    rs.getInt("sms_id"), nvl(rs.getString("bank_name"),"-"), nvl(rs.getString("tx_type"),"-"),
                    rs.getObject("amount") != null ? String.format("%.2f", rs.getDouble("amount")) : "-",
                    rs.getDate("tx_date"), rs.getTime("tx_time"),
                    nvl(rs.getString("merchant_clean"),"-"), nvl(rs.getString("category_name"),"-"),
                    nvl(rs.getString("upi_ref"),"-"), nvl(rs.getString("account_last4"),"-"),
                    nvl(rs.getString("parse_status"),"-")
            });
            log("Filter: " + txModel.getRowCount() + " results.");
        } catch (SQLException e) { log("Filter: " + e.getMessage()); }
    }

    private void showAddTransactionDialog() {
        JDialog dlg = new JDialog(this, "Add Transaction Manually", true);
        dlg.setSize(480, 380);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD);
        form.setBorder(pad(18));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 8, 7, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill   = GridBagConstraints.HORIZONTAL;

        JTextField merchantF  = darkField(20);
        JTextField amountF    = darkField(12); amountF.setText("0.00");
        JTextField dateF      = darkField(12); dateF.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        JComboBox<String> typeBox = combo("DEBIT", "CREDIT");
        JComboBox<String> catBox  = combo("1-Food","2-Entertainment","3-Transport","4-Shopping","5-Travel","6-Utilities");
        JTextField currF      = darkField(8); currF.setText("INR");

        g.gridx=0;g.gridy=0;g.weightx=0; form.add(lbl("Merchant:"),g);
        g.gridx=1;g.weightx=1;           form.add(merchantF,g);
        g.gridx=0;g.gridy=1;g.weightx=0; form.add(lbl("Amount (INR):"),g);
        g.gridx=1;                        form.add(amountF,g);
        g.gridx=0;g.gridy=2;             form.add(lbl("Date (YYYY-MM-DD):"),g);
        g.gridx=1;                        form.add(dateF,g);
        g.gridx=0;g.gridy=3;             form.add(lbl("Type:"),g);
        g.gridx=1;                        form.add(typeBox,g);
        g.gridx=0;g.gridy=4;             form.add(lbl("Category:"),g);
        g.gridx=1;                        form.add(catBox,g);
        g.gridx=0;g.gridy=5;             form.add(lbl("Currency:"),g);
        g.gridx=1;                        form.add(currF,g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(CARD);
        JButton saveBtn = btn("  Save  ", GREEN, true);
        JButton canBtn  = btn("  Cancel  ", TEXT2, false);
        btns.add(canBtn); btns.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try {
                String merchant = merchantF.getText().trim();
                double amount   = Double.parseDouble(amountF.getText().trim());
                String date     = dateF.getText().trim();
                String currency = currF.getText().trim();
                String txType   = (String) typeBox.getSelectedItem();
                int catId       = catBox.getSelectedIndex() + 1;

                PreparedStatement mFind = DBConnection.getConnection().prepareStatement(
                        "SELECT merchant_id FROM MERCHANT WHERE LOWER(merchant_name)=LOWER(?)");
                mFind.setString(1, merchant);
                ResultSet mr = mFind.executeQuery();
                int mId;
                if (mr.next()) { mId = mr.getInt(1); }
                else {
                    PreparedStatement mIns = DBConnection.getConnection().prepareStatement(
                            "INSERT INTO MERCHANT(merchant_name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
                    mIns.setString(1, merchant); mIns.executeUpdate();
                    ResultSet mg = mIns.getGeneratedKeys(); mg.next(); mId = mg.getInt(1);
                }

                PreparedStatement tx = DBConnection.getConnection().prepareStatement(
                        "INSERT INTO TRANSACTION(user_id,merchant_id,category_id,source_id,amount,transaction_date,currency,status,raw_text) VALUES(?,?,?,1,?,?,?,'manual','Manually added')",
                        Statement.RETURN_GENERATED_KEYS);
                tx.setInt(1,activeUserId); tx.setInt(2,mId); tx.setInt(3,catId);
                tx.setDouble(4,amount); tx.setString(5,date); tx.setString(6,currency);
                tx.executeUpdate();

                ResultSet tg = tx.getGeneratedKeys();
                int txId = tg.next() ? tg.getInt(1) : -1;
                PreparedStatement bs = DBConnection.getConnection().prepareStatement(
                        "INSERT INTO BANK_SMS(raw_sms,currency,amount,tx_type,merchant_clean,bank_name,transaction_id,parse_status) VALUES(?,?,?,?,?,'Manual',?,'parsed')");
                bs.setString(1,"Manually added: "+merchant); bs.setString(2,currency);
                bs.setDouble(3,amount); bs.setString(4,txType); bs.setString(5,merchant); bs.setInt(6,txId);
                bs.executeUpdate();

                log("Manual transaction added: Rs." + amount + " — " + merchant);
                loadTransactions();
                dlg.dispose();
            } catch (Exception ex) { log("Add tx: " + ex.getMessage()); }
        });
        canBtn.addActionListener(e -> dlg.dispose());

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void editSelectedTransaction() {
        if (txTable == null || txTable.getSelectedRow() < 0) { log("Select a row first."); return; }
        int row = txTable.getSelectedRow();
        int smsId = (int) txModel.getValueAt(row, 0);

        JDialog dlg = new JDialog(this, "Edit Transaction — SMS ID " + smsId, true);
        dlg.setSize(460, 320);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD);
        form.setBorder(pad(18));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7,8,7,8); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;

        JTextField merchantF = darkField(20);
        JTextField amountF   = darkField(12);
        JComboBox<String> typeBox = combo("DEBIT","CREDIT");
        JComboBox<String> catBox  = combo("1-Food","2-Entertainment","3-Transport","4-Shopping","5-Travel","6-Utilities");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT b.merchant_clean,b.tx_type,b.amount,t.category_id FROM BANK_SMS b LEFT JOIN TRANSACTION t ON b.transaction_id=t.transaction_id WHERE b.sms_id=?")) {
            ps.setInt(1, smsId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                merchantF.setText(nvl(rs.getString("merchant_clean"),""));
                amountF.setText(String.format("%.2f", rs.getDouble("amount")));
                typeBox.setSelectedItem(nvl(rs.getString("tx_type"),"DEBIT"));
                int ci = rs.getInt("category_id");
                if (ci >= 1 && ci <= 6) catBox.setSelectedIndex(ci - 1);
            }
        } catch (SQLException ex) { log("Edit load: " + ex.getMessage()); }

        g.gridx=0;g.gridy=0;g.weightx=0; form.add(lbl("Merchant:"),g); g.gridx=1;g.weightx=1; form.add(merchantF,g);
        g.gridx=0;g.gridy=1;g.weightx=0; form.add(lbl("Amount:"),g);   g.gridx=1; form.add(amountF,g);
        g.gridx=0;g.gridy=2;             form.add(lbl("Type:"),g);     g.gridx=1; form.add(typeBox,g);
        g.gridx=0;g.gridy=3;             form.add(lbl("Category:"),g); g.gridx=1; form.add(catBox,g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        btns.setBackground(CARD);
        JButton saveBtn = btn("  Save  ", GREEN, true);
        JButton canBtn  = btn("  Cancel  ", TEXT2, false);
        btns.add(canBtn); btns.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try {
                int catId = catBox.getSelectedIndex() + 1;
                double amt = Double.parseDouble(amountF.getText().trim());
                String merchant = merchantF.getText().trim();
                String txType = (String) typeBox.getSelectedItem();

                DBConnection.getConnection().prepareStatement(
                        "UPDATE BANK_SMS SET merchant_clean='"+merchant+"',tx_type='"+txType+"',amount="+amt+" WHERE sms_id="+smsId).executeUpdate();

                PreparedStatement gTx = DBConnection.getConnection().prepareStatement("SELECT transaction_id FROM BANK_SMS WHERE sms_id=?");
                gTx.setInt(1, smsId);
                ResultSet rs = gTx.executeQuery();
                if (rs.next()) {
                    int txId = rs.getInt("transaction_id");
                    if (txId > 0) DBConnection.getConnection().prepareStatement(
                            "UPDATE TRANSACTION SET category_id="+catId+",amount="+amt+" WHERE transaction_id="+txId).executeUpdate();
                }
                log("Updated SMS ID " + smsId + " — " + merchant + " Rs." + amt);
                loadTransactions();
                dlg.dispose();
            } catch (Exception ex) { log("Update: " + ex.getMessage()); }
        });
        canBtn.addActionListener(e -> dlg.dispose());
        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void deleteSelectedTransaction() {
        if (txTable == null || txTable.getSelectedRow() < 0) { log("Select a row first."); return; }
        int smsId = (int) txModel.getValueAt(txTable.getSelectedRow(), 0);
        if (!confirm("Delete transaction SMS ID " + smsId + "?\nThis removes BANK_SMS + TRANSACTION rows.")) return;
        try {
            PreparedStatement gTx = DBConnection.getConnection().prepareStatement("SELECT transaction_id FROM BANK_SMS WHERE sms_id=?");
            gTx.setInt(1, smsId);
            ResultSet rs = gTx.executeQuery();
            if (rs.next()) {
                int txId = rs.getInt("transaction_id");
                if (txId > 0) {
                    DBConnection.getConnection().prepareStatement("DELETE FROM PARSED_LOG WHERE transaction_id="+txId).executeUpdate();
                    DBConnection.getConnection().prepareStatement("DELETE FROM TRANSACTION WHERE transaction_id="+txId).executeUpdate();
                }
            }
            DBConnection.getConnection().prepareStatement("DELETE FROM BANK_SMS WHERE sms_id="+smsId).executeUpdate();
            log("Deleted SMS ID " + smsId);
            loadTransactions();
        } catch (SQLException e) { log("Delete: " + e.getMessage()); }
    }

    // ═════════════════════ 2: ADD SMS ════════════════════════════
    private JPanel buildAddSmsPanel() {
        JPanel p = bg(new BorderLayout(0, 14));
        p.setBorder(pad(20));
        JLabel title = new JLabel("Add New Bank SMS  —  " + activeUserName);
        title.setFont(FT);
        title.setForeground(TEXT);

        JPanel mainCard = cardPanel("  Paste SMS Message (exactly as received)");
        mainCard.setLayout(new BorderLayout(0, 10));
        mainCard.setBorder(BorderFactory.createCompoundBorder(mainCard.getBorder(), pad(14)));

        smsInputArea = new JTextArea(10, 50);
        smsInputArea.setFont(FM);
        smsInputArea.setBackground(new Color(8, 12, 24));
        smsInputArea.setForeground(new Color(130, 255, 130));
        smsInputArea.setCaretColor(Color.WHITE);
        smsInputArea.setLineWrap(true);
        smsInputArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        smsInputArea.setText("INR 72.00 debited\nA/c no. XX8814\n07-04-26, 21:27:59\nUPI/P2M/646304856626/SAI BHAKRI CENTRE\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank");

        JScrollPane isc = new JScrollPane(smsInputArea);
        isc.setBorder(BorderFactory.createLineBorder(ACCENT, 1, true));

        JPanel hintCard = new JPanel(new GridLayout(4, 1, 0, 2));
        hintCard.setBackground(CARD2);
        hintCard.setBorder(pad(10));
        for (String h : new String[]{
                "  Axis Bank: paste multi-line SMS exactly (each line on its own line)",
                "  SBI / HDFC / ICICI / Kotak: paste the single long SMS line",
                "  Duplicate UPI reference numbers are blocked automatically",
                "  SMS is auto-parsed, categorized and saved to MySQL for user: " + activeUserName
        }) {
            JLabel hl = new JLabel(h);
            hl.setFont(FS);
            hl.setForeground(TEXT2);
            hintCard.add(hl);
        }

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        JButton parseBtn = btn("  Parse & Save SMS  ", GREEN, true);
        JButton clearBtn = btn("  Clear  ", TEXT2, false);
        JButton sbiEx    = btn("  SBI Example  ", ACCENT, false);
        JButton hdfcEx   = btn("  HDFC Example  ", PURPLE, false);
        parseBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        parseBtn.setPreferredSize(new Dimension(210, 40));
        parseBtn.addActionListener(e -> parseAndSaveSms());
        clearBtn.addActionListener(e -> smsInputArea.setText(""));
        sbiEx.addActionListener(e  -> smsInputArea.setText("Rs.499.00 debited from ac **6789 on 15-10-24. UPI:123456789012. Avl bal:Rs.12,340.50-SBI"));
        hdfcEx.addActionListener(e -> smsInputArea.setText("Rs 2399 debited from HDFC Bank A/c **1234 on 18/10/24 by AMAZON. Avl Bal: Rs 8,901.00"));
        btnRow.add(parseBtn); btnRow.add(clearBtn);
        btnRow.add(Box.createHorizontalStrut(16));
        btnRow.add(lbl("Examples:")); btnRow.add(sbiEx); btnRow.add(hdfcEx);

        mainCard.add(isc, BorderLayout.CENTER);
        mainCard.add(hintCard, BorderLayout.SOUTH);
        p.add(title, BorderLayout.NORTH);
        p.add(mainCard, BorderLayout.CENTER);
        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    private void parseAndSaveSms() {
        String sms = smsInputArea != null ? smsInputArea.getText().trim() : "";
        if (sms.isEmpty()) { log("SMS empty."); return; }
        boolean isAxis = sms.contains("\n") && sms.matches("(?s)(?i)INR\\s+[\\d,]+\\.?\\d*\\s+(debited|credited).*");
        SmsDAO dao = new SmsDAO();
        if (isAxis) {
            AxisSmsParser.SmsResult r = AxisSmsParser.parse(sms);
            if (!r.success) { log("Parse failed: " + r.failureReason); return; }
            if (isDupe(r.upiRef)) { log("DUPLICATE — " + r.upiRef + " already saved."); return; }
            dao.ingestAxisSms(sms, activeUserId, 1);
            log("Saved: " + r.currency + " " + r.amount + " — " + r.merchantClean + " — " + r.txDate + " (user: " + activeUserName + ")");
        } else {
            BankSmsParser.SmsResult r = BankSmsParser.parse(sms);
            if (!r.success) { log("Parse failed: no amount or date."); return; }
            if (isDupe(r.upiRef)) { log("DUPLICATE — already saved."); return; }
            dao.ingestBankSms(sms, activeUserId, 1);
            log("Saved: Rs." + r.amount + " — " + r.merchant + " — " + r.date);
        }
        refreshStats();
        JOptionPane.showMessageDialog(this, "SMS saved for " + activeUserName + "!\nView in Transactions tab.", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean isDupe(String ref) {
        if (ref == null || ref.isBlank()) return false;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM BANK_SMS WHERE upi_ref=?")) {
            ps.setString(1, ref);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    // ═════════════════════ 3: SMS FILE ═══════════════════════════
    private JPanel buildSmsFilePanel() {
        JPanel p = bg(new BorderLayout(0, 12));
        p.setBorder(pad(20));
        JLabel title = new JLabel("SMS Inbox File Manager");
        title.setFont(FT);
        title.setForeground(TEXT);

        smsFileArea = new JTextArea();
        smsFileArea.setFont(FM);
        smsFileArea.setBackground(new Color(8, 12, 24));
        smsFileArea.setForeground(new Color(180, 215, 255));
        smsFileArea.setCaretColor(Color.WHITE);
        smsFileArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane sc = new JScrollPane(smsFileArea);
        sc.setBorder(BorderFactory.createLineBorder(ACCENT, 1, true));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        JButton loadBtn  = btn(" Load File ",     ACCENT,  true);
        JButton saveBtn  = btn(" Save & Replace ", GREEN,   true);
        JButton parseBtn = btn(" Parse All ",      ACCENT2, true);
        JButton clearBtn = btn(" Clear ",           TEXT2,   false);
        loadBtn.addActionListener(e  -> loadSmsFile());
        saveBtn.addActionListener(e  -> saveSmsFile());
        parseBtn.addActionListener(e -> parseAllFromFile());
        clearBtn.addActionListener(e -> smsFileArea.setText(""));
        btnRow.add(loadBtn); btnRow.add(saveBtn); btnRow.add(parseBtn); btnRow.add(clearBtn);

        JLabel note = new JLabel("  'Save & Replace' overwrites sms_inbox.txt.  'Parse All' skips duplicates, saves for user: " + activeUserName);
        note.setFont(FS);
        note.setForeground(ORANGE);

        JPanel south = op(new GridLayout(2, 1, 0, 3));
        south.add(btnRow);
        south.add(note);

        p.add(title, BorderLayout.NORTH);
        p.add(sc, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        loadSmsFile();
        return p;
    }

    private void loadSmsFile() {
        String[] paths = {"resources/sms_inbox.txt", System.getProperty("user.dir") + "/resources/sms_inbox.txt"};
        for (String path : paths) {
            File f = new File(path);
            if (!f.exists()) continue;
            try (BufferedReader br = new BufferedReader(new java.io.FileReader(f))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                if (smsFileArea != null) smsFileArea.setText(sb.toString());
                log("Loaded: " + f.getAbsolutePath());
                return;
            } catch (IOException e) { log("Load: " + e.getMessage()); }
        }
        if (smsFileArea != null) smsFileArea.setText("# Paste SMS here\n# Blank line between each\n\n");
        log("sms_inbox.txt not found. Paste and save.");
    }

    private void saveSmsFile() {
        if (smsFileArea == null) return;
        String[] paths = {"resources/sms_inbox.txt", System.getProperty("user.dir") + "/resources/sms_inbox.txt"};
        for (String path : paths) {
            try {
                File f = new File(path);
                if (f.getParentFile() != null) f.getParentFile().mkdirs();
                try (FileWriter fw = new FileWriter(f)) { fw.write(smsFileArea.getText()); }
                log("Saved: " + f.getAbsolutePath());
                return;
            } catch (IOException ignored) {}
        }
        log("Cannot save file.");
    }

    private void parseAllFromFile() {
        if (smsFileArea == null) return;
        String content = smsFileArea.getText();
        List<String> msgs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.trim().startsWith("#")) continue;
            if (line.trim().isEmpty()) {
                if (cur.length() > 0) { msgs.add(cur.toString().trim()); cur = new StringBuilder(); }
            } else {
                if (cur.length() > 0) cur.append("\n");
                cur.append(line);
            }
        }
        if (cur.length() > 0) msgs.add(cur.toString().trim());
        log("Processing " + msgs.size() + " SMS for " + activeUserName + "...");
        int saved = 0, dupes = 0, failed = 0;
        SmsDAO dao = new SmsDAO();
        for (String sms : msgs) {
            boolean isAxis = sms.contains("\n") && sms.matches("(?s)(?i)INR\\s+[\\d,]+\\.?\\d*\\s+(debited|credited).*");
            if (isAxis) {
                AxisSmsParser.SmsResult r = AxisSmsParser.parse(sms);
                if (!r.success) { failed++; continue; }
                if (isDupe(r.upiRef)) { dupes++; continue; }
                dao.ingestAxisSms(sms, activeUserId, 1); saved++;
            } else {
                BankSmsParser.SmsResult r = BankSmsParser.parse(sms);
                if (!r.success) { failed++; continue; }
                if (isDupe(r.upiRef)) { dupes++; continue; }
                dao.ingestBankSms(sms, activeUserId, 1); saved++;
            }
        }
        log("Done — Saved:" + saved + "  Dupes skipped:" + dupes + "  Failed:" + failed);
        refreshStats();
        refreshAllTables();
    }

    // ═════════════════════ 4: REPORTS ════════════════════════════
    private JPanel buildReportsPanel() {
        JPanel p = bg(new BorderLayout(0, 12));
        p.setBorder(pad(20));

        JPanel hdr = op(new BorderLayout());
        JLabel title = new JLabel("Analytics Reports  —  " + activeUserName);
        title.setFont(FT);
        title.setForeground(TEXT);
        JButton refBtn = btn(" Refresh ", ACCENT, true);
        refBtn.addActionListener(e -> { loadReports(); log("Reports refreshed for " + activeUserName); });
        hdr.add(title, BorderLayout.WEST);
        hdr.add(refBtn, BorderLayout.EAST);

        JPanel grid = op(new GridLayout(3, 1, 0, 14));

        catModel   = new DefaultTableModel(new String[]{"Category","Transactions","Total (INR)","Avg (INR)","% of Total"}, 0) { public boolean isCellEditable(int r,int c){return false;} };
        monthModel = new DefaultTableModel(new String[]{"Month","Transactions","Total (INR)","Max","Min"}, 0)               { public boolean isCellEditable(int r,int c){return false;} };
        budgetModel= new DefaultTableModel(new String[]{"Category","Budget","Spent","Remaining","% Used","Status"}, 0)      { public boolean isCellEditable(int r,int c){return false;} };

        grid.add(rCard("  Spending by Category", catModel, ACCENT));
        grid.add(rCard("  Monthly Trend",         monthModel, GREEN));
        grid.add(rCard("  Budget vs Actual",      budgetModel, ORANGE));

        p.add(hdr, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(grid);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.setBorder(null);
        p.add(sc, BorderLayout.CENTER);
        loadReports();
        return p;
    }

    private JPanel rCard(String header, DefaultTableModel m, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14))));
        JLabel l = new JLabel(header);
        l.setFont(FMD);
        l.setForeground(accent);
        p.add(l, BorderLayout.NORTH);
        p.add(scroll(buildTable(m)), BorderLayout.CENTER);
        return p;
    }

    private void loadReports() {
        if (catModel != null) {
            catModel.setRowCount(0);
            try {
                PreparedStatement tps = DBConnection.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(amount),0) FROM TRANSACTION WHERE user_id=?");
                tps.setInt(1, activeUserId);
                ResultSet tr = tps.executeQuery();
                double grand = tr.next() ? tr.getDouble(1) : 1.0;
                PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                        "SELECT c.category_name,COUNT(*) cnt,SUM(t.amount) total,AVG(t.amount) avg " +
                                "FROM TRANSACTION t JOIN CATEGORY c ON t.category_id=c.category_id " +
                                "WHERE t.user_id=? GROUP BY c.category_name ORDER BY total DESC");
                ps.setInt(1, activeUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double tot = rs.getDouble("total");
                    catModel.addRow(new Object[]{rs.getString("category_name"), rs.getInt("cnt"),
                            String.format("%.2f",tot), String.format("%.2f",rs.getDouble("avg")),
                            String.format("%.1f%%", grand > 0 ? tot/grand*100 : 0)});
                }
            } catch (SQLException e) { log("Cat report: " + e.getMessage()); }
        }
        if (monthModel != null) {
            monthModel.setRowCount(0);
            try {
                PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                        "SELECT DATE_FORMAT(transaction_date,'%Y-%m') m,COUNT(*) cnt,SUM(amount) total,MAX(amount) mx,MIN(amount) mn " +
                                "FROM TRANSACTION WHERE user_id=? GROUP BY m ORDER BY m DESC");
                ps.setInt(1, activeUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) monthModel.addRow(new Object[]{rs.getString("m"),rs.getInt("cnt"),
                        String.format("%.2f",rs.getDouble("total")),String.format("%.2f",rs.getDouble("mx")),String.format("%.2f",rs.getDouble("mn"))});
            } catch (SQLException e) { log("Month report: " + e.getMessage()); }
        }
        if (budgetModel != null) {
            budgetModel.setRowCount(0);
            try {
                PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                        "SELECT c.category_name,b.limit_amount budget,COALESCE(SUM(t.amount),0) spent " +
                                "FROM BUDGET b JOIN CATEGORY c ON b.category_id=c.category_id " +
                                "LEFT JOIN TRANSACTION t ON t.category_id=b.category_id AND t.user_id=b.user_id " +
                                "AND DATE_FORMAT(t.transaction_date,'%Y-%m')=b.period " +
                                "WHERE b.user_id=? GROUP BY c.category_name,b.limit_amount,b.period ORDER BY c.category_name");
                ps.setInt(1, activeUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double bud=rs.getDouble("budget"),spent=rs.getDouble("spent"),rem=bud-spent,pct=bud>0?spent/bud*100:0;
                    String status = spent>bud ? "OVER" : pct>80 ? "WARNING" : "OK";
                    budgetModel.addRow(new Object[]{rs.getString("category_name"),
                            String.format("%.0f",bud),String.format("%.0f",spent),
                            String.format("%.0f",rem),String.format("%.1f%%",pct),status});
                }
            } catch (SQLException e) { log("Budget report: " + e.getMessage()); }
        }
    }

    // ═════════════════════ 5: BUDGET ═════════════════════════════
    private JPanel buildBudgetPanel() {
        JPanel p = bg(new BorderLayout(0, 14));
        p.setBorder(pad(20));
        JLabel title = new JLabel("Budget Manager  —  " + activeUserName);
        title.setFont(FT);
        title.setForeground(TEXT);

        JPanel addCard = cardPanel("  Add / Update Budget");
        addCard.setLayout(new GridBagLayout());
        addCard.setBorder(BorderFactory.createCompoundBorder(addCard.getBorder(), pad(14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7,10,7,10); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;

        JComboBox<String> catBox  = combo("1-Food","2-Entertainment","3-Transport","4-Shopping","5-Travel","6-Utilities");
        JTextField periodF = darkField(10); periodF.setText(new SimpleDateFormat("yyyy-MM").format(new Date()));
        JTextField limitF  = darkField(10); limitF.setText("3000");
        JButton addBtn = btn("  Add / Update Budget  ", GREEN, true);
        JButton delBtn = btn("  Delete Budget  ",        RED,   false);

        g.gridx=0;g.gridy=0;g.weightx=0;addCard.add(lbl("Category:"),g);      g.gridx=1;g.weightx=0.4;addCard.add(catBox,g);
        g.gridx=2;g.weightx=0;addCard.add(lbl("Period (YYYY-MM):"),g);         g.gridx=3;g.weightx=0.3;addCard.add(periodF,g);
        g.gridx=4;g.weightx=0;addCard.add(lbl("Limit (INR):"),g);              g.gridx=5;g.weightx=0.3;addCard.add(limitF,g);
        g.gridx=0;g.gridy=1;g.gridwidth=3;addCard.add(addBtn,g);
        g.gridx=3;g.gridwidth=3;addCard.add(delBtn,g);

        addBtn.addActionListener(e -> {
            try {
                int catId = catBox.getSelectedIndex() + 1;
                String period = periodF.getText().trim();
                double limit  = Double.parseDouble(limitF.getText().trim());
                PreparedStatement del = DBConnection.getConnection().prepareStatement(
                        "DELETE FROM BUDGET WHERE user_id=? AND category_id=? AND period=?");
                del.setInt(1,activeUserId); del.setInt(2,catId); del.setString(3,period); del.executeUpdate();
                PreparedStatement ins = DBConnection.getConnection().prepareStatement(
                        "INSERT INTO BUDGET(user_id,category_id,limit_amount,period) VALUES(?,?,?,?)");
                ins.setInt(1,activeUserId); ins.setInt(2,catId); ins.setDouble(3,limit); ins.setString(4,period); ins.executeUpdate();
                log("Budget set: Category "+catId+" = Rs."+limit+" for "+period+" ("+activeUserName+")");
                loadBudgetTable(loadBudgetTableModel(period));
            } catch (Exception ex) { log("Budget add: " + ex.getMessage()); }
        });
        delBtn.addActionListener(e -> {
            try {
                int catId = catBox.getSelectedIndex() + 1;
                String period = periodF.getText().trim();
                PreparedStatement del = DBConnection.getConnection().prepareStatement(
                        "DELETE FROM BUDGET WHERE user_id=? AND category_id=? AND period=?");
                del.setInt(1,activeUserId); del.setInt(2,catId); del.setString(3,period); del.executeUpdate();
                log("Budget deleted: Category "+catId+" period "+period);
                loadBudgetTable(loadBudgetTableModel(periodF.getText().trim()));
            } catch (Exception ex) { log("Budget del: " + ex.getMessage()); }
        });

        JPanel viewBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        viewBar.setBackground(CARD);
        viewBar.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        JTextField viewPeriod = darkField(10);
        viewPeriod.setText(new SimpleDateFormat("yyyy-MM").format(new Date()));
        JButton viewBtn = btn(" View ", ACCENT, true);
        viewBar.add(lbl("View period:")); viewBar.add(viewPeriod); viewBar.add(viewBtn);

        DefaultTableModel bModel = loadBudgetTableModel(new SimpleDateFormat("yyyy-MM").format(new Date()));
        JTable bTable = buildTable(bModel);
        JScrollPane bsc = scroll(bTable);

        viewBtn.addActionListener(e -> {
            bModel.setRowCount(0);
            DefaultTableModel fresh = loadBudgetTableModel(viewPeriod.getText().trim());
            for (int i = 0; i < fresh.getRowCount(); i++) bModel.addRow(new Object[]{
                    fresh.getValueAt(i,0),fresh.getValueAt(i,1),fresh.getValueAt(i,2),
                    fresh.getValueAt(i,3),fresh.getValueAt(i,4),fresh.getValueAt(i,5)
            });
        });

        JPanel center = op(new BorderLayout(0, 10));
        center.add(viewBar, BorderLayout.NORTH);
        center.add(bsc, BorderLayout.CENTER);

        p.add(title,   BorderLayout.NORTH);
        p.add(addCard, BorderLayout.CENTER);
        p.add(center,  BorderLayout.SOUTH);
        return p;
    }

    private DefaultTableModel loadBudgetTableModel(String period) {
        DefaultTableModel m = new DefaultTableModel(new String[]{"Category","Budget","Spent","Remaining","% Used","Status"}, 0) { public boolean isCellEditable(int r,int c){return false;} };
        try {
            PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                    "SELECT c.category_name,b.limit_amount bud,COALESCE(SUM(t.amount),0) spent " +
                            "FROM BUDGET b JOIN CATEGORY c ON b.category_id=c.category_id " +
                            "LEFT JOIN TRANSACTION t ON t.category_id=b.category_id AND t.user_id=b.user_id " +
                            "AND DATE_FORMAT(t.transaction_date,'%Y-%m')=? " +
                            "WHERE b.user_id=? AND b.period=? GROUP BY c.category_name,b.limit_amount");
            ps.setString(1,period); ps.setInt(2,activeUserId); ps.setString(3,period);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double bud=rs.getDouble("bud"),spent=rs.getDouble("spent"),rem=bud-spent,pct=bud>0?spent/bud*100:0;
                String status = spent>bud ? "OVER BUDGET" : pct>80 ? "WARNING >80%" : "OK";
                m.addRow(new Object[]{rs.getString("category_name"),String.format("%.0f",bud),
                        String.format("%.0f",spent),String.format("%.0f",rem),String.format("%.1f%%",pct),status});
            }
        } catch (SQLException e) { log("Budget view: " + e.getMessage()); }
        return m;
    }

    private void loadBudgetTable(DefaultTableModel m) {
        if (budgetModel != null) {
            budgetModel.setRowCount(0);
            for (int i = 0; i < m.getRowCount(); i++) budgetModel.addRow(new Object[]{
                    m.getValueAt(i,0),m.getValueAt(i,1),m.getValueAt(i,2),
                    m.getValueAt(i,3),m.getValueAt(i,4),m.getValueAt(i,5)});
        }
    }

    // ═════════════════════ 6: SQL RUNNER ═════════════════════════
    private JPanel buildSqlRunnerPanel() {
        JPanel p = bg(new BorderLayout(0, 12));
        p.setBorder(pad(20));
        JLabel title = new JLabel("SQL Query Runner");
        title.setFont(FT);
        title.setForeground(TEXT);

        JPanel inputCard = cardPanel("  Enter SQL Query");
        inputCard.setLayout(new BorderLayout(0, 8));
        inputCard.setBorder(BorderFactory.createCompoundBorder(inputCard.getBorder(), pad(12)));

        sqlInputArea = new JTextArea(6, 60);
        sqlInputArea.setFont(FM);
        sqlInputArea.setBackground(new Color(8, 12, 24));
        sqlInputArea.setForeground(new Color(180, 215, 180));
        sqlInputArea.setCaretColor(Color.WHITE);
        sqlInputArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        sqlInputArea.setText("SELECT * FROM TRANSACTION WHERE user_id=" + activeUserId + " ORDER BY transaction_id DESC LIMIT 10;");
        JScrollPane sqlSc = new JScrollPane(sqlInputArea);
        sqlSc.setBorder(BorderFactory.createLineBorder(ACCENT, 1, true));

        // Preset buttons
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        presets.setOpaque(false);
        presets.add(lbl("Presets: "));
        String[][] presetQ = {
                {"My Transactions",   "SELECT * FROM TRANSACTION WHERE user_id="+activeUserId+" ORDER BY transaction_id DESC LIMIT 20;"},
                {"All BANK_SMS",      "SELECT sms_id,bank_name,tx_type,amount,tx_date,merchant_clean,parse_status FROM BANK_SMS ORDER BY sms_id DESC LIMIT 20;"},
                {"Category Spending", "SELECT c.category_name, COUNT(*) cnt, SUM(t.amount) total FROM TRANSACTION t JOIN CATEGORY c ON t.category_id=c.category_id WHERE t.user_id="+activeUserId+" GROUP BY c.category_name ORDER BY total DESC;"},
                {"Monthly Trend",     "SELECT DATE_FORMAT(transaction_date,'%Y-%m') month, COUNT(*) cnt, SUM(amount) total FROM TRANSACTION WHERE user_id="+activeUserId+" GROUP BY month ORDER BY month;"},
                {"All Users",         "SELECT * FROM USER;"},
                {"All Budgets",       "SELECT b.*,c.category_name FROM BUDGET b JOIN CATEGORY c ON b.category_id=c.category_id WHERE b.user_id="+activeUserId+";"},
                {"Category Rules",    "SELECT r.rule_id,r.keyword,c.category_name,r.match_type,r.priority FROM CATEGORY_RULES r JOIN CATEGORY c ON r.category_id=c.category_id ORDER BY r.priority LIMIT 30;"},
                {"Top Merchants",     "SELECT m.merchant_name, COUNT(*) visits, SUM(t.amount) total FROM TRANSACTION t JOIN MERCHANT m ON t.merchant_id=m.merchant_id WHERE t.user_id="+activeUserId+" GROUP BY m.merchant_name ORDER BY total DESC LIMIT 10;"},
                {"Failed SMS",        "SELECT sms_id,bank_name,parse_status,failure_reason,LEFT(raw_sms,60) sms FROM BANK_SMS WHERE parse_status='failed';"},
                {"Show Tables",       "SHOW TABLES;"},
        };
        for (String[] pq : presetQ) {
            JButton pb = new JButton(pq[0]);
            pb.setFont(FS);
            pb.setBackground(CARD2);
            pb.setForeground(TEXT);
            pb.setOpaque(true);
            pb.setBorderPainted(true);
            pb.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)));
            pb.setFocusPainted(false);
            pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pb.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { pb.setBackground(SEL); pb.setForeground(Color.WHITE); }
                public void mouseExited(MouseEvent e)  { pb.setBackground(CARD2); pb.setForeground(TEXT); }
            });
            final String q = pq[1];
            pb.addActionListener(e -> sqlInputArea.setText(q));
            presets.add(pb);
        }

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        JButton runBtn   = btn("  Run SELECT  ",  GREEN,  true);
        JButton dmlBtn   = btn("  Run DML/DDL  ", ORANGE, false);
        JButton clearBtn = btn("  Clear  ",        TEXT2,  false);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        runBtn.addActionListener(e  -> runSelectQuery());
        dmlBtn.addActionListener(e  -> runDmlQuery());
        clearBtn.addActionListener(e -> { sqlInputArea.setText(""); if(sqlResultModel!=null){sqlResultModel.setRowCount(0);sqlResultModel.setColumnCount(0);} });
        btnRow.add(runBtn); btnRow.add(dmlBtn); btnRow.add(clearBtn);

        inputCard.add(sqlSc, BorderLayout.CENTER);
        inputCard.add(presets, BorderLayout.SOUTH);

        sqlResultModel = new DefaultTableModel() { public boolean isCellEditable(int r,int c){return false;} };
        JTable rTable = buildTable(sqlResultModel);
        JPanel resultCard = cardPanel("  Query Result");
        resultCard.setLayout(new BorderLayout(0, 0));
        resultCard.setBorder(BorderFactory.createCompoundBorder(resultCard.getBorder(), pad(8)));
        resultCard.add(scroll(rTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputCard, resultCard);
        split.setDividerLocation(270);
        split.setBackground(BG);
        split.setBorder(null);
        split.setDividerSize(6);

        JPanel top2 = op(new BorderLayout(0, 10));
        top2.add(title, BorderLayout.NORTH);
        top2.add(btnRow, BorderLayout.SOUTH);
        p.add(top2, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private void runSelectQuery() {
        if (sqlInputArea == null) return;
        String sql = sqlInputArea.getText().trim();
        if (sql.isEmpty()) return;
        sqlResultModel.setRowCount(0);
        sqlResultModel.setColumnCount(0);
        long start = System.currentTimeMillis();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) sqlResultModel.addColumn(md.getColumnLabel(i));
            int cnt = 0;
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) row[i-1] = rs.getObject(i);
                sqlResultModel.addRow(row);
                cnt++;
            }
            log(cnt + " rows in " + (System.currentTimeMillis()-start) + "ms");
        } catch (SQLException e) {
            log("SQL: " + e.getMessage());
            sqlResultModel.addColumn("Error");
            sqlResultModel.addRow(new Object[]{e.getMessage()});
        }
    }

    private void runDmlQuery() {
        if (sqlInputArea == null) return;
        String sql = sqlInputArea.getText().trim();
        if (sql.isEmpty()) return;
        if (sql.toUpperCase().startsWith("SELECT")) { runSelectQuery(); return; }
        if (!confirm("Run DML/DDL?\n\n" + sql)) return;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            int n = ps.executeUpdate();
            log("DML: " + n + " row(s) affected.");
            refreshStats();
            refreshAllTables();
        } catch (SQLException e) { log("DML: " + e.getMessage()); }
    }

    // ═════════════════════ 7: MANAGE ═════════════════════════════
    private JPanel buildManagePanel() {
        JPanel p = bg(new BorderLayout(0, 14));
        p.setBorder(pad(20));
        JLabel title = new JLabel("Manage  —  Rules & Database");
        title.setFont(FT);
        title.setForeground(TEXT);

        JPanel ruleCard = cardPanel("  Add Category Rule");
        ruleCard.setLayout(new GridBagLayout());
        ruleCard.setBorder(BorderFactory.createCompoundBorder(ruleCard.getBorder(), pad(14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7,10,7,10); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;

        JTextField kwF = darkField(20);
        JComboBox<String> catB = combo("1-Food","2-Entertainment","3-Transport","4-Shopping","5-Travel","6-Utilities");
        JComboBox<String> typB = combo("CONTAINS","EXACT","STARTS");
        JTextField priF = darkField(5); priF.setText("5");
        JButton addR  = btn("  Add Rule  ",  GREEN, true);
        JButton viewR = btn("  View All  ",  ACCENT, true);
        JButton delR  = btn("  Del by ID  ", RED, false);

        g.gridx=0;g.gridy=0;g.weightx=0; ruleCard.add(lbl("Keyword:"),g); g.gridx=1;g.weightx=0.4; ruleCard.add(kwF,g);
        g.gridx=2;g.weightx=0; ruleCard.add(lbl("Category:"),g); g.gridx=3;g.weightx=0.3; ruleCard.add(catB,g);
        g.gridx=4; ruleCard.add(lbl("Match:"),g); g.gridx=5; ruleCard.add(typB,g);
        g.gridx=6; ruleCard.add(lbl("Priority:"),g); g.gridx=7; ruleCard.add(priF,g);
        g.gridx=0;g.gridy=1;g.gridwidth=2; ruleCard.add(addR,g);
        g.gridx=2;g.gridwidth=2; ruleCard.add(viewR,g);
        g.gridx=4; ruleCard.add(delR,g);

        addR.addActionListener(e -> {
            String kw = kwF.getText().trim().toLowerCase();
            if (kw.isEmpty()) { log("Keyword empty"); return; }
            int cat = catB.getSelectedIndex() + 1;
            String mt = (String) typB.getSelectedItem();
            int pri = 5;
            try { pri = Integer.parseInt(priF.getText().trim()); } catch (NumberFormatException ignored) {}
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                    "INSERT INTO CATEGORY_RULES(keyword,category_id,match_type,priority) VALUES(?,?,?,?)")) {
                ps.setString(1,kw); ps.setInt(2,cat); ps.setString(3,mt); ps.setInt(4,pri); ps.executeUpdate();
                CategoryEngine.reloadRules();
                log("Rule: '" + kw + "' -> cat " + cat);
            } catch (SQLException ex) { log("Rule: " + ex.getMessage()); }
        });
        viewR.addActionListener(e -> {
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                    "SELECT r.rule_id,r.keyword,c.category_name,r.match_type,r.priority,r.is_active " +
                            "FROM CATEGORY_RULES r JOIN CATEGORY c ON r.category_id=c.category_id ORDER BY r.priority");
                 ResultSet rs = ps.executeQuery()) {
                DefaultTableModel m = new DefaultTableModel(new String[]{"ID","Keyword","Category","Match","Priority","Active"}, 0) { public boolean isCellEditable(int r2,int c){return false;} };
                while (rs.next()) m.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getBoolean(6)?"Yes":"No"});
                JTable t = buildTable(m);
                JScrollPane sc = scroll(t);
                sc.setPreferredSize(new Dimension(720, 420));
                JOptionPane.showMessageDialog(this, sc, "Category Rules (" + m.getRowCount() + ")", JOptionPane.PLAIN_MESSAGE);
            } catch (SQLException ex) { log("View: " + ex.getMessage()); }
        });
        delR.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "Rule ID to delete:");
            if (id == null || id.isBlank()) return;
            try {
                int n = DBConnection.getConnection().prepareStatement("DELETE FROM CATEGORY_RULES WHERE rule_id=" + id.trim()).executeUpdate();
                CategoryEngine.reloadRules();
                log(n > 0 ? "Rule " + id + " deleted." : "Not found.");
            } catch (SQLException ex) { log("Del rule: " + ex.getMessage()); }
        });

        // DB action buttons
        JPanel dbCard = cardPanel("  Database Actions");
        dbCard.setLayout(new GridLayout(2, 4, 12, 10));
        dbCard.setBorder(BorderFactory.createCompoundBorder(dbCard.getBorder(), pad(14)));

        Object[][] acts = {
                {"Clear All Transactions", RED,    (Runnable)()->{ if(confirm("Delete ALL transactions?")){execSQL("DELETE FROM PARSED_LOG");execSQL("DELETE FROM TRANSACTION");execSQL("DELETE FROM BANK_SMS");refreshStats();refreshAllTables();log("All tx deleted.");}}},
                {"Clear BANK_SMS",         RED,    (Runnable)()->{ if(confirm("Delete all BANK_SMS?")){execSQL("DELETE FROM BANK_SMS");refreshStats();log("BANK_SMS cleared.");}}},
                {"Clear Chart Data",       ORANGE, (Runnable)()->{ execSQL("DELETE FROM CHART_DATA"); log("Charts cleared.");}},
                {"Clear Parse Logs",       ORANGE, (Runnable)()->{ execSQL("DELETE FROM PARSED_LOG"); log("Logs cleared.");}},
                {"Export Charts HTML",     GREEN,  (Runnable)()->{ ChartDataExporter.exportAll(activeUserId,"2024-10"); ChartDataExporter.writeHtmlDashboard(activeUserId,"charts/index.html"); log("Charts exported for "+activeUserName);}},
                {"Reload Rules",           ACCENT, (Runnable)()->{ CategoryEngine.reloadRules(); log("Rules reloaded.");}},
                {"Refresh All Data",       ACCENT2,(Runnable)()->{ refreshStats();refreshAllTables(); log("Refreshed.");}},
                {"Test DB Connection",     TEAL,   (Runnable)()->{ try{DBConnection.getConnection().createStatement().execute("SELECT 1");log("MySQL connection OK.");}catch(SQLException ex){log("DB error: "+ex.getMessage());}}},
        };
        for (Object[] a : acts) {
            Color ac = (Color) a[1];
            JButton ab = new JButton((String) a[0]);
            ab.setFont(new Font("Segoe UI", Font.BOLD, 11));
            ab.setForeground(Color.WHITE);
            ab.setBackground(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 190));
            ab.setOpaque(true);
            ab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ac, 1, true),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            ab.setFocusPainted(false);
            ab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Runnable r = (Runnable) a[2];
            ab.addActionListener(e -> r.run());
            ab.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { ab.setBackground(ac); }
                public void mouseExited(MouseEvent e)  { ab.setBackground(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),190)); }
            });
            dbCard.add(ab);
        }

        JPanel center = op(new GridLayout(2, 1, 0, 14));
        center.add(ruleCard);
        center.add(dbCard);
        p.add(title, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ═════════════════════ 8: USERS ══════════════════════════════
    private JPanel buildUsersPanel() {
        JPanel p = bg(new BorderLayout(0, 14));
        p.setBorder(pad(20));
        JLabel title = new JLabel("User Management");
        title.setFont(FT);
        title.setForeground(TEXT);

        DefaultTableModel um = new DefaultTableModel(new String[]{"ID","Name","Email","Phone","Created"}, 0) { public boolean isCellEditable(int r,int c){return false;} };
        JTable ut = buildTable(um);

        Runnable loadUsers = () -> {
            um.setRowCount(0);
            try (ResultSet rs = DBConnection.getConnection().createStatement().executeQuery("SELECT * FROM USER ORDER BY user_id")) {
                while (rs.next()) um.addRow(new Object[]{rs.getInt("user_id"),rs.getString("name"),rs.getString("email"),nvl(rs.getString("phone"),"-"),rs.getDate("created_at")});
            } catch (SQLException e) { log("Load users: " + e.getMessage()); }
        };
        loadUsers.run();

        JScrollPane usc = scroll(ut);

        JPanel addCard = cardPanel("  Add New User");
        addCard.setLayout(new GridBagLayout());
        addCard.setBorder(BorderFactory.createCompoundBorder(addCard.getBorder(), pad(14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7,10,7,10); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;

        JTextField nameF  = darkField(18);
        JTextField emailF = darkField(22);
        JTextField phoneF = darkField(14);
        JButton addBtn = btn("  Add User  ",     GREEN, true);
        JButton delBtn = btn("  Delete User  ",  RED,   false);
        JButton swBtn  = btn("  Switch To  ",    ACCENT, true);

        g.gridx=0;g.gridy=0;g.weightx=0; addCard.add(lbl("Name:"),g);  g.gridx=1;g.weightx=0.4; addCard.add(nameF,g);
        g.gridx=2;g.weightx=0; addCard.add(lbl("Email:"),g); g.gridx=3;g.weightx=0.4; addCard.add(emailF,g);
        g.gridx=4;g.weightx=0; addCard.add(lbl("Phone:"),g); g.gridx=5;g.weightx=0.3; addCard.add(phoneF,g);
        g.gridx=0;g.gridy=1;g.gridwidth=2; addCard.add(addBtn,g);
        g.gridx=2;g.gridwidth=2; addCard.add(delBtn,g);
        g.gridx=4;g.gridwidth=2; addCard.add(swBtn,g);

        addBtn.addActionListener(e -> {
            String name=nameF.getText().trim(), email=emailF.getText().trim(), phone=phoneF.getText().trim();
            if (name.isEmpty() || email.isEmpty()) { log("Name and Email required."); return; }
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement("INSERT INTO USER(name,email,phone) VALUES(?,?,?)")) {
                ps.setString(1,name); ps.setString(2,email); ps.setString(3,phone.isEmpty()?null:phone); ps.executeUpdate();
                log("User added: " + name); loadUsers.run(); nameF.setText(""); emailF.setText(""); phoneF.setText("");
            } catch (SQLException ex) { log("Add user: " + ex.getMessage()); }
        });
        delBtn.addActionListener(e -> {
            int row = ut.getSelectedRow();
            if (row < 0) { log("Select user to delete."); return; }
            int uid = (int) um.getValueAt(row, 0);
            String uname = (String) um.getValueAt(row, 1);
            if (uid == activeUserId) { log("Cannot delete the active user!"); return; }
            if (!confirm("Delete user '" + uname + "' (ID=" + uid + ")?\nThis will delete all their transactions too!")) return;
            try {
                execSQL("DELETE FROM BUDGET WHERE user_id="+uid);
                execSQL("DELETE FROM PARSED_LOG WHERE transaction_id IN (SELECT transaction_id FROM TRANSACTION WHERE user_id="+uid+")");
                execSQL("DELETE FROM BANK_SMS WHERE transaction_id IN (SELECT transaction_id FROM TRANSACTION WHERE user_id="+uid+")");
                execSQL("DELETE FROM TRANSACTION WHERE user_id="+uid);
                execSQL("DELETE FROM USER WHERE user_id="+uid);
                log("User " + uname + " deleted."); loadUsers.run();
            } catch (Exception ex) { log("Del user: " + ex.getMessage()); }
        });
        swBtn.addActionListener(e -> {
            int row = ut.getSelectedRow();
            if (row < 0) { log("Select user to switch to."); return; }
            activeUserId   = (int) um.getValueAt(row, 0);
            activeUserName = (String) um.getValueAt(row, 1);
            updateUserBadge(); updateSidebarUser();
            refreshStats(); refreshAllTables();
            log("Switched to: " + activeUserName + " (ID=" + activeUserId + ")");
            showPanel(activeNav);
        });

        JPanel viewCard = cardPanel("  All Users");
        viewCard.setLayout(new BorderLayout(0, 8));
        viewCard.setBorder(BorderFactory.createCompoundBorder(viewCard.getBorder(), pad(10)));
        JPanel refRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        refRow.setOpaque(false);
        JButton refBtn = btn(" Refresh ", ACCENT, true);
        refBtn.addActionListener(e -> loadUsers.run());
        refRow.add(refBtn);
        viewCard.add(refRow, BorderLayout.NORTH);
        viewCard.add(usc, BorderLayout.CENTER);

        JPanel center = op(new GridLayout(2, 1, 0, 14));
        center.add(addCard);
        center.add(viewCard);
        p.add(title, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ═════════════════════ 9: LOG ════════════════════════════════
    private JPanel buildLogPanel() {
        JPanel p = bg(new BorderLayout(0, 8));
        p.setBorder(pad(20));
        JLabel title = new JLabel("Application Log");
        title.setFont(FT);
        title.setForeground(TEXT);

        if (logArea == null) {
            logArea = new JTextArea();
            logArea.setFont(FM);
            logArea.setBackground(new Color(5, 8, 18));
            logArea.setForeground(new Color(120, 255, 120));
            logArea.setEditable(false);
            logArea.setLineWrap(true);
            logArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        }
        JScrollPane sc = new JScrollPane(logArea);
        sc.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        JButton clrBtn = btn(" Clear Log ", TEXT2, false);
        JButton cpyBtn = btn(" Copy All  ", ACCENT, true);
        clrBtn.addActionListener(e -> { logArea.setText(""); log("Log cleared."); });
        cpyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(logArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            log("Log copied to clipboard.");
        });
        btns.add(clrBtn); btns.add(cpyBtn);

        p.add(title, BorderLayout.NORTH);
        p.add(sc, BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ═══════════════════ STATUS BAR ══════════════════════════════
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(8, 10, 18));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(5, 16, 5, 16)));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FS);
        statusLabel.setForeground(GREEN);

        JLabel db = new JLabel("finance_db @ localhost:3306   |   Active: " + activeUserName);
        db.setFont(FS);
        db.setForeground(TEXT2);

        p.add(statusLabel, BorderLayout.WEST);
        p.add(db, BorderLayout.EAST);
        return p;
    }

    // ════════════════════ HELPERS ══════════════════════════════════
    private void log(String msg) {
        String line = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg;
        if (logArea != null) SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        if (statusLabel != null) SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg.length() > 100 ? msg.substring(0, 100) + "..." : msg);
            statusLabel.setForeground(
                    msg.contains("error") || msg.contains("Error") || msg.contains("Cannot") ? RED :
                            msg.contains("OK") || msg.contains("Saved") || msg.contains("added") ? GREEN :
                            msg.contains("WARNING") || msg.contains("DUPLICATE") ? ORANGE : ACCENT);
        });
        System.out.println(line);
    }

    private void refreshAllTables() { loadTransactions(); loadReports(); }
    private void execSQL(String sql) {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) { ps.executeUpdate(); }
        catch (SQLException e) { log("SQL: " + e.getMessage()); }
    }
    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
    private String nvl(String s, String d) { return s != null ? s : d; }
    private void startClock() {
        clockTimer = new javax.swing.Timer(1000, e -> {
            if (clockLabel != null) clockLabel.setText(new SimpleDateFormat("EEE, dd MMM yyyy  HH:mm:ss").format(new Date()));
        });
        clockTimer.start();
    }

    // ── UI Factories ──────────────────────────────────────────────
    private JPanel bg(LayoutManager lm)  { JPanel p = new JPanel(lm); p.setBackground(BG); return p; }
    private JPanel op(LayoutManager lm)  { JPanel p = new JPanel(lm); p.setOpaque(false); return p; }
    private JPanel fp(int align, int hgap, int vgap) { JPanel p = new JPanel(new FlowLayout(align, hgap, vgap)); p.setOpaque(false); return p; }
    private Border pad(int n) { return BorderFactory.createEmptyBorder(n, n, n, n); }

    /** Renamed from card() to cardPanel() to avoid shadowing the local variable 'c' in loops */
    private JPanel cardPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        Border b = title != null
                ? BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                title, TitledBorder.LEFT, TitledBorder.TOP, FS, TEXT2)
                : BorderFactory.createLineBorder(BORDER, 1);
        p.setBorder(b);
        return p;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(FB);
        l.setForeground(TEXT);
        return l;
    }

    private JTextField darkField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(CARD2);
        f.setForeground(TEXT);
        f.setCaretColor(Color.WHITE);
        f.setFont(FB);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return f;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(CARD2);
        c.setForeground(TEXT);
        c.setFont(FB);
        c.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        return c;
    }

    /**
     * btn() — always visible.
     * filled=true  → coloured bg, white text
     * filled=false → dark bg, coloured text + coloured border
     * Hover always brings text to white.
     */
    private JButton btn(String text, Color accent, boolean filled) {
        JButton b = new JButton(text);
        b.setFont(FB);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (filled) {
            b.setBackground(accent);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent.darker(), 1, true),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        } else {
            b.setBackground(CARD2);
            b.setForeground(accent);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1, true),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        }
        final Color origBg = b.getBackground();
        final Color origFg = b.getForeground();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(filled ? accent.brighter() : accent); b.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { b.setBackground(origBg); b.setForeground(origFg); }
        });
        return b;
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? SEL : (row % 2 == 0 ? ROW1 : ROW2));
                c.setForeground(TEXT);
                return c;
            }
        };
        t.setFont(FB);
        t.setRowHeight(27);
        t.setBackground(ROW1);
        t.setForeground(TEXT);
        t.setGridColor(new Color(30, 40, 70));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(SEL);
        t.setSelectionForeground(Color.WHITE);
        JTableHeader h = t.getTableHeader();
        h.setBackground(HDR);
        h.setForeground(ACCENT);
        h.setFont(FH);
        h.setReorderingAllowed(false);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        return t;
    }

    private JScrollPane scroll(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setBackground(CARD);
        s.getViewport().setBackground(CARD);
        s.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        return s;
    }

    // ═══════════════════ MAIN ════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("OptionPane.background",        new Color(22, 27, 42));
        UIManager.put("Panel.background",             new Color(22, 27, 42));
        UIManager.put("OptionPane.messageForeground", new Color(225, 232, 255));
        UIManager.put("Button.background",            new Color(30, 36, 56));
        UIManager.put("Button.foreground",            new Color(225, 232, 255));
        SwingUtilities.invokeLater(() -> new FinanceGUI().setVisible(true));
    }
}