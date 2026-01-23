package org.nvminh162.strategy;

public class MomoPayment implements PaymentStrategy {
    private String phoneNumber;

    public MomoPayment(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Paid " + amount + " using Momo: " + phoneNumber);
    }
}
