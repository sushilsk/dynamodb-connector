/**
 * This file was automatically generated by the Mule Development Kit
 */
package org.mule.modules;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.*;

import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.*;
import org.mule.api.annotations.param.ConnectionKey;

import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AWS DynamoDB Cloud Connector
 *
 * @author Sheldon Porcina
 */
@Connector(name = "dynamodb", schemaVersion = "1.0-SNAPSHOT")
public class DynamoDBConnector {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDBConnector.class);
    private static AmazonDynamoDBClient dynamoDBClient;

    private static final int TWENTY_SECONDS = 1000 * 20;


    /**
     * Configurable
     */
    @Configurable
    private String region;


    /**
     * Set the AWS region we are targeting
     *
     * @param region Defines the AWS region to target.  Use the strings provided in the com.amazonaws.regions.Regions class.
     * @see com.amazonaws.regions.Regions
     */
    public void setRegion(String region) {
        this.region = region;
    }


    /**
     * Get aws region we are targeting (e.g. US_WEST_1)
     */
    String getRegion() {
        return this.region;
    }


    private Regions getRegionAsEnum() {
        return Regions.valueOf(getRegion());
    }


    private static AmazonDynamoDBClient getDynamoDBClient() {
        return dynamoDBClient;
    }


    private static void setDynamoDBClient(AmazonDynamoDBClient dynamoDBClient) {
        DynamoDBConnector.dynamoDBClient = dynamoDBClient;
    }


    private static Boolean isDynamoDBClientConnected() {
        return getDynamoDBClient() != null;
    }


    /**
     * Connect to the DynamoDB service
     *
     * @param accessKey
     *              The access key provided to you through your Amazon AWS account
     * @param secretKey
     *              The secret key provided to you through your Amazon AWS account
     */
    @Connect
    // TODO: try this => @Default (value = Query.MILES) @Optional String unit
    public void connect(@ConnectionKey String accessKey, String secretKey) throws ConnectionException {

        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (AmazonClientException e) {
            logger.warn("AWSCredentials.properties file was not found.  Attempting to acquire credentials from the default provider chain.");
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }

        try {
        setDynamoDBClient(new AmazonDynamoDBClient(credentialsProvider));
        Region regionEnum = Region.getRegion(getRegionAsEnum());
        getDynamoDBClient().setRegion(regionEnum);
        } catch (Exception e) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "string", "string");
        }
    }


    /**
     * Disconnect from DynamoDB
     */
    @Disconnect
    public void disconnect() {
        setDynamoDBClient(null);
    }


    /**
     * Are we connected to DynamoDB?
     */
    @ValidateConnection
    public boolean isConnected() {
        return isDynamoDBClientConnected();
    }


    /**
     * A unique identifier for the connection, used for logging and debugging
     */
    @ConnectionIdentifier
    public String connectionId() {
        return "AWS DynamoDB Mule Connector";
    }


    /**
     * Create a new table
     *
     * {@sample.xml ../../../doc/DynamoDB-connector.xml.sample dynamodb:create-table}
     *
     * @param tableName
     *              title of the table
     * @param readCapacityUnits
     *              dedicated read units per second
     * @param writeCapacityUnits
     *              dedicated write units per second
     * @param primaryKeyName
     *              the name of the primary key for the table
     * @param waitFor
     *              the number of minutes to wait for the table to become active
     * @return ACTIVE
     *              if the table already exists, or was created successfully, and responded that it is ready for requests
     * @return Exception
     *              if a problem was encountered
     */
    @Processor
    public String createTable(final String tableName, final Long readCapacityUnits, final Long writeCapacityUnits, final String primaryKeyName, final Integer waitFor) {
        try {
            return describeTable(tableName);
        } catch (ResourceNotFoundException e) {

            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                    .withKeySchema(new KeySchemaElement().withAttributeName(primaryKeyName).withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName(primaryKeyName).withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(readCapacityUnits).withWriteCapacityUnits(writeCapacityUnits));

            getDynamoDBClient().createTable(createTableRequest);

            waitForTableToBecomeAvailable(tableName, waitFor);
        }
        return TableStatus.ACTIVE.toString();
    }

    /**
     * Acquire information about a table
     *
     * {@sample.xml ../../../doc/DynamoDB-connector.xml.sample dynamodb:describe-table}
     *
     * @param tableName
     *              title of the table
     * @return ACTIVE
     *              if the table already exists, or was created successfully, and responded that it is ready for requests
     * @return Exception
     *              if a problem was encountered
     */
    @Processor
    public String describeTable(String tableName) {
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription description = getDynamoDBClient().describeTable(describeTableRequest).getTable();

        // The table could be in several different states: CREATING, UPDATING, DELETING, & ACTIVE.
        logger.warn(tableName + " already exists and is in the state of " + description.getTableStatus());
        return description.getTableStatus();
    }


    /**
     * Save a document to a DynamoDB table
     *
     * {@sample.xml ../../../doc/DynamoDB-connector.xml.sample dynamodb:save-document}
     *
     * @param tableName
     *          The table to update
     * @param document
     *          The object to save to the table as a document.  If not explicitly provided, it defaults to "#[payload]".
     * @return Object
     *          the place that was stored
     */
    @Processor
    public Object saveDocument(final String tableName, @Optional @Default("#[payload]") final Object document) {

        try {
            DynamoDBMapper mapper = getDbObjectMapper(tableName);
            mapper.save(document);

            // the document is automatically updated with the data that was stored in DynamoDB
            return document;
        } catch (Exception e) {
            // TODO: what is the best practice for logging and reporting errors from mule connectors? - sporcina (June 20, 2013)
            logger.error(e.getMessage());
        }

        return null;
    }


    /**
     * Acquire a document processor
     *
     * {@sample.xml ../../../doc/DynamoDB-connector.xml.sample dynamodb:get-document}
     *
     * @param tableName
     *          the name of the table to get the document from
     * @param template
     *          an object with the document data that DynamoDB will match against
     * @return Object
     *          the document from the table
     */
    @Processor
    public Object getDocument(final String tableName, @Optional @Default("#[payload]") final Object template) {
        DynamoDBMapper mapper = getDbObjectMapper(tableName);
        return mapper.load(template);
    }

    /**
     * Update document processor
     * <p/>
     * {@sample.xml ../../../doc/DynamoDB-connector.xml.sample dynamodb:update-document}
     *
     * @param tableName
     *          The table to update
     * @param document
     *          The object to save to the table as a document.  If not explicitly provided, it defaults to "#[payload]".
     * @return Object
     *          the place that was stored
     */
    @Processor
    public Object updateDocument(final String tableName, @Optional @Default("#[payload]") final Object document) {

        try {
            DynamoDBMapperConfig config = new DynamoDBMapperConfig(DynamoDBMapperConfig.SaveBehavior.UPDATE);
            DynamoDBMapper mapper = getDbObjectMapper(tableName);
            mapper.save(document, config);

            // save does not return the modified document.  Just return the original.
            return document;
        } catch (Exception e) {
            // TODO: what is the best practice for logging and reporting errors from mule connectors? - sporcina (June 20, 2013)
            System.out.println(e.getMessage());
        }

        return null;
    }


    /**
     * Builds a database object mapper for a dynamodb table
     *
     * @param tableName
     *          the name of the table
     * @return DynamoDBMapper
     *          a new DynamoDB mapper for the targeted table
     */
    private DynamoDBMapper getDbObjectMapper(String tableName) {
        DynamoDBMapperConfig.TableNameOverride override = new DynamoDBMapperConfig.TableNameOverride(tableName);
        DynamoDBMapperConfig config = new DynamoDBMapperConfig(override);
        return new com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper(getDynamoDBClient(), config);
    }


    /**
     * Wait for the table to become active
     *
     * DynamoDB takes some time to create a new table, depending on the complexity of the table and the requested
     * read/write capacity.  Performing any actions against the table before it is active will result in a failure.
     * This method periodically checks to see if the table is active for the requested period.
     *
     * @param tableName
     *              the name of the table to create
     * @param waitFor
     *              number of minutes to wait for the table
     */
    private void waitForTableToBecomeAvailable(final String tableName, final Integer waitFor) {

        logger.info("Waiting for table " + tableName + " to become ACTIVE...");

        final long millisecondsToWaitFor = (waitFor * 60 * 1000);
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + millisecondsToWaitFor;

        while (System.currentTimeMillis() < endTime) {

            try { Thread.sleep(TWENTY_SECONDS);
            } catch (Exception e) {/*ignore sleep exceptions*/}

            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = getDynamoDBClient().describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                logger.info("  - current state: " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
            } catch (AmazonServiceException ase) {
                if (!ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException")) throw ase;
            }
        }

        throw new RuntimeException("Table " + tableName + " never went active");
    }
}
