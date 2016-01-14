package com.lambdaworks.redis;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:a.abdelfatah@live.com">Ahmed Kamal</a>
 * @since 12.01.16 09:17
 */
public class SimpleRedisConnectionTest extends AbstractCommandTest{

    SimpleRedisConnection redisConnection;

    @Before
    public void setUp() throws Exception {
        redisConnection = new SimpleRedisConnection(client);
    }

    @Test
    public void testCachePersonAndGetIt() throws Exception {
        Person person = createPersonAdam();
        boolean succeeded = redisConnection.cache("People", "Adam", person);
        assertEquals(succeeded, true);

        Person retrievedPerson = redisConnection.get("People", "Adam");
        assertEqualsForPerson(person, retrievedPerson);
    }

    @Test
    public void testCachePersonListAndGetThem() throws Exception {
        ArrayList<Person> personList = new ArrayList<Person>();

        personList.add(createPersonAdam());
        personList.add(createPersonSara());
        personList.add(createPersonSteve());

        boolean succeeded = redisConnection.cache("People", "PeopleList", personList);
        assertEquals(succeeded, true);

        ArrayList<Person> retrievedPersonList = redisConnection.get("People", "PeopleList");

        assertEqualsForPerson(retrievedPersonList.get(0), createPersonAdam());
        assertEqualsForPerson(retrievedPersonList.get(1), createPersonSara());
        assertEqualsForPerson(retrievedPersonList.get(2), createPersonSteve());

    }

    private void assertEqualsForPerson(Person current, Person retrieved) {
        assertEquals(current.name, retrieved.name);
        assertEquals(current.age, retrieved.age);
        assertEquals(current.phone, retrieved.phone);
        assertEquals(current.isMarried, retrieved.isMarried);
    }

    @Test
    public void testCachePersonAndClearIt() throws Exception {
        Person person = createPersonAdam();
        boolean succeeded = redisConnection.cache("People", "AdamForClear", person);
        assertEquals(succeeded, true);

        assertThat(redisConnection.clear("People", "AdamForClear")).isEqualTo(1);
    }

    private Person createPersonAdam() {
        Person person = new Person("Adam", 20, "+343424324324", false);
        return person;
    }

    private Person createPersonSara() {
        Person person = new Person("Sara", 24, "+211111124", true);
        return person;
    }

    private Person createPersonSteve() {
        Person person = new Person("Steve", 50, "+00604400900", true);
        return person;
    }

}

class Person implements Serializable {
    public Person(String name, int age, String phone, boolean isMarried) {
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.isMarried = isMarried;
    }

    public String name;
    public int age;
    public String phone;
    public boolean isMarried;
}