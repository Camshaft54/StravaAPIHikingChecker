package io.github.camshaft54.HikingPEStrava;

public class User {
    String name;
    float time;

    public User(String name, float time) {
        this.name = name;
        this.time = time;
    }

    public void addTime(float time) {
        this.time += time;
    }

    public boolean isGood() {
        return this.time >= 2700f;
    }

    public float getTime() {
        return this.time;
    }
}
