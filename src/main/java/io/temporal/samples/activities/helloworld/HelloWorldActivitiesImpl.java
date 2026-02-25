package io.temporal.samples.activities.helloworld;

public class HelloWorldActivitiesImpl implements HelloWorldActivities {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
