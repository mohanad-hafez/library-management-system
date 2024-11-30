import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.sql.*;
import java.time.LocalDate;
import java.util.Vector;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

public class LibraryManagementSystem extends JFrame {
    private Connection conn;
    private JComboBox<String> memberCombo;
    private JComboBox<String> bookCombo;
    private JComboBox<String> bookCopyCombo;
    private JComboBox<String> authorCombo;
    
    public LibraryManagementSystem() {
        initializeDatabase();
        initializeComboBoxes(); // Initialize combo boxes before refreshing
        initializeGUI();
        refreshComboBoxes(); // Refresh combo boxes after GUI initialization
    }

    private void initializeComboBoxes() {
        memberCombo = new JComboBox<>();
        bookCombo = new JComboBox<>();
        bookCopyCombo = new JComboBox<>();
        authorCombo = new JComboBox<>();
    }

    private void refreshComboBoxes() {
        try {
            // Refresh member combo box
            memberCombo.removeAllItems();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT First_name, Last_name FROM Member"
            );
            while (rs.next()) {
                memberCombo.addItem(rs.getString("First_name") + " " + rs.getString("Last_name"));
            }

            // Refresh book combo box
            bookCombo.removeAllItems();
            rs = stmt.executeQuery(
                "SELECT Book_Id, Title FROM Book"
            );
            while (rs.next()) {
                bookCombo.addItem(rs.getInt("Book_Id") + " - " + rs.getString("Title"));
            }

            // Refresh book copy combo box
            bookCopyCombo.removeAllItems();
            rs = stmt.executeQuery("SELECT Title FROM Book");
            while (rs.next()) {
                bookCopyCombo.addItem(rs.getString("Title"));
            }

            // Refresh author combo box
            authorCombo.removeAllItems();
            rs = stmt.executeQuery("SELECT First_name, Last_name FROM Author");

            while (rs.next()) {
                System.out.println(rs.getString("First_name") + " " + rs.getString("Last_name"));
                authorCombo.addItem(rs.getString("First_name") + " " + rs.getString("Last_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    
    private JPanel createBorrowedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create JTable to display borrowed books
        String[] columnNames = {"Member Name", "Book Title", "Borrow Date", "Due Date", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable borrowedBooksTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(borrowedBooksTable);

        JButton refreshButton = new JButton("Refresh");

        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(refreshButton, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> {
            try {
                String query = "SELECT m.First_name, m.Last_name, b.Title, " +
                               "bc.Borrow_date, bc.Return_date, bc.Status " +
                               "FROM Book_copy bc " +
                               "JOIN Book b ON bc.Book_Id = b.Book_Id " +
                               "JOIN Member m ON bc.Borrower_Id = m.Member_Id " +
                               "WHERE bc.Status = 'BORROWED' " +
                               "ORDER BY bc.Borrow_date DESC";

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                // Clear previous results
                tableModel.setRowCount(0);

                // Add results to table
                while (rs.next()) {
                    String memberName = rs.getString("First_name") + " " + rs.getString("Last_name");
                    String bookTitle = rs.getString("Title");
                    Date borrowDate = rs.getDate("Borrow_date");
                    Date dueDate = rs.getDate("Return_date");
                    String status = rs.getString("Status");
                    tableModel.addRow(new Object[]{memberName, bookTitle, borrowDate, dueDate, status});
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error retrieving borrowed books: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void initializeDatabase() {
        try {
            // Create database and tables if they don't exist
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
            Statement stmt = conn.createStatement();
            
            // Create database if not exists
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS library_db");
            stmt.executeUpdate("USE library_db");
            
            // Execute the SQL schema creation script
            String[] sqlStatements = {
                "SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0",
                "SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0",
                "SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'",
                
                "CREATE TABLE IF NOT EXISTS `Author` (" +
                "`Author_Id` INT NOT NULL AUTO_INCREMENT," +
                "`First_name` VARCHAR(45) NULL," +
                "`Last_name` VARCHAR(45) NULL," +
                "`Bio` VARCHAR(500) NULL," +
                "PRIMARY KEY (`Author_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Book` (" +
                "`Book_Id` INT NOT NULL AUTO_INCREMENT," +
                "`Publication_date` DATE NULL," +
                "`Title` VARCHAR(45) NULL," +
                "PRIMARY KEY (`Book_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Library_card` (" +
                "`Card_Id` INT NOT NULL AUTO_INCREMENT," +
                "`Issue_date` DATE NULL," +
                "`Expiry_date` DATE NULL," +
                "PRIMARY KEY (`Card_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Member` (" +
                "`Member_Id` INT NOT NULL AUTO_INCREMENT," +
                "`First_name` VARCHAR(45) NULL," +
                "`Last_name` VARCHAR(45) NULL," +
                "`Email` VARCHAR(45) NULL," +
                "`Phone_number` VARCHAR(10) NULL," +
                "`Library_card_Id` INT NULL," +
                "PRIMARY KEY (`Member_Id`)," +
                "INDEX `fk_Member_Library_card_idx` (`Library_card_Id` ASC)," +
                "UNIQUE INDEX `Library_card_Id_UNIQUE` (`Library_card_Id` ASC)," +
                "CONSTRAINT `fk_Member_Library_card` " +
                "FOREIGN KEY (`Library_card_Id`) " +
                "REFERENCES `Library_card` (`Card_Id`) " +
                "ON DELETE CASCADE " + // deleting the card means deleting the member
                "ON UPDATE CASCADE)" + // updating the card means updating the member
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Book_copy` (" +
                "`Copy_Id` INT NOT NULL AUTO_INCREMENT," +
                "`Print_date` DATE NULL," +
                "`Book_Id` INT NOT NULL," +
                "`Borrower_Id` INT NULL," +
                "`Borrow_date` DATE NULL," +
                "`Return_date` DATE NULL," +
                "`Status` ENUM('AVAILABLE', 'BORROWED', 'DAMAGED') DEFAULT 'AVAILABLE'," +
                "PRIMARY KEY (`Copy_Id`, `Book_Id`)," +
                "INDEX `fk_Book_copy_Book1_idx` (`Book_Id` ASC)," +
                "INDEX `fk_Book_copy_Member1_idx` (`Borrower_Id` ASC)," +
                "CONSTRAINT `fk_Book_copy_Book1` " +
                "FOREIGN KEY (`Book_Id`) " +
                "REFERENCES `Book` (`Book_Id`) " +
                "ON DELETE NO ACTION " + // Delete book copies when a book is deleted
                "ON UPDATE NO ACTION," +
                "CONSTRAINT `fk_Book_copy_Member1` " +
                "FOREIGN KEY (`Borrower_Id`) " +
                "REFERENCES `Member` (`Member_Id`) " +
                "ON DELETE NO ACTION " +
                "ON UPDATE NO ACTION)" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `WROTE` (" +
                "`Author_Author_Id` INT NOT NULL," +
                "`Book_Book_Id` INT NOT NULL," +
                "PRIMARY KEY (`Author_Author_Id`, `Book_Book_Id`)," +
                "INDEX `fk_Author_has_Book_Book1_idx` (`Book_Book_Id` ASC)," +
                "INDEX `fk_Author_has_Book_Author1_idx` (`Author_Author_Id` ASC)," +
                "CONSTRAINT `fk_Author_has_Book_Author1` " +
                "FOREIGN KEY (`Author_Author_Id`) " +
                "REFERENCES `Author` (`Author_Id`) " +
                "ON DELETE CASCADE " + // this doesn't delete a book nor an author but just removes the connection between them.
                "ON UPDATE CASCADE," +
                "CONSTRAINT `fk_Author_has_Book_Book1` " +
                "FOREIGN KEY (`Book_Book_Id`) " +
                "REFERENCES `Book` (`Book_Id`) " +
                "ON DELETE NO ACTION " +
                "ON UPDATE NO ACTION)" +
                "ENGINE = InnoDB"
            };
            
            for (String sql : sqlStatements) {
                try {
                    stmt.executeUpdate(sql);
                } catch (SQLException e) {
                    System.out.println("Warning: " + e.getMessage());
                }
            }
            
            // Reconnect to the specific database
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/library_db", "root", "");
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void initializeGUI() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Add Book", createBookPanel());
        tabbedPane.addTab("Add Member", createMemberPanel());
        tabbedPane.addTab("Add Author", createAuthorPanel());
        tabbedPane.addTab("Borrow Book", createBorrowPanel());
        tabbedPane.addTab("Add Copy", createCopyPanel());
        tabbedPane.addTab("Borrowed Books", createBorrowedBooksPanel());
        tabbedPane.addTab("Search", createSearchPanel());
        tabbedPane.addTab("Update/Delete", createUpdateDeletePanel());
        
        add(tabbedPane);

        // Decorate combo boxes with auto-complete
        AutoCompleteDecorator.decorate(memberCombo);
        AutoCompleteDecorator.decorate(bookCombo);
        AutoCompleteDecorator.decorate(bookCopyCombo);
        AutoCompleteDecorator.decorate(authorCombo);
    }
    
    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Components for adding books
        JTextField titleField = new JTextField(20);
        JTextField publicationDateField = new JTextField(20);
        DefaultListModel<String> authorListModel = new DefaultListModel<>();
        JList<String> authorList = new JList<>(authorListModel);
        JScrollPane authorScrollPane = new JScrollPane(authorList);
        JButton addAuthorButton = new JButton("Add Author");
        JButton removeAuthorButton = new JButton("Remove Author"); 

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        panel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Authors:"), gbc);
        gbc.gridx = 1;
        panel.add(authorScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(authorCombo, gbc);
       
        gbc.gridx = 1;gbc.gridy = 2;
        panel.add(addAuthorButton, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        panel.add(removeAuthorButton, gbc); 

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Publication Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        panel.add(publicationDateField, gbc);

        JButton addButton = new JButton("Add Book");
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);

        // Add author button action
        addAuthorButton.addActionListener(e -> {
            String selectedAuthor = (String) authorCombo.getSelectedItem();
            if (selectedAuthor != null && !authorListModel.contains(selectedAuthor)) {
                authorListModel.addElement(selectedAuthor);
            }
        });

        // Remove author button action
        removeAuthorButton.addActionListener(e -> {
            String selectedAuthor = authorList.getSelectedValue();
            if (selectedAuthor != null) {
                authorListModel.removeElement(selectedAuthor);
            }
        });

        // Add book button action
        addButton.addActionListener(e -> {
            try {
                List<String> selectedAuthors = new ArrayList<>();
                for (int i = 0; i < authorListModel.size(); i++) {
                    selectedAuthors.add(authorListModel.getElementAt(i));
                }
                if (selectedAuthors.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "Please select at least one author");
                    return;
                }

                // Insert book
                PreparedStatement bookStmt = conn.prepareStatement(
                    "INSERT INTO Book (Title, Publication_date) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                bookStmt.setString(1, titleField.getText());
                try {
                    bookStmt.setDate(2, Date.valueOf(publicationDateField.getText()));
                } catch (IllegalArgumentException ee) {
                    JOptionPane.showMessageDialog(panel, "Empty/Incorrect date format.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                bookStmt.executeUpdate();

                ResultSet bookKeys = bookStmt.getGeneratedKeys();
                int bookId = -1;
                if (bookKeys.next()) {
                    bookId = bookKeys.getInt(1);
                }

                // Link authors and book
                for (String author : selectedAuthors) {
                    PreparedStatement authorStmt = conn.prepareStatement(
                        "SELECT Author_Id FROM Author WHERE CONCAT(First_name, ' ', Last_name) = ?"
                    );
                    authorStmt.setString(1, author);
                    ResultSet authorRs = authorStmt.executeQuery();
                    if (authorRs.next()) {
                        int authorId = authorRs.getInt("Author_Id");
                        PreparedStatement wroteStmt = conn.prepareStatement(
                            "INSERT INTO WROTE (Author_Author_Id, Book_Book_Id) VALUES (?, ?)"
                        );
                        wroteStmt.setInt(1, authorId);
                        wroteStmt.setInt(2, bookId);
                        wroteStmt.executeUpdate();
                    }
                }

                JOptionPane.showMessageDialog(panel, "Book added successfully!");

                // Clear fields
                titleField.setText("");
                publicationDateField.setText("");
                authorListModel.clear();
                refreshComboBoxes(); // Refresh combo boxes after adding a book

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error adding book: " + ex.getMessage());
            }
        });

        return panel;
    }

    
    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
       
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);
        
        JButton addButton = new JButton("Add Member");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        addButton.addActionListener(e -> {
            try {
                    // Validate input fields
                    if (firstNameField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "First Name cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (lastNameField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Last Name cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (emailField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Email cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (phoneField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Phone cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                // Create library card
                PreparedStatement cardStmt = conn.prepareStatement(
                    "INSERT INTO Library_card (Issue_date, Expiry_date) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                LocalDate issueDate = LocalDate.now();
                LocalDate expiryDate = issueDate.plusYears(1);
                cardStmt.setDate(1, Date.valueOf(issueDate));
                cardStmt.setDate(2, Date.valueOf(expiryDate));
                cardStmt.executeUpdate();
                
                ResultSet cardKeys = cardStmt.getGeneratedKeys();
                int cardId = -1;
                if (cardKeys.next()) {
                    cardId = cardKeys.getInt(1);
                }
                
                // Create member
                PreparedStatement memberStmt = conn.prepareStatement(
                    "INSERT INTO Member (First_name, Last_name, Email, Phone_number, Library_card_Id) VALUES (?, ?, ?, ?, ?)"
                );
                memberStmt.setString(1, firstNameField.getText());
                memberStmt.setString(2, lastNameField.getText());
                memberStmt.setString(3, emailField.getText());
                memberStmt.setString(4, phoneField.getText());
                memberStmt.setInt(5, cardId);
                memberStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Member added successfully!");
                
                // Clear fields
                firstNameField.setText("");
                lastNameField.setText("");
                emailField.setText("");
                phoneField.setText("");
                refreshComboBoxes();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding member: " + ex.getMessage());
            }
        });
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> entityCombo = new JComboBox<>(new String[]{"Book", "Author", "Member"});
        JComboBox<String> attributeCombo = new JComboBox<>();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        
        searchPanel.add(new JLabel("Entity:"));
        searchPanel.add(entityCombo);
        searchPanel.add(new JLabel("Attribute:"));
        searchPanel.add(attributeCombo);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Create JTable to display results
        DefaultTableModel tableModel = new DefaultTableModel();
        JTable resultTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        entityCombo.addActionListener(e -> {
            attributeCombo.removeAllItems();
            String selectedEntity = (String) entityCombo.getSelectedItem();
            if ("Book".equals(selectedEntity)) {
                attributeCombo.addItem("Title");
                attributeCombo.addItem("Author");
                attributeCombo.addItem("Publication Date");
            } else if ("Author".equals(selectedEntity)) {
                attributeCombo.addItem("First Name");
                attributeCombo.addItem("Last Name");
                attributeCombo.addItem("Bio");
            } else if ("Member".equals(selectedEntity)) {
                attributeCombo.addItem("First Name");
                attributeCombo.addItem("Last Name");
                attributeCombo.addItem("Email");
                attributeCombo.addItem("Phone Number");
            }
        });

        searchButton.addActionListener(e -> {
            try {
                String selectedEntity = (String) entityCombo.getSelectedItem();
                String selectedAttribute = (String) attributeCombo.getSelectedItem();
                String query;
                if ("Book".equals(selectedEntity)) {
                    tableModel.setColumnIdentifiers(new String[]{"Book ID", "Title", "Authors", "Publication Date", "Total Copies", "Available Copies"});
                    if ("Title".equals(selectedAttribute)) {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date, " +
                                "(SELECT COUNT(*) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id) as total_copies, " +
                                "(SELECT COUNT(DISTINCT bc.Copy_Id) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id AND bc.Status = 'AVAILABLE') as available_copies " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE b.Title LIKE ? " +
                                "GROUP BY b.Book_Id";
                    } else if ("Author".equals(selectedAttribute)) {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date, " +
                                "(SELECT COUNT(*) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id) as total_copies, " +
                                "(SELECT COUNT(DISTINCT bc.Copy_Id) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id AND bc.Status = 'AVAILABLE') as available_copies " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE a.First_name LIKE ? OR a.Last_name LIKE ? " +
                                "GROUP BY b.Book_Id";
                    } else {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date, " +
                                "(SELECT COUNT(*) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id) as total_copies, " +
                                "(SELECT COUNT(DISTINCT bc.Copy_Id) FROM Book_copy bc WHERE bc.Book_Id = b.Book_Id AND bc.Status = 'AVAILABLE') as available_copies " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE b.Publication_date LIKE ? " +
                                "GROUP BY b.Book_Id";
                    }
                } else if ("Author".equals(selectedEntity)) {
                    tableModel.setColumnIdentifiers(new String[]{"Author ID", "First Name", "Last Name", "Bio", "Total Books"});
                    if ("First Name".equals(selectedAttribute)) {
                        query = "SELECT a.Author_Id, a.First_name, a.Last_name, a.Bio, COUNT(DISTINCT w.Book_Book_Id) as total_books " +
                                "FROM Author a " +
                                "LEFT JOIN WROTE w ON a.Author_Id = w.Author_Author_Id " +
                                "WHERE a.First_name LIKE ? " +
                                "GROUP BY a.Author_Id";
                    } else if ("Last Name".equals(selectedAttribute)) {
                        query = "SELECT a.Author_Id, a.First_name, a.Last_name, a.Bio, COUNT(DISTINCT w.Book_Book_Id) as total_books " +
                                "FROM Author a " +
                                "LEFT JOIN WROTE w ON a.Author_Id = w.Author_Author_Id " +
                                "WHERE a.Last_name LIKE ? " +
                                "GROUP BY a.Author_Id";
                    } else {
                        query = "SELECT a.Author_Id, a.First_name, a.Last_name, a.Bio, COUNT(DISTINCT w.Book_Book_Id) as total_books " +
                                "FROM Author a " +
                                "LEFT JOIN WROTE w ON a.Author_Id = w.Author_Author_Id " +
                                "WHERE a.Bio LIKE ? " +
                                "GROUP BY a.Author_Id";
                    }
                } else {
                    tableModel.setColumnIdentifiers(new String[]{"Member ID", "First Name", "Last Name", "Email", "Phone Number", "Issue Date", "Expiry Date"});
                    if ("First Name".equals(selectedAttribute)) {
                        query = "SELECT m.Member_Id, m.First_name, m.Last_name, m.Email, m.Phone_number, lc.Issue_date, lc.Expiry_date " +
                                "FROM Member m " +
                                "LEFT JOIN Library_card lc ON m.Library_card_Id = lc.Card_Id " +
                                "WHERE m.First_name LIKE ?";
                    } else if ("Last Name".equals(selectedAttribute)) {
                        query = "SELECT m.Member_Id, m.First_name, m.Last_name, m.Email, m.Phone_number, lc.Issue_date, lc.Expiry_date " +
                                "FROM Member m " +
                                "LEFT JOIN Library_card lc ON m.Library_card_Id = lc.Card_Id " +
                                "WHERE m.Last_name LIKE ?";
                    } else if ("Email".equals(selectedAttribute)) {
                        query = "SELECT m.Member_Id, m.First_name, m.Last_name, m.Email, m.Phone_number, lc.Issue_date, lc.Expiry_date " +
                                "FROM Member m " +
                                "LEFT JOIN Library_card lc ON m.Library_card_Id = lc.Card_Id " +
                                "WHERE m.Email LIKE ?";
                    } else {
                        query = "SELECT m.Member_Id, m.First_name, m.Last_name, m.Email, m.Phone_number, lc.Issue_date, lc.Expiry_date " +
                                "FROM Member m " +
                                "LEFT JOIN Library_card lc ON m.Library_card_Id = lc.Card_Id " +
                                "WHERE m.Phone_number LIKE ?";
                    }
                }

                PreparedStatement stmt = conn.prepareStatement(query);
                String searchTerm = "%" + searchField.getText() + "%";
                stmt.setString(1, searchTerm);
                if ("Book".equals(selectedEntity) && "Author".equals(selectedAttribute)) {
                    stmt.setString(2, searchTerm);
                }

                ResultSet rs = stmt.executeQuery();

                // Clear previous results
                tableModel.setRowCount(0);

                // Add results to table
                while (rs.next()) {
                    if ("Book".equals(selectedEntity)) {
                        int bookId = rs.getInt("Book_Id");
                        String title = rs.getString("Title");
                        String authors = rs.getString("Authors");
                        Date publicationDate = rs.getDate("Publication_date");
                        int totalCopies = rs.getInt("total_copies");
                        int availableCopies = rs.getInt("available_copies");
                        tableModel.addRow(new Object[]{bookId, title, authors, publicationDate, totalCopies, availableCopies});
                    } else if ("Author".equals(selectedEntity)) {
                        int authorId = rs.getInt("Author_Id");
                        String firstName = rs.getString("First_name");
                        String lastName = rs.getString("Last_name");
                        String bio = rs.getString("Bio");
                        int totalBooks = rs.getInt("total_books");
                        tableModel.addRow(new Object[]{authorId, firstName, lastName, bio, totalBooks});
                    } else {
                        int memberId = rs.getInt("Member_Id");
                        String firstName = rs.getString("First_name");
                        String lastName = rs.getString("Last_name");
                        String email = rs.getString("Email");
                        String phone = rs.getString("Phone_number");
                        Date issueDate = rs.getDate("Issue_date");
                        Date expiryDate = rs.getDate("Expiry_date");
                        tableModel.addRow(new Object[]{memberId, firstName, lastName, email, phone, issueDate, expiryDate});
                    }
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error searching: " + ex.getMessage());
            }
        });

        return panel;
    }
    
    private JPanel createBorrowPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Initialize combo boxes
        // memberCombo = new JComboBox<>();
        // bookCombo = new JComboBox<>();
        refreshComboBoxes();
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Member:"), gbc);
        gbc.gridx = 1;
        panel.add(memberCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        panel.add(bookCombo, gbc);
        
        JButton borrowButton = new JButton("Borrow Book");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(borrowButton, gbc);

        
        
        borrowButton.addActionListener(e -> {
            try {
                String memberSelection = (String) memberCombo.getSelectedItem();
                String bookSelection = (String) bookCombo.getSelectedItem();
                
                if (memberSelection == null || bookSelection == null) {
                    JOptionPane.showMessageDialog(this, "Please select both member and book");
                    return;
                }
                
                // Get member ID
                PreparedStatement memberStmt = conn.prepareStatement(
                    "SELECT Member_Id FROM Member WHERE CONCAT(First_name, ' ', Last_name) = ?"
                );
                memberStmt.setString(1, memberSelection);
                ResultSet memberRs = memberStmt.executeQuery();
                memberRs.next();
                int memberId = memberRs.getInt("Member_Id");
                
                // Get book ID
                PreparedStatement bookStmt = conn.prepareStatement(
                    "SELECT Book_Id FROM Book WHERE Title = ?"
                );
                bookStmt.setString(1, bookSelection);
                ResultSet bookRs = bookStmt.executeQuery();
                bookRs.next();
                int bookId = bookRs.getInt("Book_Id");
                
                // Check if member has any overdue books
                PreparedStatement overdueStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Book_copy WHERE Borrower_Id = ? AND Return_date < CURRENT_DATE"
                );
                overdueStmt.setInt(1, memberId);
                ResultSet overdueRs = overdueStmt.executeQuery();
                overdueRs.next();
                if (overdueRs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "Member has overdue books!");
                    return;
                }
                
                // Get available copy
                PreparedStatement copyStmt = conn.prepareStatement(
                    "SELECT Copy_Id FROM Book_copy WHERE Book_Id = ? AND Status = 'AVAILABLE' LIMIT 1"
                );
                copyStmt.setInt(1, bookId);
                ResultSet copyRs = copyStmt.executeQuery();
                
                if (copyRs.next()) {
                    int copyId = copyRs.getInt("Copy_Id");
                    
                    // Update book copy
                    PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE Book_copy SET Borrower_Id = ?, Borrow_date = CURRENT_DATE, " +
                        "Return_date = DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), Status = 'BORROWED' " +
                        "WHERE Copy_Id = ?"
                    );
                    updateStmt.setInt(1, memberId);
                    updateStmt.setInt(2, copyId);
                    updateStmt.executeUpdate();
                    
                    JOptionPane.showMessageDialog(this, "Book borrowed successfully!");
                    
                    // Refresh book combo box
                    bookCombo.removeAllItems();
                    ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT DISTINCT b.Title FROM Book b " +
                        "INNER JOIN Book_copy bc ON b.Book_Id = bc.Book_Id " +
                        "WHERE bc.Status = 'AVAILABLE'"
                    );
                    while (rs.next()) {
                        bookCombo.addItem(rs.getString("Title"));
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No copies available!");
                }
                refreshComboBoxes();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error borrowing book: " + ex.getMessage());
            }
        });
        
        return panel;
    }
    
    private JPanel createCopyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField printDateField = new JTextField(20);
        
        // Populate book combo box
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        panel.add(bookCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Print Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        panel.add(printDateField, gbc);
        
        JButton addButton = new JButton("Add Copy");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        addButton.addActionListener(e -> {
            try {
                String bookSelection = (String) bookCombo.getSelectedItem();
                if (bookSelection == null) {
                    JOptionPane.showMessageDialog(this, "Please select a book");
                    return;
                }
                
                int bookId = Integer.parseInt(bookSelection.split(" - ")[0]);
                
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Book_copy (Book_Id, Print_date, Status) VALUES (?, ?, 'AVAILABLE')"
                );
                stmt.setInt(1, bookId);
                stmt.setDate(2, Date.valueOf(printDateField.getText()));
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Copy added successfully!");
                printDateField.setText("");
                refreshComboBoxes(); // Refresh combo boxes after adding a copy
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding copy: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid book selection format: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid date format.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        });
        
        return panel;
    }
    
    private JPanel createAuthorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Components for adding authors
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextArea bioArea = new JTextArea(3, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Bio:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(bioArea), gbc);
        
        JButton addButton = new JButton("Add Author");
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        // Add author button action
        addButton.addActionListener(e -> {
            try {
            	
            	
            	if (firstNameField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "First Name cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            	if (lastNameField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "Last Name cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            	
                // Insert author
                PreparedStatement authorStmt = conn.prepareStatement(
                    "INSERT INTO Author (First_name, Last_name, Bio) VALUES (?, ?, ?)"
                );
                authorStmt.setString(1, firstNameField.getText());
                authorStmt.setString(2, lastNameField.getText());
                authorStmt.setString(3, bioArea.getText());
                authorStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Author added successfully!");
                
                // Clear fields
                firstNameField.setText("");
                lastNameField.setText("");
                bioArea.setText("");
                System.out.println("I'm gonna refresh now...");
                refreshComboBoxes(); // Refresh combo boxes after adding an author
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding author: " + ex.getMessage());
            }
        });
        
        return panel;
    }
    
    private JPanel createUpdateDeletePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JComboBox<String> entityCombo = new JComboBox<>(new String[]{"Author", "Book", "Member"});
        JTextField idField = new JTextField(20);
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Entity:"), gbc);
        gbc.gridx = 1;
        panel.add(entityCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        panel.add(idField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(updateButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(deleteButton, gbc);
        
        updateButton.addActionListener(e -> {
            String entity = (String) entityCombo.getSelectedItem();
            try {
            	int id = Integer.parseInt(idField.getText());
            	 switch (entity) {
                 case "Author":
                     updateAuthor(id);
                     break;
                 case "Book":
                     updateBook(id);
                     break;
                 case "Member":
                     updateMember(id);
                     break;
             }
            }catch(NumberFormatException ee) {
            	 JOptionPane.showMessageDialog(panel, "Please Enter number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
        });
        
        deleteButton.addActionListener(e -> {
            String entity = (String) entityCombo.getSelectedItem();
            try {
            	 int id = Integer.parseInt(idField.getText());
                 switch (entity) {
                     case "Author":
                         deleteAuthor(id);
                         break;
                     case "Book":
                         deleteBook(id);
                         break;
                     case "Member":
                         deleteMember(id);
                         break;
                 }
            }catch(NumberFormatException ee) {
            	JOptionPane.showMessageDialog(panel, "Please Enter number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
           
        });
        
        return panel;
    }
    
    private void updateAuthor(int id) {
        try {
            // Check if the author exists
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT First_name, Last_name, Bio FROM Author WHERE Author_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Author not found!");
                return;
            }
            
            // Retrieve current data for the author
            String currentFirstName = rs.getString("First_name");
            String currentLastName = rs.getString("Last_name");
            String currentBio = rs.getString("Bio");

            // Input fields pre-filled with current data
            JTextField firstNameField = new JTextField(currentFirstName, 20);
            JTextField lastNameField = new JTextField(currentLastName, 20);
            JTextArea bioArea = new JTextArea(currentBio, 3, 20);

            // Set up the panel for input
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("First Name:"), gbc);
            gbc.gridx = 1;
            panel.add(firstNameField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Last Name:"), gbc);
            gbc.gridx = 1;
            panel.add(lastNameField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Bio:"), gbc);
            gbc.gridx = 1;
            panel.add(new JScrollPane(bioArea), gbc);
            
            // Show dialog
            int result = JOptionPane.showConfirmDialog(this, panel, "Update Author", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Update the author with new values
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Author SET First_name = ?, Last_name = ?, Bio = ? WHERE Author_Id = ?"
                    );
                    stmt.setString(1, firstNameField.getText());
                    stmt.setString(2, lastNameField.getText());
                    stmt.setString(3, bioArea.getText());
                    stmt.setInt(4, id);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Author updated successfully!");
                    refreshComboBoxes();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating author: " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving author details: " + ex.getMessage());
        }
    }

    
    private void updateBook(int id) {
        try {
            // Check if the book exists
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT Title, Publication_date FROM Book WHERE Book_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book not found!");
                return;
            }

            // Retrieve current data for the book
            String currentTitle = rs.getString("Title");
            String currentPublicationDate = rs.getDate("Publication_date").toString();

            // Input fields pre-filled with current data
            JTextField titleField = new JTextField(currentTitle, 20);
            JTextField publicationDateField = new JTextField(currentPublicationDate, 20);

            // Set up the panel for input
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Title:"), gbc);
            gbc.gridx = 1;
            panel.add(titleField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Publication Date (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1;
            panel.add(publicationDateField, gbc);

            // Show dialog
            int result = JOptionPane.showConfirmDialog(this, panel, "Update Book", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Update the book with new values
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Book SET Title = ?, Publication_date = ? WHERE Book_Id = ?"
                    );
                    stmt.setString(1, titleField.getText());
                    
                    stmt.setDate(2, Date.valueOf(publicationDateField.getText()));
                    stmt.setInt(3, id);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Book updated successfully!");
                    refreshComboBoxes();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating book: " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving book details: " + ex.getMessage());
        }catch(IllegalArgumentException ee) {
        	 JOptionPane.showMessageDialog(this, "Error updating: check information integrity", "Input Error", JOptionPane.ERROR_MESSAGE);
             return;
        }
    }

    
    private void updateMember(int id) {
        try {
            // Check if the member exists and retrieve current details
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT First_name, Last_name, Email, Phone_number FROM Member WHERE Member_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Member not found!");
                return;
            }

            // Retrieve current details of the member
            String currentFirstName = rs.getString("First_name");
            String currentLastName = rs.getString("Last_name");
            String currentEmail = rs.getString("Email");
            String currentPhone = rs.getString("Phone_number");

            // Input fields pre-filled with current data
            JTextField firstNameField = new JTextField(currentFirstName, 20);
            JTextField lastNameField = new JTextField(currentLastName, 20);
            JTextField emailField = new JTextField(currentEmail, 20);
            JTextField phoneField = new JTextField(currentPhone, 20);

            // Set up the panel for input
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("First Name:"), gbc);
            gbc.gridx = 1;
            panel.add(firstNameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(firstNameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Last Name:"), gbc);
            gbc.gridx = 1;
            panel.add(lastNameField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            panel.add(emailField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("Phone:"), gbc);
            gbc.gridx = 1;
            panel.add(phoneField, gbc);

            // Show dialog
            int result = JOptionPane.showConfirmDialog(this, panel, "Update Member", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Update the member with new values
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Member SET First_name = ?, Last_name = ?, Email = ?, Phone_number = ? WHERE Member_Id = ?"
                    );
                    stmt.setString(1, firstNameField.getText());
                    stmt.setString(2, lastNameField.getText());
                    stmt.setString(3, emailField.getText());
                    stmt.setString(4, phoneField.getText());
                    stmt.setInt(5, id);
                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Member updated successfully!");
                    refreshComboBoxes();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating member: " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving member details: " + ex.getMessage());
        }
    }

    
    private void deleteAuthor(int id) {
        try {
            // Check if the author exists and retrieve details
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT First_name, Last_name FROM Author WHERE Author_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Author not found!");
                return;
            }

            // Retrieve current details of the author
            String firstName = rs.getString("First_name");
            String lastName = rs.getString("Last_name");

            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the author: \n" +
                "Name: " + firstName + " " + lastName,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                // Proceed with deletion
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM Author WHERE Author_Id = ?"
                );
                stmt.setInt(1, id);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Author deleted successfully!");
                refreshComboBoxes();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting author: " + ex.getMessage());
        }
    }

    
    private void deleteBook(int id) {
        try {
            // Check if the book exists and retrieve details
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT Title, Publication_date FROM Book WHERE Book_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book not found!");
                return;
            }

            // Retrieve current details of the book
            String title = rs.getString("Title");
            String publicationDate = rs.getDate("Publication_date").toString();

            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the book: \n" +
                "Title: " + title + "\nPublication Date: " + publicationDate,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                // Delete related entries in the WROTE table
                PreparedStatement deleteWroteStmt = conn.prepareStatement(
                    "DELETE FROM WROTE WHERE Book_Book_Id = ?"
                );
                deleteWroteStmt.setInt(1, id);
                deleteWroteStmt.executeUpdate();

                // Delete the book
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM Book WHERE Book_Id = ?"
                );
                stmt.setInt(1, id);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Book deleted successfully!");
                refreshComboBoxes();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage());
        }
    }

    
    private void deleteMember(int id) {
        try {
            // Check if the member exists and retrieve details
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT First_name, Last_name, Email, Phone_number FROM Member WHERE Member_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Member not found!");
                return;
            }

            // Retrieve current details of the member
            String firstName = rs.getString("First_name");
            String lastName = rs.getString("Last_name");
            String email = rs.getString("Email");
            String phone = rs.getString("Phone_number");

            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the member: \n" +
                "Name: " + firstName + " " + lastName + "\n" +
                "Email: " + email + "\nPhone: " + phone,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                // Delete the library card associated with the member
                PreparedStatement cardStmt = conn.prepareStatement(
                    "DELETE FROM Library_card WHERE Card_Id = (SELECT Library_card_Id FROM Member WHERE Member_Id = ?)"
                );
                cardStmt.setInt(1, id);
                cardStmt.executeUpdate();

                // Delete the member
                PreparedStatement memberStmt = conn.prepareStatement(
                    "DELETE FROM Member WHERE Member_Id = ?"
                );
                memberStmt.setInt(1, id);
                memberStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Member and associated library card deleted successfully!");
                refreshComboBoxes();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting member: " + ex.getMessage());
        }
    }

    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                new LibraryManagementSystem().setVisible(true);
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!");
                System.exit(1);
            }
        });
    }
}