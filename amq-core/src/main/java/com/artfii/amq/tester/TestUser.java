package com.artfii.amq.tester;

import java.io.Serializable;

/**
 * Func :
 *
 * @author: leeton on 2019/3/22.
 */
public class TestUser implements Serializable {
    private Integer id;
    private String name;

    public TestUser(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TestUser{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
