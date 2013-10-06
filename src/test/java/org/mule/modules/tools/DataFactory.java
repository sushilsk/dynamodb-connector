package org.mule.modules.tools;


import org.mule.modules.samples.FakeCustomer;

public class DataFactory {

    public static FakeCustomer createFakeCustomer() {
        /*
        The 'num' does not need to be set for the save request to work.  @DynamoDBAutoGeneratedKey in the FakeCustomer
        class ensures that the field is populated automatically.
        */
        return new FakeCustomer()
                .setName("John Doe")
                .setPhone("(123)456-789");
    }
}
