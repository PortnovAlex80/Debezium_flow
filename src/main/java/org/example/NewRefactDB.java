package org.example;

import java.sql.*;
import java.util.HashSet;

public class NewRefactDB {

    private static final String url = "jdbc:postgresql://localhost:5433/newdb";
    private static final String user = "postgres";
    private static final String password = "postgres";

    private static HashSet<Integer> processedIds = new HashSet<>(); // HashSet для хранения обработанных ID

    public static void main(String[] args) {

        System.out.println("Подключение к базе данных PostgreSQL...");

        // Бесконечный цикл для постоянного опроса базы данных
        while (true) {
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                showNewEntries(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Задержка в 1 секунду
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void showNewEntries(Connection connection) {
        String query = "SELECT * FROM users ORDER BY id ASC";
//        System.out.println("Выполняется запрос: " + query); // Логирование запроса

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            boolean newEntriesFound = false;

            while (resultSet.next()) {
                int id = resultSet.getInt("id");

                if (!processedIds.contains(id)) {
                    processedIds.add(id); // Добавляем ID в HashSet

                    String name = resultSet.getString("name");
                    String email = resultSet.getString("email");

                    System.out.printf("Новая запись: ID: %d, Name: %s, Email: %s%n", id, name, email);
                    newEntriesFound = true;
                }
            }

            if (!newEntriesFound) {
//                System.out.println("Нет новых записей."); // Сообщение, если новых записей нет
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
