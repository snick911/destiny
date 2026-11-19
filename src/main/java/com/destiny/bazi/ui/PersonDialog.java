package com.destiny.bazi.ui;

import com.destiny.bazi.core.BaziCalculator;
import com.destiny.bazi.core.BaziResult;
import com.destiny.bazi.io.BirthInput;
import com.destiny.bazi.io.PersonStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 人員查詢小視窗：列出已儲存的人員（姓名、出生時間、性別、八字），可依姓名、日期或八字
 * 即時篩選，選定後「帶入」主畫面重新排盤，也可刪除人員。
 *
 * <p>操作：在搜尋框輸入文字即篩選；↑／↓ 移動選取；Enter 或雙擊列即帶入；
 * Delete 刪除選取的人員；Esc 關閉。</p>
 */
public final class PersonDialog extends JDialog {

    private static final float BASE_SIZE = 15f;

    private final PersonStore store;
    private final Consumer<BirthInput> onLoad;
    private final BaziCalculator calculator = new BaziCalculator();

    private final JTextField searchField = new JTextField(16);
    private final PersonTableModel model = new PersonTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<PersonTableModel> sorter = new TableRowSorter<>(model);
    private final JLabel searchLabel = new JLabel("搜尋");
    private final JLabel countLabel = new JLabel(" ");
    private final JButton loadButton = new JButton("帶入");
    private final JButton deleteButton = new JButton("刪除");
    private final JButton closeButton = new JButton("關閉");
    private final JButton importButton = new JButton("匯入 CSV…");
    private final JButton exportButton = new JButton("匯出 CSV…");
    private Runnable importAction;
    private Runnable exportAction;

    public PersonDialog(Frame owner, PersonStore store, Consumer<BirthInput> onLoad) {
        super(owner, "人員查詢", false);
        this.store = store;
        this.onLoad = onLoad;

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(DarkTheme.BG_PANEL);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // 搜尋列
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        searchLabel.setForeground(DarkTheme.FG_SECONDARY);
        DarkTheme.styleTextField(searchField);
        searchField.setToolTipText("輸入姓名、出生日期（如 1990-05）或干支（如 庚午）篩選");
        top.add(searchLabel, BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        // 人員表格
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DarkTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    loadSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER));
        scroll.getViewport().setBackground(DarkTheme.BG_CARD);
        DarkTheme.styleScrollPane(scroll);
        root.add(scroll, BorderLayout.CENTER);

