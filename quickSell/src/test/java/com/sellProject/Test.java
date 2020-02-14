package com.sellProject;

/**
 * @author whvo
 * @date 2020/2/6 0006 -23:34
 */
public class Test {
    public static Test t1 = new Test();
    {
        System.out.println("实力块");
    }
    static {
        System.out.println("静态块");

    }

    public static void main(String[] args) {
        Test t2 = new Test();
    }
}
