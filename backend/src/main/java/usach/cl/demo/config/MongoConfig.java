package usach.cl.demo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfig.class);

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String mongoUri) {
        if (mongoUri == null || mongoUri.isBlank()) {
            throw new IllegalArgumentException("La URI de MongoDB no puede estar vacía");
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase mongoDatabase(
            MongoClient mongoClient,
            @Value("${app.mongodb.database}") String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la base MongoDB no puede estar vacío");
        }

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.runCommand(new Document("ping", 1));
        LOGGER.info("Conexión a MongoDB verificada para la base {}", databaseName);
        return database;
    }
}