        // 底部：筆數與按鈕
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        countLabel.setForeground(DarkTheme.FG_SECONDARY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        DarkTheme.styleAccentButton(loadButton);
        loadButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        DarkTheme.styleSecondaryButton(deleteButton);
        DarkTheme.styleSecondaryButton(closeButton);
        loadButton.setToolTipText("將選取的人員帶入主畫面並排盤（Enter／雙擊）");
        deleteButton.setToolTipText("刪除選取的人員（Delete）");
        loadButton.addActionListener(e -> loadSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        closeButton.addActionListener(e -> setVisible(false));
        buttons.add(deleteButton);
        buttons.add(closeButton);
        buttons.add(loadButton);
        JPanel csvButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        csvButtons.setOpaque(false);
        DarkTheme.styleSecondaryButton(importButton);
        DarkTheme.styleSecondaryButton(exportButton);
        importButton.setToolTipText("從 CSV 檔匯入人員（欄位：姓名,曆法,年,月,日,閏月,時,分,性別）");
        exportButton.setToolTipText("把全部人員匯出為 CSV 檔");
        importButton.addActionListener(e -> {
            if (importAction != null) {
                importAction.run();
            }
        });
        exportButton.addActionListener(e -> {
            if (exportAction != null) {
                exportAction.run();
            }
        });
        importButton.setVisible(false);
        exportButton.setVisible(false);
        csvButtons.add(importButton);
        csvButtons.add(exportButton);
        csvButtons.add(countLabel);
        bottom.add(csvButtons, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        installKeys(root);

        setContentPane(root);
        setFontScale(1.0);
        setSize(760, 460);
        setMinimumSize(new Dimension(460, 300));
        setLocationRelativeTo(owner);
    }

    private void installKeys(JComponent root) {
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", action(() -> setVisible(false)));
        // 搜尋框內：Enter 帶入、↑↓ 移動表格選取，不必離開搜尋框
        searchField.addActionListener(e -> loadSelected());
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "next");
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "previous");
        searchField.getActionMap().put("next", action(() -> moveSelection(1)));
        searchField.getActionMap().put("previous", action(() -> moveSelection(-1)));
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "load");
        table.getActionMap().put("load", action(this::loadSelected));
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getActionMap().put("delete", action(this::deleteSelected));
    }

    private static Action action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        };
    }

    /** 開啟視窗：重新讀取人員資料、清空搜尋並把焦點放在搜尋框。 */
    public void open() {
        refresh();
        searchField.setText("");
        setVisible(true);
        toFront();
        searchField.requestFocusInWindow();
    }

    /** 重新讀取人員資料檔（主畫面儲存人員後呼叫，讓已開啟的視窗同步更新）。 */
    public void refresh() {
        String selectedName = selectedPerson() == null ? null : selectedPerson().name();
        try {
            model.setPeople(store.list(), calculator);
        } catch (IOException ex) {
            model.setPeople(new ArrayList<>(), calculator);
            JOptionPane.showMessageDialog(this, "讀取人員資料失敗：" + ex.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
        applyFilter();
        if (selectedName != null) {
            selectByName(selectedName);
        }
    }

    /** 設定「匯入 CSV」「匯出 CSV」按鈕的動作（設定後才顯示這兩個按鈕）。 */
    public void setCsvActions(Runnable importAction, Runnable exportAction) {
        this.importAction = importAction;
        this.exportAction = exportAction;
        importButton.setVisible(importAction != null);
        exportButton.setVisible(exportAction != null);
    }

    public void setFontScale(double scale) {
        Font font = DarkTheme.BASE_FONT.deriveFont((float) (BASE_SIZE * scale));
        for (JComponent c : List.of(searchField, searchLabel, countLabel, table, table.getTableHeader(),
                deleteButton, closeButton, importButton, exportButton)) {
            c.setFont(font);
        }
        loadButton.setFont(font.deriveFont(Font.BOLD));
        table.setRowHeight(table.getFontMetrics(font).getHeight() + 12);
        revalidate();
        repaint();
    }

    private void applyFilter() {
        String text = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends PersonTableModel, ? extends Integer> entry) {
                    for (int c = 0; c < entry.getValueCount(); c++) {
                        if (entry.getStringValue(c).toLowerCase(Locale.ROOT).contains(text)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        if (table.getSelectedRow() < 0 && table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
        int total = model.getRowCount();
        if (total == 0) {
            countLabel.setText("尚無人員資料：在主畫面輸入姓名後按「儲存人員」即可加入");
        } else if (text.isEmpty()) {
            countLabel.setText("共 " + total + " 人");
        } else {
            countLabel.setText("符合 " + table.getRowCount() + " 人（共 " + total + " 人）");
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean has = selectedPerson() != null;
        loadButton.setEnabled(has);
        deleteButton.setEnabled(has);
    }

    private void moveSelection(int delta) {
        int rows = table.getRowCount();
        if (rows == 0) {
            return;
        }
        int row = Math.max(0, Math.min(rows - 1, table.getSelectedRow() + delta));
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    private void selectByName(String name) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (model.get(table.convertRowIndexToModel(row)).name().equals(name)) {
                table.setRowSelectionInterval(row, row);
                return;
            }
        }
    }

    private BirthInput selectedPerson() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.get(table.convertRowIndexToModel(row));
    }

    private void loadSelected() {
        BirthInput person = selectedPerson();
        if (person != null) {
            onLoad.accept(person);
            setVisible(false);
        }
    }

    private void deleteSelected() {
        BirthInput person = selectedPerson();
        if (person == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "確定要刪除「" + person.name() + "」的資料？",
                "刪除人員", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            store.remove(person.name());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "刪除失敗：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
        table.clearSelection();
        refresh();
    }

    /** 表格資料：每列一位人員，八字於載入時預先算好以供顯示與搜尋。 */
    private static final class PersonTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"姓名", "出生時間", "性別", "八字"};
        private final List<BirthInput> people = new ArrayList<>();
        private final List<String> charts = new ArrayList<>();

        void setPeople(List<BirthInput> list, BaziCalculator calculator) {
            people.clear();
            charts.clear();
            for (BirthInput p : list) {
                people.add(p);
                BaziResult r = calculator.calculate(p.year(), p.month(), p.day(), p.hour(), p.minute());
                charts.add(r.getYearPillar().getGanZhi() + " " + r.getMonthPillar().getGanZhi() + " "
                        + r.getDayPillar().getGanZhi() + " " + r.getHourPillar().getGanZhi());
            }
            fireTableDataChanged();
        }

        BirthInput get(int row) {
            return people.get(row);
        }

        @Override
        public int getRowCount() {
            return people.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            BirthInput p = people.get(row);
            return switch (column) {
                case 0 -> p.name();
                case 1 -> String.format("%04d-%02d-%02d %02d:%02d", p.year(), p.month(), p.day(), p.hour(), p.minute());
                case 2 -> p.gender().getLabel();
                default -> charts.get(row);
            };
        }
    }
}
