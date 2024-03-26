package com.airlines.catalog.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import org.springframework.boot.jdbc.DataSourceBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;

/*Create public class DataSourceConfig and 2 member variables awsRegion and secretName.
Autowire these variables with aws.region and secretmanager.key from application.properties file.
*/
@Configuration
public class DataSourceConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${secretmanager.key}")
    private String secretName;

    /* Create a private getSecret method that connects to AWS Secrets Manager and gets the secret string using
    member variables awsRegion and secretName. Catch and throw Secret Manager exceptions  */
    private String getSecret() {
        Region region = Region.of(awsRegion);
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();

        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
        return valueResponse.secretString();
    }

    /* Create a method getDataSource to build the datasource object.
   Call the getSecret function. Parse  the returned JSON string to extract host, port, db,
   username and password. Then configure the mysql url, username and password of the datasource object.
   Return the datasource object. Throw any Json Processing exception.
   */
    @Bean
    public DataSource getDataSource() throws JsonProcessingException {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        String secret = getSecret();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        JsonNode jsonNode = objectMapper.readTree(secret);
        String host = jsonNode.get("host").asText();
        int port = jsonNode.get("port").asInt();
        String db = jsonNode.get("db").asText();
        String username = jsonNode.get("username").asText();
        String password = jsonNode.get("password").asText();
        dataSourceBuilder.url("jdbc:mysql://" + host + ":" + port + "/" + db);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        return dataSourceBuilder.build();
    }

}
