package org.nvminh162;

import org.nvminh162.decorator.*;
import org.nvminh162.factory.*;
import org.nvminh162.singleton.DatabaseConnection;
import org.nvminh162.state.*;
import org.nvminh162.strategy.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("===== DESIGN PATTERNS DEMO =====\n");

        // 1. Singleton Pattern
        System.out.println("1. SINGLETON PATTERN:");
        DatabaseConnection db1 = DatabaseConnection.getInstance();
        DatabaseConnection db2 = DatabaseConnection.getInstance();
        db1.connect();
        System.out.println("db1 == db2: " + (db1 == db2));
        System.out.println();

        // 2. Factory Pattern
        System.out.println("2. FACTORY PATTERN:");
        Vehicle car = VehicleFactory.createVehicle("car");
        Vehicle motorcycle = VehicleFactory.createVehicle("motorcycle");
        car.drive();
        motorcycle.drive();
        System.out.println();

        // 3. Decorator Pattern
        System.out.println("3. DECORATOR PATTERN:");
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " - Cost: " + coffee.getCost() + " VND");

        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " - Cost: " + coffee.getCost() + " VND");

        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " - Cost: " + coffee.getCost() + " VND");
        System.out.println();

        // 4. Strategy Pattern
        System.out.println("4. STRATEGY PATTERN:");
        ShoppingCart cart = new ShoppingCart();

        cart.setPaymentStrategy(new CreditCardPayment("1234-5678-9012-3456"));
        cart.checkout(100000);

        cart.setPaymentStrategy(new MomoPayment("0909123456"));
        cart.checkout(50000);
        System.out.println();

        // 5. State Pattern
        System.out.println("5. STATE PATTERN:");
        OrderContext order = new OrderContext();
        order.printStatus();

        order.nextState();
        order.printStatus();

        order.nextState();
        order.printStatus();

        order.nextState();
        order.printStatus();
    }
}