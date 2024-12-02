import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.sql.*;
import java.time.LocalDate;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class LibraryManagementSystem extends JFrame {
    private Connection conn;
    private JComboBox<String> memberComboBorrow;
    private JComboBox<String> bookComboBorrowSpecific;
    private JComboBox<String> bookCopyComboBorrowSpecific;
    private JComboBox<String> memberComboReturn;
    private JComboBox<String> bookCopyComboReturn;
    private JComboBox<String> authorComboBook;
    private JComboBox<String> bookComboCopy;
    private BufferedImage backgroundImage;

    public LibraryManagementSystem() {
        loadBackgroundImage();
        initializeDatabase();
        initializeComboBoxes(); // Initialize combo boxes before refreshing
        initializeGUI();
        refreshComboBoxes(); // Refresh combo boxes after GUI initialization
    }

    private void loadBackgroundImage() {
        try {
            backgroundImage = ImageIO.read(new File("background.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeComboBoxes() {
        memberComboBorrow = new JComboBox<>();
        bookComboBorrowSpecific = new JComboBox<>();
        bookCopyComboBorrowSpecific = new JComboBox<>();
        memberComboReturn = new JComboBox<>();
        bookCopyComboReturn = new JComboBox<>();
        authorComboBook = new JComboBox<>();
        bookComboCopy = new JComboBox<>();
    }
    private void initializeGUI() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Add tabs
        tabbedPane.addTab("Add ➕", createAddPanel());
        tabbedPane.addTab("Borrow Book 🤝", createBorrowPanel());
        tabbedPane.addTab("Return Book ↩️", createReturnPanel());
        tabbedPane.addTab("Borrowed Books ✔️", createBorrowedBooksPanel());
        tabbedPane.addTab("Search 🔍", createSearchPanel());
        tabbedPane.addTab("Update/Delete 🗑️", createUpdateDeletePanel());

        // Create a panel with a background image
        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(backgroundPanel);

        // Decorate combo boxes with auto-complete
        AutoCompleteDecorator.decorate(memberComboBorrow);
        AutoCompleteDecorator.decorate(bookComboBorrowSpecific);
        AutoCompleteDecorator.decorate(bookCopyComboBorrowSpecific);
        AutoCompleteDecorator.decorate(memberComboReturn);
        AutoCompleteDecorator.decorate(bookCopyComboReturn);
        AutoCompleteDecorator.decorate(authorComboBook);
        AutoCompleteDecorator.decorate(bookComboCopy);
    }
    private void refreshComboBoxes() {
        try {
            // Refresh member combo box for borrowing
            memberComboBorrow.removeAllItems();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Member_Id, First_name, Last_name FROM Member");
            while (rs.next()) {
                memberComboBorrow.addItem(rs.getString("First_name") + " " + rs.getString("Last_name") + " - " + rs.getInt("Member_Id"));
            }

            // Refresh book combo box for borrowing - show only books with available copies
            bookComboBorrowSpecific.removeAllItems();
            rs = stmt.executeQuery("SELECT DISTINCT b.Book_Id, b.Title FROM Book b JOIN Book_copy bc ON b.Book_Id = bc.Book_Id WHERE bc.Status = 'AVAILABLE'");
            while (rs.next()) {
                bookComboBorrowSpecific.addItem(rs.getString("Title") + " - " + rs.getInt("Book_Id"));
            }



            // Refresh book copy combo box for borrowing
            bookCopyComboBorrowSpecific.removeAllItems();
            // rs = stmt.executeQuery("SELECT Copy_Id, Title, Print_date FROM Book_copy JOIN Book ON Book_copy.Book_Id = Book.Book_Id WHERE Status = 'AVAILABLE'");
            // while (rs.next()) {
            //     bookCopyComboBorrowSpecific.addItem(rs.getString("Title") + " - " + rs.getInt("Copy_Id") + " (Print Date: " + rs.getDate("Print_date") + ")");
            // }

            // Refresh member combo box for returning
            memberComboReturn.removeAllItems();
            rs = stmt.executeQuery("SELECT Member_Id, First_name, Last_name FROM Member");
            while (rs.next()) {
                memberComboReturn.addItem(rs.getString("First_name") + " " + rs.getString("Last_name") + " - " + rs.getInt("Member_Id"));
            }

            // Refresh book copy combo box for returning
            bookCopyComboReturn.removeAllItems();
            rs = stmt.executeQuery("SELECT Copy_Id, Title, Print_date FROM Book_copy JOIN Book ON Book_copy.Book_Id = Book.Book_Id WHERE Status = 'BORROWED'");
            while (rs.next()) {
                bookCopyComboReturn.addItem(rs.getString("Title") + " - " + rs.getInt("Copy_Id") + " (Print Date: " + rs.getDate("Print_date") + ")");
            }

            // Refresh author combo box for adding books
            authorComboBook.removeAllItems();
            rs = stmt.executeQuery("SELECT Author_Id, First_name, Last_name FROM Author");
            while (rs.next()) {
                authorComboBook.addItem(rs.getString("First_name") + " " + rs.getString("Last_name") + " - " + rs.getInt("Author_Id"));
            }

            // Refresh book combo box for adding copies
            bookComboCopy.removeAllItems();
            rs = stmt.executeQuery("SELECT Book_Id, Title FROM Book");
            while (rs.next()) {
                bookComboCopy.addItem(rs.getString("Title") + " - " + rs.getInt("Book_Id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    
    private JPanel createBorrowedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create JTable to display borrowed books
        String[] columnNames = {"Copy ID", "Member Name", "Book Title", "Borrow Date", "Due Date", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable borrowedBooksTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(borrowedBooksTable);

        JButton refreshButton = new JButton("Refresh");

        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(refreshButton, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> {
            try {
                String query = """
                        SELECT bc.Copy_Id, m.First_name, m.Last_name, b.Title, 
                               bc.Borrow_date, bc.Return_date, bc.Status 
                        FROM Book_copy bc
                        JOIN Book b ON bc.Book_Id = b.Book_Id
                        JOIN Member m ON bc.Borrower_Id = m.Member_Id
                        WHERE bc.Status = 'BORROWED'
                        ORDER BY bc.Borrow_date DESC
                        """;

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                // Clear previous results
                tableModel.setRowCount(0);

                // Add results to table
                while (rs.next()) {
                    int copyId = rs.getInt("Copy_Id");
                    String memberName = rs.getString("First_name") + " " + rs.getString("Last_name");
                    String bookTitle = rs.getString("Title");
                    Date borrowDate = rs.getDate("Borrow_date");
                    Date dueDate = rs.getDate("Return_date");
                    String status = rs.getString("Status");

                    // Add row to table with Copy ID
                    tableModel.addRow(new Object[]{copyId, memberName, bookTitle, borrowDate, dueDate, status});
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error retrieving borrowed books: " + ex.getMessage());
            }
        });

        // Custom cell renderer for overdue highlighting
        borrowedBooksTable.setDefaultRenderer(Object.class, (TableCellRenderer) new DefaultTableCellRenderer() {
            @SuppressWarnings("deprecation")
			public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Get the "Due Date" column value (column index 4, as "Copy ID" is now index 0)
                Object dueDateValue = table.getValueAt(row, 4);
                if (dueDateValue instanceof Date dueDate) {
                    // If overdue, set background to red
                    if (dueDate.before(new Date(column, column, column))) {
                        cell.setBackground(Color.RED);
                        cell.setForeground(Color.WHITE);
                    } else {
                        cell.setBackground(Color.WHITE); // Default background
                        cell.setForeground(Color.BLACK);
                    }
                } else {
                    cell.setBackground(Color.WHITE); // Default for non-date cells
                    cell.setForeground(Color.BLACK);
                }

                return cell;
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
                "SET @OLD_SQL_MODE=@@SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'",
                
                "CREATE TABLE IF NOT EXISTS `Author` (" +
                "`Author_Id` INT NOT NULL," +
                "`First_name` VARCHAR(45) NULL," +
                "`Last_name` VARCHAR(45) NULL," +
                "`Bio` VARCHAR(500) NULL," +
                "PRIMARY KEY (`Author_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Book` (" +
                "`Book_Id` INT NOT NULL," +
                "`Publication_date` DATE NULL," +
                "`Title` VARCHAR(45) NULL," +
                "PRIMARY KEY (`Book_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Library_card` (" +
                "`Card_Id` INT NOT NULL," +
                "`Issue_date` DATE NULL," +
                "`Expiry_date` DATE NULL," +
                "PRIMARY KEY (`Card_Id`))" +
                "ENGINE = InnoDB",
                
                "CREATE TABLE IF NOT EXISTS `Member` (" +
                "`Member_Id` INT NOT NULL," +
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
                "`Copy_Id` INT NOT NULL," +
                "`Print_date` DATE NULL," +
                "`Book_Id` INT NOT NULL," +
                "`Borrower_Id` INT NULL," +
                "`Borrow_date` DATE NULL," +
                "`Return_date` DATE NULL," +
                "`Status` ENUM('AVAILABLE', 'BORROWED', 'LOST') DEFAULT 'AVAILABLE'," +
                "PRIMARY KEY (`Copy_Id`, `Book_Id`)," +
                "INDEX `fk_Book_copy_Book1_idx` (`Book_Id` ASC)," +
                "INDEX `fk_Book_copy_Member1_idx` (`Borrower_Id` ASC)," +
                "CONSTRAINT `fk_Book_copy_Book1` " +
                "FOREIGN KEY (`Book_Id`) " +
                "REFERENCES `Book` (`Book_Id`) " +
                "ON DELETE CASCADE " + // Delete book copies when a book is deleted
                "ON UPDATE CASCADE," +
                "CONSTRAINT `fk_Book_copy_Member1` " +
                "FOREIGN KEY (`Borrower_Id`) " +
                "REFERENCES `Member` (`Member_Id`) " +
                "ON DELETE SET NULL " +
                "ON UPDATE CASCADE)" +
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
                "ON DELETE CASCADE " +
                "ON UPDATE CASCADE)" +
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
    
    private JPanel createAddPanel() {
        JTabbedPane addTabbedPane = new JTabbedPane();
        addTabbedPane.addTab("Add Book 📚", createBookPanel());
        addTabbedPane.addTab("Add Member 👤", createMemberPanel());
        addTabbedPane.addTab("Add Author 🖌", createAuthorPanel());
        addTabbedPane.addTab("Add Copy 📖", createCopyPanel());

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        panel.add(addTabbedPane, BorderLayout.CENTER);
        return panel;
    }

   
    
    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Components for adding books
        JTextField bookIdField = new JTextField(20);
        JTextField titleField = new JTextField(20);
        JTextField publicationDateField = new JTextField(20);
        DefaultListModel<String> authorListModel = new DefaultListModel<>();
        JList<String> authorList = new JList<>(authorListModel);
        JScrollPane authorScrollPane = new JScrollPane(authorList);
        JButton addAuthorButton = new JButton("Add Author");
        JButton removeAuthorButton = new JButton("Remove Author"); 

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Book ID:"), gbc);
        gbc.gridx = 1;
        panel.add(bookIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        panel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Authors:"), gbc);
        gbc.gridx = 1;
        panel.add(authorScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(authorComboBook, gbc);
       
        gbc.gridx = 1;gbc.gridy = 3;
        panel.add(addAuthorButton, gbc);

        gbc.gridx = 2; gbc.gridy = 3;
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
            String selectedAuthor = (String) authorComboBook.getSelectedItem();
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
                    "INSERT INTO Book (Book_Id, Title, Publication_date) VALUES (?, ?, ?)"
                );
                bookStmt.setInt(1, Integer.parseInt(bookIdField.getText()));
                bookStmt.setString(2, titleField.getText());
                try {
                    bookStmt.setDate(3, Date.valueOf(publicationDateField.getText()));
                } catch (IllegalArgumentException ee) {
                    JOptionPane.showMessageDialog(panel, "Empty/Incorrect date format.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                bookStmt.executeUpdate();

                // Link authors and book
                for (String author : selectedAuthors) {
                    //System.out.println("Inserting author: " + author);
                    // PreparedStatement authorStmt = conn.prepareStatement(
                    //     "SELECT Author_Id FROM Author WHERE CONCAT(First_name, ' ', Last_name) = ?"
                    // );
                    // authorStmt.setString(1, author);
                    // ResultSet authorRs = authorStmt.executeQuery();
                    int authorId = Integer.parseInt(author.split(" - ")[1]);
                    PreparedStatement wroteStmt = conn.prepareStatement(
                        "INSERT INTO WROTE (Author_Author_Id, Book_Book_Id) VALUES (?, ?)"
                    );
                    wroteStmt.setInt(1, authorId);
                    wroteStmt.setInt(2, Integer.parseInt(bookIdField.getText()));
                    wroteStmt.executeUpdate();
                    
                }

                JOptionPane.showMessageDialog(panel, "Book added successfully!");

                // Clear fields
                bookIdField.setText("");
                titleField.setText("");
                publicationDateField.setText("");
                authorListModel.clear();
                refreshComboBoxes(); // Refresh combo boxes after adding a book

            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Duplicate entry error code
                    JOptionPane.showMessageDialog(panel, "Book ID already exists. Please enter a unique ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(panel, "Error adding book: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid Book ID format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    
    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField memberIdField = new JTextField(20);
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
       
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1;
        panel.add(memberIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);
        
        JButton addButton = new JButton("Add Member");
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        addButton.addActionListener(e -> {
            try {
                    // Validate input fields
                    if (memberIdField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Member ID cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
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
                    "INSERT INTO Library_card (Card_Id, Issue_date, Expiry_date) VALUES (?, ?, ?)"
                );
                int cardId = Integer.parseInt(memberIdField.getText());
                LocalDate issueDate = LocalDate.now();
                LocalDate expiryDate = issueDate.plusYears(1);
                cardStmt.setInt(1, cardId);
                cardStmt.setDate(2, Date.valueOf(issueDate));
                cardStmt.setDate(3, Date.valueOf(expiryDate));
                cardStmt.executeUpdate();
                
                // Create member
                PreparedStatement memberStmt = conn.prepareStatement(
                    "INSERT INTO Member (Member_Id, First_name, Last_name, Email, Phone_number, Library_card_Id) VALUES (?, ?, ?, ?, ?, ?)"
                );
                memberStmt.setInt(1, Integer.parseInt(memberIdField.getText()));
                memberStmt.setString(2, firstNameField.getText());
                memberStmt.setString(3, lastNameField.getText());
                memberStmt.setString(4, emailField.getText());
                memberStmt.setString(5, phoneField.getText());
                memberStmt.setInt(6, cardId);
                memberStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Member added successfully!");
                
                // Clear fields
                memberIdField.setText("");
                firstNameField.setText("");
                lastNameField.setText("");
                emailField.setText("");
                phoneField.setText("");
                refreshComboBoxes();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Duplicate entry error code
                    JOptionPane.showMessageDialog(panel, "Member ID already exists. Please enter a unique ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding member: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid Member ID format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> entityCombo = new JComboBox<>(new String[]{"Books", "Book Copies", "Authors", "Members"});
        JComboBox<String> attributeCombo = new JComboBox<>();
        JComboBox<String> bookCombo = new JComboBox<>();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");

        searchPanel.add(new JLabel("Entity:"));
        searchPanel.add(entityCombo);
        searchPanel.add(new JLabel("Attribute:"));
        searchPanel.add(attributeCombo);
        searchPanel.add(searchField);
        searchPanel.add(bookCombo);
        searchPanel.add(searchButton);

        // Create JTable to display results
        DefaultTableModel tableModel = new DefaultTableModel();
        JTable resultTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        entityCombo.addActionListener(e -> {
            attributeCombo.removeAllItems();
            bookCombo.removeAllItems();
            String selectedEntity = (String) entityCombo.getSelectedItem();
            if ("Books".equals(selectedEntity)) {
                attributeCombo.addItem("Title");
                attributeCombo.addItem("Author");
                attributeCombo.addItem("Publication Date");
                searchField.setVisible(true);
                bookCombo.setVisible(false);
            } else if ("Book Copies".equals(selectedEntity)) {
                attributeCombo.addItem("Book");
                searchField.setVisible(false);
                bookCombo.setVisible(true);
                try {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT Book_Id, Title FROM Book");
                    while (rs.next()) {
                        bookCombo.addItem(rs.getString("Title") + " - " + rs.getInt("Book_Id"));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else if ("Authors".equals(selectedEntity)) {
                attributeCombo.addItem("First Name");
                attributeCombo.addItem("Last Name");
                attributeCombo.addItem("Bio");
                searchField.setVisible(true);
                bookCombo.setVisible(false);
            } else if ("Members".equals(selectedEntity)) {
                attributeCombo.addItem("First Name");
                attributeCombo.addItem("Last Name");
                attributeCombo.addItem("Email");
                attributeCombo.addItem("Phone Number");
                searchField.setVisible(true);
                bookCombo.setVisible(false);
            }
        });

        searchButton.addActionListener(e -> {
            try {
                String selectedEntity = (String) entityCombo.getSelectedItem();
                String selectedAttribute = (String) attributeCombo.getSelectedItem();
                String query;
                if ("Books".equals(selectedEntity)) {
                    tableModel.setColumnIdentifiers(new String[]{"Book ID", "Title", "Authors", "Publication Date"});
                    if ("Title".equals(selectedAttribute)) {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE b.Title LIKE ? " +
                                "GROUP BY b.Book_Id";
                    } else if ("Author".equals(selectedAttribute)) {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE a.First_name LIKE ? OR a.Last_name LIKE ? " +
                                "GROUP BY b.Book_Id";
                    } else {
                        query = "SELECT b.Book_Id, b.Title, GROUP_CONCAT(CONCAT(a.First_name, ' ', a.Last_name) SEPARATOR ', ') AS Authors, b.Publication_date " +
                                "FROM Book b " +
                                "LEFT JOIN WROTE w ON b.Book_Id = w.Book_Book_Id " +
                                "LEFT JOIN Author a ON w.Author_Author_Id = a.Author_Id " +
                                "WHERE b.Publication_date LIKE ? " +
                                "GROUP BY b.Book_Id";
                    }
                    PreparedStatement stmt = conn.prepareStatement(query);
                    String searchTerm = "%" + searchField.getText() + "%";
                    stmt.setString(1, searchTerm);
                    if ("Author".equals(selectedAttribute)) {
                        stmt.setString(2, searchTerm);
                    }
                    ResultSet rs = stmt.executeQuery();

                    // Clear previous results
                    tableModel.setRowCount(0);

                    // Add results to table
                    while (rs.next()) {
                        int bookId = rs.getInt("Book_Id");
                        String title = rs.getString("Title");
                        String authors = rs.getString("Authors");
                        Date publicationDate = rs.getDate("Publication_date");
                        tableModel.addRow(new Object[]{bookId, title, authors, publicationDate});
                    }
                } else if ("Book Copies".equals(selectedEntity)) {
                    tableModel.setColumnIdentifiers(new String[]{"Copy ID", "Title", "Print Date", "Status"});
                    String selectedBook = (String) bookCombo.getSelectedItem();
                    if (selectedBook != null) {
                        int bookId = Integer.parseInt(selectedBook.split(" - ")[1]);
                        query = "SELECT bc.Copy_Id, b.Title, bc.Print_date, bc.Status " +
                                "FROM Book_copy bc " +
                                "JOIN Book b ON bc.Book_Id = b.Book_Id " +
                                "WHERE bc.Book_Id = ?";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setInt(1, bookId);
                        ResultSet rs = stmt.executeQuery();

                        // Clear previous results
                        tableModel.setRowCount(0);

                        // Add results to table
                        while (rs.next()) {
                            int copyId = rs.getInt("Copy_Id");
                            String title = rs.getString("Title");
                            Date printDate = rs.getDate("Print_date");
                            String status = rs.getString("Status");
                            tableModel.addRow(new Object[]{copyId, title, printDate, status});
                        }
                    }
                } else if ("Authors".equals(selectedEntity)) {
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
                    PreparedStatement stmt = conn.prepareStatement(query);
                    String searchTerm = "%" + searchField.getText() + "%";
                    stmt.setString(1, searchTerm);
                    ResultSet rs = stmt.executeQuery();

                    // Clear previous results
                    tableModel.setRowCount(0);

                    // Add results to table
                    while (rs.next()) {
                        int authorId = rs.getInt("Author_Id");
                        String firstName = rs.getString("First_name");
                        String lastName = rs.getString("Last_name");
                        String bio = rs.getString("Bio");
                        int totalBooks = rs.getInt("total_books");
                        tableModel.addRow(new Object[]{authorId, firstName, lastName, bio, totalBooks});
                    }
                } else if ("Members".equals(selectedEntity)) {
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
                    PreparedStatement stmt = conn.prepareStatement(query);
                    String searchTerm = "%" + searchField.getText() + "%";
                    stmt.setString(1, searchTerm);
                    ResultSet rs = stmt.executeQuery();

                    // Clear previous results
                    tableModel.setRowCount(0);

                    // Add results to table
                    while (rs.next()) {
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

        // Make sure combo boxes are populated correctly
        refreshComboBoxes();

        // Adding labels and combo boxes to the panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Member:"), gbc);
        gbc.gridx = 1;
        panel.add(memberComboBorrow, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        panel.add(bookComboBorrowSpecific, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Select Copy:"), gbc);
        gbc.gridx = 1;
        panel.add(bookCopyComboBorrowSpecific, gbc);

        // Borrow Button
        JButton borrowButton = new JButton("Borrow Book");
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(borrowButton, gbc);

        // Action Listener for book selection to populate available copies
        bookComboBorrowSpecific.addActionListener(e -> {
            bookCopyComboBorrowSpecific.removeAllItems();
            String selectedBook = (String) bookComboBorrowSpecific.getSelectedItem();
            if (selectedBook != null) {
                int bookId = Integer.parseInt(selectedBook.split(" - ")[1]);  // Extract Book ID
                try {
                    PreparedStatement stmt = conn.prepareStatement(
                        "SELECT Copy_Id, Print_date FROM Book_copy WHERE Book_Id = ? AND Status = 'AVAILABLE'"
                    );
                    stmt.setInt(1, bookId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        // Add available copies to the combo box
                        bookCopyComboBorrowSpecific.addItem("Copy ID: " + rs.getInt("Copy_Id") + " (Print Date: " + rs.getDate("Print_date") + ")");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error retrieving book copies: " + ex.getMessage());
                }
            }
        });

        borrowButton.addActionListener(e -> {
            try {
                String memberSelection = (String) memberComboBorrow.getSelectedItem();
                String bookSelection = (String) bookComboBorrowSpecific.getSelectedItem();
                String copySelection = (String) bookCopyComboBorrowSpecific.getSelectedItem();

                // Check if all selections are made
                if (memberSelection == null || bookSelection == null || copySelection == null) {
                    JOptionPane.showMessageDialog(this, "Please select member, book, and copy.");
                    return;
                }

                // Get Member ID
                int memberId = Integer.parseInt(memberSelection.split(" - ")[1]);  // Extract Member ID

                // Extract Copy ID from the selected item using regex
                int copyId = extractCopyId(copySelection);

                if (copyId == -1) {
                    JOptionPane.showMessageDialog(this, "Invalid Copy ID format.");
                    return;
                }

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

                // Update Book Copy to indicate it's borrowed
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Book_copy SET Borrower_Id = ?, Borrow_date = CURRENT_DATE, " +
                    "Return_date = DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), Status = 'BORROWED' " +
                    "WHERE Copy_Id = ?"
                );
                updateStmt.setInt(1, memberId);
                updateStmt.setInt(2, copyId);
                updateStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Book borrowed successfully!");
                refreshComboBoxes(); // Refresh combo boxes after borrowing a book
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error borrowing book: " + ex.getMessage());
            }
        });

        return panel;
    }private int extractCopyId(String copySelection) {
        //return Integer.parseInt(copySelection.split(" - ")[1].split(" ")[0]); // Extract Copy ID from the selected item
        // Regular expression to extract Copy ID
        Pattern pattern = Pattern.compile("Copy ID: (\\d+)"); // Match "Copy ID: <id>"
        Matcher matcher = pattern.matcher(copySelection);
        
        if (matcher.find()) {
            // Return the Copy ID as integer if matched
            return Integer.parseInt(matcher.group(1));
        } else {
            return -1; // Return -1 if no match found
        }
    }

    
    private JPanel createReturnPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        refreshComboBoxes();

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Member:"), gbc);
        gbc.gridx = 1;
        panel.add(memberComboReturn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        panel.add(bookCopyComboReturn, gbc);

        JButton returnButton = new JButton("Return Book");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(returnButton, gbc);

        returnButton.addActionListener(e -> {
            try {
                String memberSelection = (String) memberComboReturn.getSelectedItem();
                String bookSelection = (String) bookCopyComboReturn.getSelectedItem();

                if (memberSelection == null || bookSelection == null) {
                    JOptionPane.showMessageDialog(this, "Please select both member and book");
                    return;
                }

                // Get book copy ID
                int copyId = Integer.parseInt(bookSelection.split(" - ")[1].split(" ")[0]);

                // Update book copy status to 'AVAILABLE'
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Book_copy SET Borrower_Id = NULL, Borrow_date = NULL, Return_date = NULL, Status = 'AVAILABLE' WHERE Copy_Id = ?"
                );
                updateStmt.setInt(1, copyId);
                updateStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Book returned successfully!");
                refreshComboBoxes(); // Refresh combo boxes after returning a book
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
            }
        });

        return panel;
    }

    
    private JPanel createCopyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField copyIdField = new JTextField(20);
        JTextField printDateField = new JTextField(20);
        refreshComboBoxes();
        // Populate book combo box
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Copy ID:"), gbc);
        gbc.gridx = 1;
        panel.add(copyIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        panel.add(bookComboCopy, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Print Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        panel.add(printDateField, gbc);
        
        JButton addButton = new JButton("Add Copy");
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        addButton.addActionListener(e -> {
            try {
                String bookSelection = (String) bookComboCopy.getSelectedItem();
                if (bookSelection == null) {
                    JOptionPane.showMessageDialog(this, "Please select a book");
                    return;
                }
                
                int bookId = Integer.parseInt(bookSelection.split(" - ")[1]);
                
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Book_copy (Copy_Id, Book_Id, Print_date, Status) VALUES (?, ?, ?, 'AVAILABLE')"
                );
                stmt.setInt(1, Integer.parseInt(copyIdField.getText()));
                stmt.setInt(2, bookId);
                stmt.setDate(3, Date.valueOf(printDateField.getText()));
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Copy added successfully!");
                copyIdField.setText("");
                printDateField.setText("");
                refreshComboBoxes(); // Refresh combo boxes after adding a copy
                
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Duplicate entry error code
                    JOptionPane.showMessageDialog(panel, "Copy ID already exists. Please enter a unique ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding copy: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid Copy ID format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid date format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
    private JPanel createAuthorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Components for adding authors
        JTextField authorIdField = new JTextField(20);
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextArea bioArea = new JTextArea(3, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Author ID:"), gbc);
        gbc.gridx = 1;
        panel.add(authorIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Bio:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(bioArea), gbc);
        
        JButton addButton = new JButton("Add Author");
        gbc.gridx = 0; gbc.gridy = 4;
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
                    "INSERT INTO Author (Author_Id, First_name, Last_name, Bio) VALUES (?, ?, ?, ?)"
                );
                authorStmt.setInt(1, Integer.parseInt(authorIdField.getText()));
                authorStmt.setString(2, firstNameField.getText());
                authorStmt.setString(3, lastNameField.getText());
                authorStmt.setString(4, bioArea.getText());
                authorStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Author added successfully!");
                
                // Clear fields
                authorIdField.setText("");
                firstNameField.setText("");
                lastNameField.setText("");
                bioArea.setText("");
                System.out.println("I'm gonna refresh now...");
                refreshComboBoxes(); // Refresh combo boxes after adding an author
                
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Duplicate entry error code
                    JOptionPane.showMessageDialog(panel, "Author ID already exists. Please enter a unique ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding author: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid Author ID format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
    private JPanel createUpdateDeletePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JComboBox<String> entityCombo = new JComboBox<>(new String[]{"Author", "Book", "Member", "Book Copy"});
        JComboBox<String> itemCombo = new JComboBox<>();
        JComboBox<String> bookCombo = new JComboBox<>();
        JLabel selectBookLabel = new JLabel("Select Book:");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Entity:"), gbc);
        gbc.gridx = 1;
        panel.add(entityCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(selectBookLabel, gbc);
        gbc.gridx = 1;
        panel.add(bookCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1;
        panel.add(itemCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(updateButton, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(deleteButton, gbc);

        selectBookLabel.setVisible(false);
        bookCombo.setVisible(false);

        entityCombo.addActionListener(e -> {
            itemCombo.removeAllItems();
            bookCombo.removeAllItems();
            String selectedEntity = (String) entityCombo.getSelectedItem();
            boolean isBookCopy = "Book Copy".equals(selectedEntity);
            selectBookLabel.setVisible(isBookCopy);
            bookCombo.setVisible(isBookCopy);
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs;
                if ("Author".equals(selectedEntity)) {
                    rs = stmt.executeQuery("SELECT Author_Id, First_name, Last_name FROM Author");
                    while (rs.next()) {
                        itemCombo.addItem(rs.getString("First_name") + " " + rs.getString("last_name") + " - " + rs.getInt("Author_Id"));
                    }
                } else if ("Book".equals(selectedEntity)) {
                    rs = stmt.executeQuery("SELECT Book_Id, Title FROM Book");
                    while (rs.next()) {
                        itemCombo.addItem(rs.getString("Title") + " - " + rs.getInt("Book_Id"));
                    }
                } else if ("Member".equals(selectedEntity)) {
                    rs = stmt.executeQuery("SELECT Member_Id, First_name, Last_name FROM Member");
                    while (rs.next()) {
                        itemCombo.addItem(rs.getString("First_name") + " " + rs.getString("last_name") + " - " + rs.getInt("Member_Id"));
                    }
                } else if ("Book Copy".equals(selectedEntity)) {
                    rs = stmt.executeQuery("SELECT Book_Id, Title FROM Book");
                    while (rs.next()) {
                        bookCombo.addItem(rs.getString("Title") + " - " + rs.getInt("Book_Id"));
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        bookCombo.addActionListener(e -> {
            itemCombo.removeAllItems();
            String selectedBook = (String) bookCombo.getSelectedItem();
            if (selectedBook != null) {
                int bookId = Integer.parseInt(selectedBook.split(" - ")[1]);
                try {
                    PreparedStatement stmt = conn.prepareStatement(
                        "SELECT Copy_Id, Print_date, Status FROM Book_copy WHERE Book_Id = ?"
                    );
                    stmt.setInt(1, bookId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        itemCombo.addItem("Copy ID: " + rs.getInt("Copy_Id") + " (Print Date: " + rs.getDate("Print_date") + ", Status: " + rs.getString("Status") + ")");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        updateButton.addActionListener(e -> {
            String entity = (String) entityCombo.getSelectedItem();
            String selectedItem = (String) itemCombo.getSelectedItem();
            if (selectedItem == null) {
                JOptionPane.showMessageDialog(panel, "Please select an item to update.");
                return;
            }

            // Parse ID from selected item safely
            int id = getIdFromSelection(selectedItem);

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
                case "Book Copy":
                    updateBookCopy(id);
                    break;
            }

            refreshComboBoxes();  // Ensure that combo boxes are refreshed after the update
        });

        deleteButton.addActionListener(e -> {
            String entity = (String) entityCombo.getSelectedItem();
            String selectedItem = (String) itemCombo.getSelectedItem();
            if (selectedItem == null) {
                JOptionPane.showMessageDialog(panel, "Please select an item to delete.");
                return;
            }

            // Parse ID from selected item safely
            int id = getIdFromSelection(selectedItem);

            int result = JOptionPane.showConfirmDialog(panel, 
                "Are you sure you want to delete this " + entity + "?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
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
                    case "Book Copy":
                        deleteBookCopy(id);
                        break;
                }
                refreshComboBoxes();
            }
        });

        return panel;
    }


    private int getIdFromSelection(String selectedItem) {
        String[] parts = selectedItem.split(" - ");
        if (selectedItem != null && selectedItem.startsWith("Copy ID:")) {
        	 try {
                 // Extract the ID from the part before the parentheses
                 String idPart = selectedItem.split(" ")[2];  // Splitting the string by space and picking the ID part
                 return Integer.parseInt(idPart);  // Return the extracted ID
             } catch (NumberFormatException ex) {
                 JOptionPane.showMessageDialog(this, "Error parsing ID from selection: " + ex.getMessage());
                 return -1;  // Return an invalid ID to prevent further operations
             }
        }
        if (parts.length == 2) {
            try {
                return Integer.parseInt(parts[1].trim()); // Get the ID after the " - "
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error parsing ID from selection: " + ex.getMessage());
                return -1; // Return an invalid ID to prevent further operations
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid selection format.");
            return -1; // Invalid format, return a dummy ID
        }
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

    
    private void updateBookCopy(int id) {
        if (id == -1) {
            JOptionPane.showMessageDialog(this, "Invalid Book Copy ID.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Check if the book copy exists before updating
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT Copy_Id, Print_date, Status FROM Book_copy WHERE Copy_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book copy with ID " + id + " not found!");
                return;
            }

            // Retrieve current data for the book copy
            String currentPrintDate = rs.getDate("Print_date").toString();
            String currentStatus = rs.getString("Status");

            // Input fields pre-filled with current data
            JTextField printDateField = new JTextField(currentPrintDate, 20);
            JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "BORROWED", "LOST"});
            statusCombo.setSelectedItem(currentStatus);

            // Set up the panel for input
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Print Date (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1;
            panel.add(printDateField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Status:"), gbc);
            gbc.gridx = 1;
            panel.add(statusCombo, gbc);

            // Show dialog
            int result = JOptionPane.showConfirmDialog(this, panel, "Update Book Copy", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Validate and parse the date correctly
                    String printDateText = printDateField.getText();
                    if (!printDateText.matches("\\d{4}-\\d{2}-\\d{2}")) { // Simple check for format
                        JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Update the book copy with new values
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Book_copy SET Print_date = ?, Status = ? WHERE Copy_Id = ?"
                    );
                    stmt.setDate(1, Date.valueOf(printDateText)); // Convert to Date type
                    stmt.setString(2, (String) statusCombo.getSelectedItem()); // Status
                    stmt.setInt(3, id); // Copy ID
                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Book copy updated successfully!");
                    refreshComboBoxes(); // Refresh combo boxes after updating
                    
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating book copy: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Error: check information integrity", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving book copy details: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
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
            // Start transaction
            conn.setAutoCommit(false);
            
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
                // Step 1: Update the status of any borrowed books to "LOST"
                PreparedStatement updateBooksStmt = conn.prepareStatement(
                    "UPDATE Book_copy SET Status = 'LOST' WHERE Borrower_Id = ? AND Status = 'BORROWED'"
                );
                updateBooksStmt.setInt(1, id);
                updateBooksStmt.executeUpdate();

                // Step 2: Delete the library card associated with the member
                PreparedStatement deleteCardStmt = conn.prepareStatement(
                    "DELETE FROM Library_card WHERE Card_Id = (SELECT Library_card_Id FROM Member WHERE Member_Id = ?)"
                );
                deleteCardStmt.setInt(1, id);
                deleteCardStmt.executeUpdate();

                // Step 3: Delete the member record
                PreparedStatement deleteMemberStmt = conn.prepareStatement(
                    "DELETE FROM Member WHERE Member_Id = ?"
                );
                deleteMemberStmt.setInt(1, id);
                deleteMemberStmt.executeUpdate();

                // Commit the transaction
                conn.commit();
                JOptionPane.showMessageDialog(this, "Member and associated library card deleted successfully!");
                refreshComboBoxes();
            }

        } catch (SQLException ex) {
            try {
                // Rollback the transaction in case of error
                conn.rollback();
            } catch (SQLException rollbackEx) {
                JOptionPane.showMessageDialog(this, "Error rolling back transaction: " + rollbackEx.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Error deleting member: " + ex.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);  // Restore auto-commit
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }


    private void deleteBookCopy(int id) {
        try {
            // Check if the book copy exists and retrieve details
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT Copy_Id, Title FROM Book_copy JOIN Book ON Book_copy.Book_Id = Book.Book_Id WHERE Copy_Id = ?"
            );
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book copy not found!");
                return;
            }

            // Retrieve current details of the book copy
            String title = rs.getString("Title");

            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the book copy: \n" +
                "Title: " + title + "\nCopy ID: " + id,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                // Proceed with deletion
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM Book_copy WHERE Copy_Id = ?"
                );
                stmt.setInt(1, id);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Book copy deleted successfully!");
                refreshComboBoxes();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting book copy: " + ex.getMessage());
        }
    }




    
    
    
    // ========================= MAIN ==========================
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                new LibraryManagementSystem().setVisible(true);
                }catch (ClassNotFoundException e) {         
            	JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!");      System.exit(1);    }      
            });    
        }
    }










