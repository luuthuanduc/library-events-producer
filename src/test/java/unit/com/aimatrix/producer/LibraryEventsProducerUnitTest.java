package com.aimatrix.producer;

import com.aimatrix.domain.Book;
import com.aimatrix.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryEventsProducerUnitTest {

    String topic = "library-events";
    @Mock
    KafkaTemplate<Integer, String> kafkaTemplate;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    LibraryEventsProducer libraryEventsProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(libraryEventsProducer, "topic", topic);
    }

    @Test
    void sendLibraryEventWithHeaders_failure() {
        Book book = Book.builder()
                .bookId(123)
                .bookAuthor("Dilip")
                .bookName("Kafka using Spring Boot")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        SettableListenableFuture<SendResult<Integer, String>> settableListenableFuture = new SettableListenableFuture<>();
        settableListenableFuture.setException(new RuntimeException("Exception calling Kafka"));
        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(settableListenableFuture);

        assertThrows(Exception.class, () -> libraryEventsProducer.sendLibraryEventWithHeaders(libraryEvent).get());
    }

    @Test
    void sendLibraryEventWithHeaders_success() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book book = Book.builder()
                .bookId(123)
                .bookAuthor("Dilip")
                .bookName("Kafka using Spring Boot")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        SettableListenableFuture<SendResult<Integer, String>> settableListenableFuture = new SettableListenableFuture<>();
        ProducerRecord<Integer, String> producerRecord =
                new ProducerRecord<>(topic, libraryEvent.getLibraryEventId(), objectMapper.writeValueAsString(libraryEvent));
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition(topic, 1),
                1, 1, System.currentTimeMillis(), 1, 2);
        SendResult<Integer, String> sendResult = new SendResult<>(producerRecord, recordMetadata);

        settableListenableFuture.set(sendResult);
        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(settableListenableFuture);

        ListenableFuture<SendResult<Integer, String>> listenableFuture = libraryEventsProducer.sendLibraryEventWithHeaders(libraryEvent);
        assertEquals(1, listenableFuture.get().getRecordMetadata().partition());

    }
}
