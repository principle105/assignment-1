import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;

public class Pico {
    private static final String CSV_FILE_PATH = "data.csv";
    private List<HotSauce> sauceList;
    private DefaultTableModel model;

    public Pico() {
        createAndShowGUI();
    }

    public static void main(String[] args) {
        // Creating the GUI on the event dispatch thread to prevent concurrency issues
        SwingUtilities.invokeLater(() -> {
            new Pico();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Pico's Hot Sauce Collection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);

        // Loading the saved sauce data
        try {
            sauceList = loadCSVData(CSV_FILE_PATH);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Some data was malformed. There may be some data loss.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            sauceList = new ArrayList<>();
        }

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        JPanel mainPage = createLookupPage(cardLayout, cardPanel);
        JPanel addSaucePage = newSaucePage(cardLayout, cardPanel);

        // Saving the sauce data when the window is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveCSVData(CSV_FILE_PATH);
            }
        });

        cardPanel.add(mainPage, "lookupPage");
        cardPanel.add(addSaucePage, "newSaucePage");

        frame.add(cardPanel);

        frame.setVisible(true);
    }

    private JPanel createLookupPage(CardLayout cardLayout, JPanel cardPanel) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Search label
        JLabel titleLabel = new JLabel("Search Hot Sauces");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // Search bars
        JPanel searchBarsPanel = new JPanel();
        searchBarsPanel.setLayout(new FlowLayout());

        JTextField nameSearchField = new JTextField(10);
        JTextField originSearchField = new JTextField(10);
        JTextField heatSearchField = new JTextField(10);

        searchBarsPanel.add(new JLabel("Name: "));
        searchBarsPanel.add(nameSearchField);
        searchBarsPanel.add(new JLabel("Origin: "));
        searchBarsPanel.add(originSearchField);
        searchBarsPanel.add(new JLabel("Heat: "));
        searchBarsPanel.add(heatSearchField);

        topPanel.add(searchBarsPanel, BorderLayout.CENTER);

        // Putting the button in a group to prevent it spanning the whole page
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton addNewSauceButton = new JButton("Add New Sauce");
        addNewSauceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, "newSaucePage");
            }
        });

        buttonPanel.add(addNewSauceButton);

        // Add the buttonPanel to the topPanel
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        model = new DefaultTableModel(HotSauce.getFieldNames().toArray(), 0);
        JTable table = new JTable(model);

        // Increasing the height of the table rows
        table.setRowHeight(30);

        // Populating the table with data on startup
        filterTable(model, sauceList, "", "", "");

        // Updates the table when the search fields are changed
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTable();
            }

            private void updateTable() {
                String nameSearchText = nameSearchField.getText().trim();
                String originSearchText = originSearchField.getText().trim();
                String heatSearchText = heatSearchField.getText().trim();
                filterTable(model, sauceList, nameSearchText, originSearchText, heatSearchText);
            }
        };

        nameSearchField.getDocument().addDocumentListener(documentListener);
        originSearchField.getDocument().addDocumentListener(documentListener);
        heatSearchField.getDocument().addDocumentListener(documentListener);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row < 0 || column < 0) {
                    return;
                }

                Object data = model.getValueAt(row, column);
                String newValue = data.toString();

                HotSauce sauce = sauceList.get(row);

                List<String> sauceParams = sauce.getRowValues();

                sauceParams.set(column, newValue);

                if (newValue.isEmpty()
                        && HotSauce.getRequiredFieldNames().contains(HotSauce.getFieldNames().get(column))) {
                    model.setValueAt(sauce.getRowValues().get(column), row, column);
                    JOptionPane.showMessageDialog(mainPanel, "That field cannot be blank", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    HotSauce newSauce = HotSauce.fromRow(sauceParams);

                    sauceList.set(row, newSauce);

                    // Updating the table to reflect the new data
                    String nameSearchText = nameSearchField.getText().trim();
                    String originSearchText = originSearchField.getText().trim();
                    String heatSearchText = heatSearchField.getText().trim();
                    filterTable(model, sauceList, nameSearchText, originSearchText, heatSearchText);

                    saveCSVData(CSV_FILE_PATH);
                } catch (Exception ex) {
                    model.setValueAt(sauce.getRowValues().get(column), row, column);
                    JOptionPane.showMessageDialog(mainPanel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

                }
            }
        });

        table.setShowGrid(false);

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel newSaucePage(CardLayout cardLayout, JPanel cardPanel) {
        JPanel newSaucePanel = new JPanel();
        newSaucePanel.setLayout(new BorderLayout());
        newSaucePanel.setBorder(BorderFactory.createEmptyBorder(28, 16, 12, 16));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        // Add new sauce label
        JLabel titleLabel = new JLabel("Add New Sauce");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        topPanel.add(titleLabel);

        newSaucePanel.add(topPanel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(0, 2, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 128, 8, 128));

        List<String> requiredFields = HotSauce.getRequiredFieldNames();
        List<String> allFieldNames = HotSauce.getFieldNames();

        // Adding all sauce fields
        JTextField[] newSauceFields = new JTextField[HotSauce.getFieldNames().size()];

        int i = 0;
        for (String field : allFieldNames) {
            String label = field + ":";

            if (requiredFields.contains(field)) {
                // Making asterisk character red
                label = "<html>" + field + ": <font color='red'>*</font></html>";
            }

            inputPanel.add(new JLabel(label));

            newSauceFields[i] = new JTextField(10);
            inputPanel.add(newSauceFields[i]);
            i += 1;
        }

        newSaucePanel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        // Back button
        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JTextField field : newSauceFields) {
                    field.setText("");
                }
                cardLayout.show(cardPanel, "lookupPage");
            }
        });
        buttonPanel.add(backButton);

        // Add row button
        JButton addButton = new JButton("Add Row");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> newSauceParams = new ArrayList<String>();

                List<String> requiredFields = HotSauce.getRequiredFieldNames();
                List<String> allFieldNames = HotSauce.getFieldNames();

                for (int i = 0; i < allFieldNames.size(); i++) {
                    String text = newSauceFields[i].getText().trim();

                    if (text.isEmpty() && requiredFields.contains(allFieldNames.get(i))) {
                        JOptionPane.showMessageDialog(newSaucePanel, allFieldNames.get(i) + " is a required field.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    newSauceParams.add(text);
                }

                try {
                    HotSauce sauce = HotSauce.fromRow(newSauceParams);

                    sauceList.add(sauce);
                    model.addRow(newSauceParams.toArray());

                    for (int i = 0; i < HotSauce.getFieldNames().size(); i++) {
                        newSauceFields[i].setText("");
                    }

                    saveCSVData(CSV_FILE_PATH);

                    // Switching back to the main page
                    cardLayout.show(cardPanel, "lookupPage");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(newSaucePanel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonPanel.add(addButton);

        newSaucePanel.add(buttonPanel, BorderLayout.SOUTH);

        return newSaucePanel;
    }

    private static List<HotSauce> loadCSVData(String filePath) throws Exception {
        List<HotSauce> sauceList = new ArrayList<>();

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                // Skipping the first line because it contains the field names
                br.readLine();

                String line;

                List<String> requiredFields = HotSauce.getRequiredFieldNames();
                List<String> allFields = HotSauce.getFieldNames();

                while ((line = br.readLine()) != null) {
                    List<String> values = Arrays.asList(line.split(","));

                    List<String> newValues = new ArrayList<String>();

                    for (int i = 0; i < allFields.size(); i++) {
                        String value = "";

                        if (i < values.size()) {
                            if (values.get(i).isEmpty() && requiredFields.contains(allFields.get(i))) {
                                throw new Exception(allFields.get(i) + " is a required field.");
                            }
                            value = values.get(i);
                        }
                        newValues.add(value);
                    }

                    sauceList.add(HotSauce.fromRow(newValues));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }

        return sauceList;
    }

    private void saveCSVData(String filePath) {
        try {
            File file = new File(filePath);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                // Writing the field names as the first line
                bw.write(String.join(",", HotSauce.getFieldNames()));
                bw.newLine();

                // Writing the sauce data
                for (HotSauce sauce : sauceList) {
                    String line = String.join(",", sauce.getRowValues());
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to the file: " + e.getMessage());
        }
    }

    private void filterTable(DefaultTableModel model, List<HotSauce> sauceList, String nameSearchText, String originSearchText, String heatSearchText) {
        model.setRowCount(0);

        for (HotSauce sauce : sauceList) {
            if (matchesSearchCriteria(sauce, nameSearchText, originSearchText, heatSearchText)) {
                model.addRow(sauce.getRowValues().toArray());
            }
        }
    }

    private static boolean matchesSearchCriteria(HotSauce sauce, String nameSearchText, String originSearchText, String heatSearchText) {
        // Adapted from: https://stackoverflow.com/a/26407189/21458209
        boolean nameMatches = nameSearchText.isEmpty() || Pattern
                .compile(Pattern.quote(nameSearchText), Pattern.CASE_INSENSITIVE).matcher(sauce.getName()).find();
        boolean originMatches = originSearchText.isEmpty() || Pattern
                .compile(Pattern.quote(originSearchText), Pattern.CASE_INSENSITIVE).matcher(sauce.getOrigin()).find();

        boolean heatMatches = heatSearchText.isEmpty() || sauce.getHeat().equalsIgnoreCase(heatSearchText);

        return nameMatches && originMatches && heatMatches;
    }
}
