package org.example;
import java.sql.*;
import java.util.Scanner;

public class Main {
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/librarymanagement";
    private static final String USER = "root";
    private static final String PASS = "Sabareesh_3936";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             Scanner scanner = new Scanner(System.in)) {



            while (true) {
                System.out.println("\nWelcome To THE BOOK NEST");
                System.out.println("\nLibrary Management System");
                System.out.println("1. Add Book");
                System.out.println("2. View Available Books");
                System.out.println("3. Borrow a Book");
                System.out.println("4. Return a Book");
                System.out.println("5. Register a Member");
                System.out.println("6. View Members");
//                System.out.println("7. View Member Information");
                System.out.println("7. Search Books");
                System.out.println("8. Remove a Book");
                System.out.println("9. Remove a Member");
                System.out.println("10. Exit");
                System.out.print("Choose an option: ");
                int option = scanner.nextInt();
                scanner.nextLine();

                switch (option) {
                    case 1:
                        addBook(connection, scanner);
                        break;
                    case 2:
                        viewAvailableBooks(connection);
                        break;
                    case 3:
                        borrowBook(connection, scanner);
                        break;
                    case 4:
                        returnBook(connection, scanner);
                        break;
                    case 5:
                        registerMember(connection, scanner);
                        break;
                    case 6:
                        viewMembers(connection);
                        break;
                    case 7:
                        viewMemberInformation(connection, scanner);
                        break;
                    case 8:
                        searchBooks(connection, scanner);
                        break;
                    case 9:
                        removeBook(connection, scanner);
                        break;
                    case 10:
                        removeUser(connection, scanner);
                        break;

                    case 11:
                        System.out.println("Exiting the system. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
    }

    private static void addBook(Connection connection, Scanner scanner) {
        System.out.print("Enter book title: ");
        String title = scanner.nextLine();
        System.out.print("Enter author: ");
        String author = scanner.nextLine();
        System.out.print("Enter category: ");
        String category = scanner.nextLine();
        System.out.print("Enter the date (yyyy-MM-dd): ");
        String added_date = scanner.nextLine();


        String sql = "INSERT INTO books (title, author, category,added_date) VALUES (?, ?, ?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, author);
            statement.setString(3, category);
            statement.setString(4,added_date);
            statement.executeUpdate();
            System.out.println("Book added: " + title);
            System.out.println("Book Added Successfully");
        } catch (SQLException e) {
            System.out.println("Error adding book: " + e.getMessage());
        }
    }

    private static void viewAvailableBooks(Connection connection) {
        String sql = "SELECT * FROM books WHERE is_available = TRUE";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No book is currently available.");
            } else {
                System.out.println("\nAvailable Books:");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("title") + " by " +
                            resultSet.getString("author") + " [Category: " +
                            resultSet.getString("category") + "]");
                }
                System.out.println("Books are available.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching books: " + e.getMessage());
        }
    }


    private static void borrowBook(Connection connection, Scanner scanner) {
        System.out.print("Enter member ID: ");
        String memberId = scanner.nextLine();

        System.out.print("Enter book title: ");
        String bookTitle = scanner.nextLine();

        String bookSql = "SELECT id FROM books WHERE title = ? AND is_available = TRUE";
        String memberSql = "SELECT id FROM members WHERE member_id = ?";

        try (PreparedStatement bookStatement = connection.prepareStatement(bookSql);
             PreparedStatement memberStatement = connection.prepareStatement(memberSql)) {

            bookStatement.setString(1, bookTitle);
            ResultSet bookResultSet = bookStatement.executeQuery();

            memberStatement.setString(1, memberId);
            ResultSet memberResultSet = memberStatement.executeQuery();

            if (bookResultSet.next() && memberResultSet.next()) {
                int bookId = bookResultSet.getInt("id");
                int memberID = memberResultSet.getInt("id");

                String borrowSql = "INSERT INTO loans (member_id, book_id, loan_date) VALUES (?, ?, NOW())";
                try (PreparedStatement borrowStatement = connection.prepareStatement(borrowSql)) {
                    borrowStatement.setInt(1, memberID); // Use the correct member internal id
                    borrowStatement.setInt(2, bookId);
                    borrowStatement.executeUpdate();

                    String updateBookSql = "UPDATE books SET is_available = FALSE WHERE id = ?";
                    try (PreparedStatement updateBookStatement = connection.prepareStatement(updateBookSql)) {
                        updateBookStatement.setInt(1, bookId);
                        updateBookStatement.executeUpdate();
                    }

                    System.out.println("Book borrowed: " + bookTitle);
                    System.out.println("Book is barrowed Successfully");
                }
            } else {
                System.out.println("Book is not available or Member not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error borrowing book: " + e.getMessage());
        }
    }


    private static void returnBook(Connection connection, Scanner scanner) {
        System.out.print("Enter member ID: ");
        String memberId = scanner.nextLine();
        System.out.print("Enter book title: ");
        String bookTitle = scanner.nextLine();

        String bookSql = "SELECT id FROM books WHERE title = ?";
        String memberSql = "SELECT id FROM members WHERE member_id = ?";
        String loanSql = "SELECT id FROM loans WHERE member_id = ? AND book_id = ? AND return_date IS NULL";

        try (PreparedStatement bookStatement = connection.prepareStatement(bookSql);
             PreparedStatement memberStatement = connection.prepareStatement(memberSql)) {

            bookStatement.setString(1, bookTitle);
            ResultSet bookResultSet = bookStatement.executeQuery();

            memberStatement.setString(1, memberId);
            ResultSet memberResultSet = memberStatement.executeQuery();

            if (bookResultSet.next() && memberResultSet.next()) {
                int bookId = bookResultSet.getInt("id");
                int memberID = memberResultSet.getInt("id");

                try (PreparedStatement loanStatement = connection.prepareStatement(loanSql)) {
                    loanStatement.setInt(1, memberID);
                    loanStatement.setInt(2, bookId);
                    ResultSet loanResultSet = loanStatement.executeQuery();

                    if (loanResultSet.next()) {

                        String returnSql = "UPDATE loans SET return_date = NOW() WHERE id = ?";
                        try (PreparedStatement returnStatement = connection.prepareStatement(returnSql)) {
                            returnStatement.setInt(1, loanResultSet.getInt("id"));
                            returnStatement.executeUpdate();
                        }

                        String updateBookSql = "UPDATE books SET is_available = TRUE WHERE id = ?";
                        try (PreparedStatement updateBookStatement = connection.prepareStatement(updateBookSql)) {
                            updateBookStatement.setInt(1, bookId);
                            updateBookStatement.executeUpdate();
                        }
                        System.out.println("Book returned: " + bookTitle);
                        System.out.println("Book Returned Successfully");
                    } else {
                        System.out.println("This book was not borrowed by this member.");
                    }
                }
            } else {
                System.out.println("Book or member not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error returning book: " + e.getMessage());
        }
    }

    private static void registerMember(Connection connection, Scanner scanner) {

        String getIdSql = "SELECT MAX(CAST(member_id AS UNSIGNED)) AS max_member_id FROM members";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(getIdSql)) {

            int nextMemberId = 1;
            if (resultSet.next()) {
                int maxMemberId = resultSet.getInt("max_member_id");
                nextMemberId = maxMemberId + 1;
            }


            System.out.println("Auto-generated Member ID: " + nextMemberId);

            System.out.print("Enter member name: ");
            String name = scanner.nextLine();


            System.out.print("Enter mobile number: ");
            String mobileNumber = scanner.nextLine();

            System.out.print("Enter email: ");
            String email = scanner.nextLine();

            System.out.print("Enter address: ");
            String address = scanner.nextLine();

            String sql = "INSERT INTO members (name, member_id, mobile_number, email, address) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, String.valueOf(nextMemberId)); // Auto-generated member ID
                preparedStatement.setString(3, mobileNumber);
                preparedStatement.setString(4, email);
                preparedStatement.setString(5, address);

                preparedStatement.executeUpdate();
                System.out.println("Member registered: " + name + " (ID: " + nextMemberId + ")");
            } catch (SQLException e) {
                System.out.println("Error registering member: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Error generating member ID: " + e.getMessage());
        }
    }



    private static void viewMembers(Connection connection) {
        String sql = "SELECT * FROM members";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("\nRegistered Members:");
            while (resultSet.next()) {
                System.out.println("- " + resultSet.getString("name") + " [ID: " + resultSet.getString("member_id") + "]");
            }
            System.out.println("Members found Successfully");
        } catch (SQLException e) {
            System.out.println("Error fetching members: " + e.getMessage());
        }
    }
    private static void viewMemberInformation(Connection connection, Scanner scanner) {
        System.out.println("Search by: ");
        System.out.println("1. Member ID");
        System.out.println("2. Member Name");
        System.out.println("3. Mobile Number");
        System.out.print("Choose an option: ");
        int option = scanner.nextInt();
        scanner.nextLine();

        String searchQuery = "";
        String searchValue = "";

        switch (option) {
            case 1:
                System.out.print("Enter Member ID: ");
                searchValue = scanner.nextLine(); // This should be the ID as a string
                searchQuery = "SELECT * FROM members WHERE member_id = ?";
                break;
            case 2:
                System.out.print("Enter Member Name: ");
                searchValue = scanner.nextLine();
                searchQuery = "SELECT * FROM members WHERE name = ?";
                break;
            case 3:
                System.out.print("Enter Mobile Number: ");
                searchValue = scanner.nextLine();
                searchQuery = "SELECT * FROM members WHERE mobile_number = ?";
                break;
            default:
                System.out.println("Invalid option. Returning to main menu.");
                return;
        }

        try (PreparedStatement memberStatement = connection.prepareStatement(searchQuery)) {
            // If searching by member ID, set it as an integer
            if (option == 1) {
                memberStatement.setInt(1, Integer.parseInt(searchValue)); // Parse to Integer if member_id is an integer
            } else {
                memberStatement.setString(1, searchValue); // Use setString for other options
            }
            ResultSet memberResultSet = memberStatement.executeQuery();

            if (memberResultSet.next()) {
                // Display member details
                String memberId = memberResultSet.getString("member_id");
                String memberName = memberResultSet.getString("name");
                String mobileNumber = memberResultSet.getString("mobile_number");
                String email = memberResultSet.getString("email");
                String address = memberResultSet.getString("address");

                System.out.println("\nMember Details:");
                System.out.println("ID: " + memberId);
                System.out.println("Name: " + memberName);
                System.out.println("Mobile: " + mobileNumber);
                System.out.println("Email: " + email);
                System.out.println("Address: " + address);

                // Fetch borrowed books
                String borrowSql = "SELECT books.title, loans.loan_date FROM loans " +
                        "JOIN books ON loans.book_id = books.id " +
                        "WHERE loans.member_id = ? AND loans.return_date IS NULL";
                try (PreparedStatement loanStatement = connection.prepareStatement(borrowSql)) {
                    loanStatement.setInt(1, memberResultSet.getInt("member_id")); // Use member_id here
                    ResultSet loanResultSet = loanStatement.executeQuery();

                    System.out.println("\nBorrowed Books:");
                    if (!loanResultSet.isBeforeFirst()) {
                        System.out.println("No borrowed books.");
                    } else {
                        while (loanResultSet.next()) {
                            System.out.println("- " + loanResultSet.getString("title") +
                                    " (Loaned on: " + loanResultSet.getDate("loan_date") + ")");
                        }
                    }
                }

                // Fetch pending fines
//                String fineSql = "SELECT amount FROM fines WHERE member_id = ? AND status = 'unpaid'";
//                try (PreparedStatement fineStatement = connection.prepareStatement(fineSql)) {
//                    fineStatement.setInt(1, memberResultSet.getInt("member_id")); // Use member_id here
//                    ResultSet fineResultSet = fineStatement.executeQuery();
//
//                    System.out.println("\nPending Fines:");
//                    if (!fineResultSet.isBeforeFirst()) {
//                        System.out.println("No pending fines.");
//                    } else {
//                        while (fineResultSet.next()) {
//                            System.out.println("- Fine Amount: " + fineResultSet.getDouble("amount"));
//                        }
//                    }
//                }

            } else {
                System.out.println("Member not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error fetching member information: " + e.getMessage());
        }
    }

    private static void searchBooks(Connection connection, Scanner scanner) {
        System.out.println("Search Books by:");
        System.out.println("1. Author");
        System.out.println("2. Category");
        System.out.println("3. View All Books");
        System.out.print("Choose an option: ");
        int option = scanner.nextInt();
        scanner.nextLine();

        switch (option) {
            case 1:
                System.out.print("Enter author name: ");
                String author = scanner.nextLine();
                searchBooksByAuthor(connection, author);
                break;
            case 2:
                System.out.print("Enter book category: ");
                String category = scanner.nextLine();
                searchBooksByCategory(connection, category);
                break;
            case 3:
                viewAllBooks(connection);  // New option to view all books
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    // Search books by author
    private static void searchBooksByAuthor(Connection connection, String author) {
        String sql = "SELECT * FROM books WHERE author LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + author + "%");
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No books found by author: " + author);
            } else {
                System.out.println("\nBooks by " + author + ":");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("title") + " [Category: " +
                            resultSet.getString("category") + "]");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching books by author: " + e.getMessage());
        }
    }

    // Search books by category
    private static void searchBooksByCategory(Connection connection, String category) {
        String sql = "SELECT * FROM books WHERE category LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + category + "%");
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No books found in category: " + category);
            } else {
                System.out.println("\nBooks in category " + category + ":");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("title") + " by " +
                            resultSet.getString("author"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching books by category: " + e.getMessage());
        }
    }

    // New method to view all books, regardless of availability
    private static void viewAllBooks(Connection connection) {
        String sql = "SELECT * FROM books";  // No WHERE clause to show all books
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No books found in the library.");
            } else {
                System.out.println("\nAll Books in the Library:");
                while (resultSet.next()) {
                    String title = resultSet.getString("title");
                    String author = resultSet.getString("author");
                    String category = resultSet.getString("category");
                    boolean isAvailable = resultSet.getBoolean("is_available");

                    // Convert boolean to Yes/No
                    String availability = isAvailable ? "Yes" : "No";

                    System.out.println("- Title: " + title +
                            ", Author: " + author +
                            ", Category: " + category +
                            ", Is Available: " + availability);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching all books: " + e.getMessage());
        }
    }
    private static void removeBook(Connection connection, Scanner scanner) {
        System.out.print("Enter the book title to remove: ");
        String bookTitle = scanner.nextLine();

        String sql = "DELETE FROM books WHERE title = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bookTitle);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Book removed successfully: " + bookTitle);
            } else {
                System.out.println("No book found with the title: " + bookTitle);
            }
        } catch (SQLException e) {
            System.out.println("Error removing book: " + e.getMessage());
        }
    }
    private static void removeUser(Connection connection, Scanner scanner) {
        System.out.print("Enter the member name to remove: ");
        String memberName = scanner.nextLine();

        String sql = "DELETE FROM members WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memberName);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Member removed successfully: " + memberName);
            } else {
                System.out.println("No member found with the name: " + memberName);
            }
        } catch (SQLException e) {
            System.out.println("Error removing member: " + e.getMessage());
        }
    }




}