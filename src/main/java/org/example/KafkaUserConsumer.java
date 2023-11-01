package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaUserConsumer {

    public static void main(String[] args) {
        System.out.println("Запуск Kafka Consumer");
        // Настройки консьюмера Kafka
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-applications");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            System.out.println("Подписка на топик");
            consumer.subscribe(Arrays.asList("users_topic_prefix.public.users")); // users_topic_prefix.public.users
            System.out.println("Подписка выполнена");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));


                for (ConsumerRecord<String, String> record : records) {
                    JsonObject json = JsonParser.parseString(record.value()).getAsJsonObject();
                    JsonObject payload = json.getAsJsonObject("payload");
                    JsonObject after = payload.getAsJsonObject("after");

                    if (after != null) {
                        int id = after.get("id").getAsInt();
                        String name = after.get("name").getAsString();
                        String email = after.get("email").getAsString();
                        System.out.println("Получен новый заказ для пользователя ID=" + id + "отправлено email=" + email);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка при обработке сообщений: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
