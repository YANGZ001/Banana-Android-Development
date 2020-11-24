package com.example.testanr;

public class Test {

    public Test(){

    }

    Test(String nane) {

    }

    public void Test() {
        System.out.println("Test:test");
    }

    public void Test(int a) {

    }
}

 class Test1 extends Test {

     @Override
     public void Test() {
         System.out.println("Test1:test");
     }
 }

class Test2 extends Test {

    @Override
    public void Test() {
        System.out.println("Test2:test");
    }
}

 class Test3 {
    public static void Test3() {
        Test test = new Test();
        Test1 test1 = new Test1();

        test.Test();//Test:test
        test1.Test();//Test1:test

        test = new Test1();
        test.Test();//Test1:test
    }

    public void apply(Test test) {
        test.Test();
    }
 }
