/**
 * This file was automatically generated by the Mule Development Kit
 */
package org.mule.modules;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.ConnectionException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.*;

/**
 * AWS DynamoDB Cloud Connector
 *
 * @author Sheldon Porcina
 */
@Connector(name="dynamodb", schemaVersion="1.0-SNAPSHOT")
public class DynamoDBConnector
{

    static AmazonDynamoDBClient dynamoDB;

    //public static final int TEN_MINUTES = 10 * 60 * 1000;
    public static final int TWENTY_SECONDS = 1000 * 20;

    /**
     * Configurable
     */
    @Configurable
    private String region;

    /**
     * Set the AWS region we are targeting
     *
     * @param region
     *          Defines the AWS region to target.  Use the strings provided in the com.amazonaws.regions.Regions class.
     * @see com.amazonaws.regions.Regions
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Get aws region we are targeting
     */
    public String getRegion() {
        return this.region;
    }

    private Regions getRegionAsEnum() {
        return Regions.valueOf(getRegion());
    }

    /**
     * Connect to the DynamoDB service
     *
     * @param accessKey
     *          A username
     * @param secretKey
     *          A password
     */
    @Connect
    // TODO: try this => @Default (value = Query.MILES) @Optional String unit
    public void connect(@ConnectionKey String accessKey, String secretKey) throws ConnectionException {
        dynamoDB = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
        Region regionEnum = Region.getRegion(getRegionAsEnum());
        dynamoDB.setRegion(regionEnum);
    }

    /**
     * Disconnect from DynamoDB
     */
    @Disconnect
    public void disconnect() {
        // nothing to do
    }

    /**
     * Are we connected to DynamoDB?
     */
    @ValidateConnection
    public boolean isConnected() {
        return this.dynamoDB != null;
    }

    /**
     * This method is called by Mule to find the name of the connection. This method provides a unique identifier for the connection, used for logging and debugging.
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
     *          title of the table
     * @param readCapacityUnits
     *          dedicated read units per second
     * @param writeCapacityUnits
     *          dedicated write units per second
     * @param waitFor
     *          the number of minutes to wait for the table to become active
     *
     * @return ACTIVE if the table already exists, or was created successfully
     */
    @Processor
    public String createTable(final String tableName, final Long readCapacityUnits, final Long writeCapacityUnits, final Integer waitFor) {
        try {
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            dynamoDB.describeTable(describeTableRequest).getTable();

        } catch (ResourceNotFoundException e) {
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                    .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(readCapacityUnits).withWriteCapacityUnits(writeCapacityUnits));

            dynamoDB.createTable(createTableRequest);

            waitForTableToBecomeAvailable(tableName, waitFor);

            return TableStatus.ACTIVE.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return TableStatus.ACTIVE.toString();
    }

    /**
     * Wait for the requests table to become active
     *
     * DynamoDB takes some time to create a new table, depending on the complexity of the table and the requested
     * read/write capacity.  Performing any actions against the table before it is active will result in a failure.
     * This method periodically checks to see if the table is active for the requested period.
     *
     * @param tableName the name of the table to create
     * @param waitFor number of minutes to wait for the table
     */
    private void waitForTableToBecomeAvailable(final String tableName, final Integer waitFor) {
        System.out.println("Waiting for table " + tableName + " to become ACTIVE...");

        final long minutesToWaitFor = (waitFor * 60 * 1000);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + minutesToWaitFor;

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(TWENTY_SECONDS);
            } catch (Exception e) {
            }
            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = dynamoDB.describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                System.out.println("  - current state: " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) throw ase;
            }
        }

        throw new RuntimeException("Table " + tableName + " never went active");
    }
}
