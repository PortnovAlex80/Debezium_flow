package org.example;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class OldDataBaseProfi {

    private static final String url = "jdbc:postgresql://localhost:5432/olddb";
    private static final String user = "postgres";
    private static final String password = "postgres";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // Подключение к базе данных
            Connection connection = DriverManager.getConnection(url, OldDataBaseProfi.user, password);

            // Проверка наличия таблицы и её создание при необходимости
            if (!checkTableExists(connection)) {
                createTable(connection);
                System.out.println("Таблица 'USERS' создана.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return; // Выход из программы в случае ошибки
        } finally {
            System.out.println("Блок инициализации БД завершил работу");
        }

        while (true) {
            System.out.println("Введите команду ('create' для создания пользователя, 'exit' для выхода):");
            String input = scanner.nextLine();

            if ("c".equalsIgnoreCase(input)) {
                User newUser = generateRandomUser();
                insertUser(newUser);
                System.out.println("Запись с пользователем " + newUser.getName() + " создана.");
            } else if ("e".equalsIgnoreCase(input)) {
                break;
            } else {
                System.out.println("Неизвестная команда.");
            }
        }
    }

    private static void insertUser(User user) {
        try (Connection connection = DriverManager.getConnection(url, OldDataBaseProfi.user, password);
             PreparedStatement insertUser = connection.prepareStatement(
                     "INSERT INTO USERS (id, name, email) VALUES (?, ?, ?)")) {
            insertUser.setInt(1, user.getId());
            insertUser.setString(2, user.getName());
            insertUser.setString(3, user.getEmail());
            insertUser.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                System.out.println("Такой пользователь уже существует.");
            } else {
                e.printStackTrace();
            }
        }
    }

    private static User generateRandomUser() {
        Random random = new Random();
        int id = random.nextInt(1000); // Генерируем ID в пределах от 0 до 999
        String name = "User" + id;
        String email = "user" + id + "@example.com";
        return new User(id, name, email);
    }

    static class User {
        private final int id;
        private final String name;
        private final String email;

        User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    private static boolean checkTableExists(Connection connection) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "users", null)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createTable(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE USERS (" +
                            "id INTEGER PRIMARY KEY, " +
                            "name VARCHAR(100), " +
                            "email VARCHAR(100))"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


