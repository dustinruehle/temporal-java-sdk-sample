package io.temporal.samples.activities.helloworld;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface HelloWorldActivities {

    String greet(String name);
}
