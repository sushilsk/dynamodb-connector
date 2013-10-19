package org.mule.modules.samples;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


// TODO:  The name of the table is duplicated here and in the test.properties.  I have a solution to remove the
// duplication.  I just need to port it from the other project - sporcina (Oct.3, 2013)
@DynamoDBTable(tableName = "DynamoDbConnectorTestTable_toDelete")
public class FakeCustomer {

    private String num;
    private String name;
    private String phone;

    // used when validating if objects are the same as each other
    private static final Boolean CHECK_NUM = true;
    private static final Boolean DONT_CHECK_NUM = false;


    public FakeCustomer() {
    }


    @DynamoDBHashKey(attributeName = "num")
    @DynamoDBAutoGeneratedKey
    // important: Only String-typed keys can use the @DynamoDBAutoGeneratedKey annotation - sporcina (Oct.3/2013)
    public String getNum() {
        return num;
    }


    public FakeCustomer setNum(String num) {
        this.num = num;
        return this;
    }


    @DynamoDBAttribute(attributeName = "name")
    public String getName() {
        return name;
    }


    public FakeCustomer setName(String name) {
        this.name = name;
        return this;
    }


    @DynamoDBAttribute(attributeName = "phone")
    public String getPhone() {
        return phone;
    }


    public FakeCustomer setPhone(String phone) {
        this.phone = phone;
        return this;
    }


    /**
     * Check if another copy of FakeCustomer is equal
     *
     * @param o
     *         the object, of type FakeCustomer, to compare with
     *
     * @return true: if everything is equal, false if otherwise
     */
    @Override
    public boolean equals(Object o) {
        return isEqual(o, CHECK_NUM);
    }


    /**
     * Check if another copy of FakeCustomer is equal, ignoring the 'num' value
     * <p/>
     * 'num' is automatically generated.  When exercising acceptance tests, we do not know the value that it will be
     * assigned.  This method was created to compare the key data inside request/response tests.
     *
     * @param o
     *         the object, of type FakeCustomer, to compare with
     *
     * @return true: if everything but 'num' is equal, false if otherwise
     */
    public boolean equalsIgnoreNumValue(Object o) {
        return isEqual(o, DONT_CHECK_NUM);
    }


    private boolean isEqual(Object o, Boolean checkNum) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != getClass()) return false;

        FakeCustomer fakeCustomer = (FakeCustomer) o;

        EqualsBuilder builder = new EqualsBuilder()
                .append(getName(), fakeCustomer.getName())
                .append(getPhone(), fakeCustomer.getPhone());

        if (checkNum) builder.append(getNum(), fakeCustomer.getNum());

        return builder.isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(num)
                .append(name)
                .append(phone)
                .toHashCode();
    }


    public void reset() {
        setNum(null);
        setName(null);
        setPhone(null);
    }


    @Override
    public String toString() {
        return "FakeCustomer {" +
                "num='" + getNum() + '\'' +
                ", name='" + getName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                '}';
    }
}