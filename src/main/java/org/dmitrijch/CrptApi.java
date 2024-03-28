package org.dmitrijch;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient; // Объект HttpClient для выполнения HTTP-запросов
    private final Semaphore semaphore; // Семафор для управления количеством запросов

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient(); // Инициализация HttpClient
        this.semaphore = new Semaphore(requestLimit); // Инициализация семафора с указанным лимитом запросов
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // Создание планировщика с одним потоком

        // Планируем выполнение задачи через определенный интервал времени
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()), 0, 10, timeUnit);
    }

    public void createDocument(Object document, String signature) {
        try {
            semaphore.acquire(); // Захватываем разрешение перед отправкой запроса

            // Формируем HTTP-запрос для создания документа
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"document\": " + document + ", \"signature\": \"" +
                            signature + "\"}"))
                    .build();

            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println(response.body());
            } else {
                String responseBody = response.body();
                System.out.println(responseBody);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(); // Освобождаем разрешение после завершения запроса
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);

        Object document = "{\"description\": {\"participantInn\": \"1234567890\"}, \"doc_id\": \"doc123\"," +
                " \"doc_status\": \"status123\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true," +
                " \"owner_inn\": \"9876543210\", \"participant_inn\": \"1234567890\", \"producer_inn\": \"9876543210\"," +
                " \"production_date\": \"2020-01-23\", \"production_type\": \"type123\"," +
                " \"products\": [{\"certificate_document\": \"doc123\", \"certificate_document_date\": \"2020-01-23\"," +
                " \"certificate_document_number\": \"cert123\", \"owner_inn\": \"9876543210\", " +
                "\"producer_inn\": \"9876543210\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"code123\"," +
                " \"uit_code\": \"uit123\", \"uitu_code\": \"uitu123\" }], \"reg_date\": \"2020-01-23\"," +
                " \"reg_number\": \"reg123\"}";
        String signature = "signature";

        crptApi.createDocument(document, signature);
    }
}
